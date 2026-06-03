package center.oneapi.mobile.features.audio;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Base64;

import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;

import center.oneapi.mobile.core.ApiClient;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class AudioTranscriptionController {
    public interface Callback {
        void onStatus(String message);

        void onReady();

        void onText(String text, boolean fin);

        void onLevel(float level);

        void onError(String message);

        void onAutoSubmit(String text);

        void onClosed();
    }

    private static final int SAMPLE_RATE = 16000;
    private static final int FRAME_BYTES = 6400;
    private static final long STANDBY_SILENCE_MS = 700L;
    private static final long AUTO_STOP_SILENCE_MS = 1500L;
    private static final float SPEECH_LEVEL = 0.22f;

    private final ApiClient api;
    private final OkHttpClient client = new OkHttpClient.Builder().build();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private WebSocket socket;
    private AudioRecord recorder;
    private Thread recordThread;
    private String latestText = "";
    private final StringBuilder finalText = new StringBuilder();
    private Callback activeCallback;
    private Provider provider = Provider.TENCENT;
    private ByteArrayOutputStream recordedPcm;
    private volatile boolean autoSubmitRequested;
    private volatile boolean autoSubmitDelivered;
    private volatile boolean suppressFallbackTranscription;
    private volatile boolean tencentAudioReady;
    private final AtomicReference<WebSocket> activeTencentSocket = new AtomicReference<>();

    private enum Provider {
        TENCENT,
        MIMO
    }

    public AudioTranscriptionController(ApiClient api) {
        this.api = api;
    }

    public synchronized boolean isRunning() {
        return running.get();
    }

    public void start(Context context, Callback callback) throws Exception {
        if (running.get()) return;
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("未授予录音权限");
        }
        latestText = "";
        finalText.setLength(0);
        autoSubmitRequested = false;
        autoSubmitDelivered = false;
        suppressFallbackTranscription = false;
        tencentAudioReady = false;
        activeCallback = callback;
        provider = Provider.TENCENT;
        recordedPcm = new ByteArrayOutputStream();
        running.set(true);
        startAudioLoop(null, callback);
        if (callback != null) callback.onReady();
        try {
            status(callback, "步骤1/5：获取腾讯云 ASR 签名地址");
            startTencent(callback);
        } catch (Exception error) {
            status(callback, "腾讯实时链路启动失败，切换小米备用录音：" + cleanMessage(error));
            byte[] pcm;
            synchronized (this) {
                pcm = recordedPcm == null ? new byte[0] : recordedPcm.toByteArray();
            }
            fallbackAfterTencentFailure(pcm, callback, cleanMessage(error));
        }
    }

    private void startTencent(Callback callback) throws Exception {
        JSONObject signed = api.get("/api/mobile/asr/tencent-url");
        String url = signed.optJSONObject("data") == null ? signed.optString("url", "") : signed.optJSONObject("data").optString("url", "");
        url = normalizeTencentUrl(url);
        if (url.trim().isEmpty()) throw new IllegalStateException("服务器未返回腾讯云 ASR 签名地址");
        if (!url.startsWith("wss://")) throw new IllegalStateException("腾讯云 ASR 签名地址无效：" + trimForDisplay(url));
        status(callback, "步骤2/5：连接腾讯云实时 ASR WebSocket");
        Request request = new Request.Builder().url(url).build();
        socket = client.newWebSocket(request, new WebSocketListener() {
            private final AtomicBoolean audioStarted = new AtomicBoolean(false);

            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                activeTencentSocket.set(webSocket);
                status(callback, "步骤3/5：WebSocket 已连接，等待腾讯云握手确认");
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                if (handleTencentHandshake(text, webSocket, audioStarted, callback)) return;
                handleTencentMessage(text, callback);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                if (!isCurrentTencentSocket(webSocket)) return;
                running.set(false);
                stopRecorderOnly();
                String message = "腾讯实时识别失败：" + websocketFailureMessage(t, response);
                byte[] pcm = recordedPcm == null ? new byte[0] : recordedPcm.toByteArray();
                if (latestText == null || latestText.trim().isEmpty()) {
                    fallbackAfterTencentFailure(pcm, callback, message);
                } else if (callback != null) {
                    callback.onError(message);
                }
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                if (!isCurrentTencentSocket(webSocket)) return;
                running.set(false);
                stopRecorderOnly();
                if (callback != null) callback.onClosed();
            }
        });
    }

    private synchronized boolean isCurrentTencentSocket(WebSocket webSocket) {
        boolean current = activeTencentSocket.compareAndSet(webSocket, null);
        if (socket == webSocket) {
            socket = null;
            current = true;
        }
        if (current) tencentAudioReady = false;
        return current;
    }

    public synchronized String stop() {
        running.set(false);
        stopRecorderOnly();
        if (socket != null) {
            WebSocket closing = socket;
            socket = null;
            tencentAudioReady = false;
            activeTencentSocket.compareAndSet(closing, null);
            try {
                closing.send("{\"type\":\"end\"}");
            } catch (Exception ignored) {
            }
            new Thread(() -> {
                try {
                    Thread.sleep(1200);
                } catch (InterruptedException ignored) {
                }
                try {
                    closing.close(1000, "end");
                } catch (Exception ignored) {
                }
            }, "tencent-asr-close").start();
        }
        byte[] pcm = recordedPcm == null ? new byte[0] : recordedPcm.toByteArray();
        recordedPcm = null;
        if (!suppressFallbackTranscription && (provider == Provider.MIMO || ((latestText == null || latestText.trim().isEmpty()) && pcm.length > 0))) {
            transcribeMimoAsync(pcm, activeCallback, provider == Provider.MIMO ? "小米备用转写" : "腾讯无最终文本，小米备用转写");
        } else if (autoSubmitRequested) {
            deliverAutoSubmit(activeCallback);
        }
        suppressFallbackTranscription = false;
        return latestText == null ? "" : latestText.trim();
    }

    private void startAudioLoop(WebSocket streamSocket, Callback callback) {
        if (recordThread != null && recordThread.isAlive()) {
            return;
        }
        int minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        int bufferSize = Math.max(minBuffer, FRAME_BYTES * 2);
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        recorder.startRecording();
        recordThread = new Thread(() -> {
            byte[] buffer = new byte[FRAME_BYTES];
            boolean heardSpeech = false;
            boolean standby = false;
            long lastSpeechAt = System.currentTimeMillis();
            while (running.get()) {
                AudioRecord currentRecorder = recorder;
                if (currentRecorder == null) break;
                int read;
                try {
                    read = currentRecorder.read(buffer, 0, buffer.length);
                } catch (Exception error) {
                    running.set(false);
                    byte[] pcm;
                    synchronized (this) {
                        pcm = recordedPcm == null ? new byte[0] : recordedPcm.toByteArray();
                    }
                    fallbackAfterTencentFailure(pcm, callback, "录音读取失败：" + cleanMessage(error));
                    break;
                }
                if (read > 0) {
                    synchronized (this) {
                        if (recordedPcm != null) recordedPcm.write(buffer, 0, read);
                    }
                    WebSocket currentSocket = activeTencentSocket.get();
                    if (provider == Provider.TENCENT && tencentAudioReady && currentSocket != null && (streamSocket == null || currentSocket == streamSocket)) {
                        boolean sent = currentSocket.send(ByteString.of(buffer, 0, read));
                        if (!sent) {
                            running.set(false);
                            byte[] pcm;
                            synchronized (this) {
                                pcm = recordedPcm == null ? new byte[0] : recordedPcm.toByteArray();
                            }
                            stopRecorderOnly();
                            fallbackAfterTencentFailure(pcm, callback, "腾讯实时链路发送音频失败");
                            break;
                        }
                    }
                    float level = level(buffer, read);
                    if (level >= SPEECH_LEVEL) {
                        heardSpeech = true;
                        standby = false;
                        lastSpeechAt = System.currentTimeMillis();
                    } else if (!standby && System.currentTimeMillis() - lastSpeechAt >= STANDBY_SILENCE_MS) {
                        standby = true;
                        if (callback != null) callback.onLevel(0f);
                    }
                    if (System.currentTimeMillis() - lastSpeechAt >= AUTO_STOP_SILENCE_MS) {
                        status(callback, "检测到 1.5 秒静音，结束语音输入");
                        autoSubmitRequested = true;
                        suppressFallbackTranscription = !heardSpeech;
                        stop();
                        break;
                    }
                    if (callback != null) callback.onLevel(level);
                }
                try {
                    Thread.sleep(180);
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        }, "tencent-asr-audio");
        recordThread.start();
    }

    private boolean handleTencentHandshake(String payload, WebSocket webSocket, AtomicBoolean audioStarted, Callback callback) {
        try {
            JSONObject json = new JSONObject(payload);
            boolean hasResult = json.has("result") && !json.isNull("result");
            boolean isFinal = json.optInt("final", 0) == 1;
            int code = json.optInt("code", 0);
            if (!hasResult && !isFinal) {
                if (code == 0) {
                    if (audioStarted.compareAndSet(false, true)) {
                        status(callback, "步骤4/5：腾讯云握手成功，开始实时发送 PCM 音频");
                        tencentAudioReady = true;
                        if (callback != null) callback.onReady();
                    }
                    return true;
                }
                String message = json.optString("message", json.optString("error", "握手失败"));
                running.set(false);
                activeTencentSocket.compareAndSet(webSocket, null);
                fallbackAfterTencentFailure(new byte[0], callback, "腾讯握手失败：" + message);
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private void startMimoRecording(Callback callback) {
        provider = Provider.MIMO;
        running.set(true);
        recordedPcm = new ByteArrayOutputStream();
        int minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        int bufferSize = Math.max(minBuffer, FRAME_BYTES * 2);
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        recorder.startRecording();
        recordThread = new Thread(() -> {
            byte[] buffer = new byte[FRAME_BYTES];
            boolean heardSpeech = false;
            boolean standby = false;
            long lastSpeechAt = System.currentTimeMillis();
            status(callback, "步骤3/5：小米备用链路录音中，停止后上传识别");
            if (callback != null) callback.onReady();
            while (running.get()) {
                AudioRecord currentRecorder = recorder;
                if (currentRecorder == null) break;
                int read;
                try {
                    read = currentRecorder.read(buffer, 0, buffer.length);
                } catch (Exception error) {
                    running.set(false);
                    if (callback != null) callback.onError("录音读取失败：" + cleanMessage(error));
                    break;
                }
                if (read > 0) {
                    synchronized (this) {
                        if (recordedPcm != null) recordedPcm.write(buffer, 0, read);
                    }
                    float level = level(buffer, read);
                    if (level >= SPEECH_LEVEL) {
                        heardSpeech = true;
                        standby = false;
                        lastSpeechAt = System.currentTimeMillis();
                    } else if (!standby && System.currentTimeMillis() - lastSpeechAt >= STANDBY_SILENCE_MS) {
                        standby = true;
                        if (callback != null) callback.onLevel(0f);
                    }
                    if (System.currentTimeMillis() - lastSpeechAt >= AUTO_STOP_SILENCE_MS) {
                        status(callback, "检测到 1.5 秒静音，结束语音输入");
                        autoSubmitRequested = true;
                        suppressFallbackTranscription = !heardSpeech;
                        stop();
                        break;
                    }
                    if (callback != null) callback.onLevel(level);
                }
                try {
                    Thread.sleep(180);
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        }, "mimo-asr-audio");
        recordThread.start();
    }

    private void transcribeMimoAsync(byte[] pcm, Callback callback, String reason) {
        if (pcm == null || pcm.length == 0) {
            if (callback != null) callback.onClosed();
            return;
        }
        new Thread(() -> {
            try {
                status(callback, "步骤4/5：" + reason + "，上传音频");
                String audio = Base64.encodeToString(wavBytes(pcm), Base64.NO_WRAP);
                JSONObject body = new JSONObject()
                        .put("data", "data:audio/wav;base64," + audio)
                        .put("format", "wav")
                        .put("language", "auto");
                JSONObject response = api.post("/api/mobile/asr/mimo-transcribe", body);
                JSONObject data = response.optJSONObject("data");
                latestText = data == null ? response.optString("text", "").trim() : data.optString("text", "").trim();
                if (callback != null && !latestText.isEmpty()) callback.onText(latestText, true);
                status(callback, "步骤5/5：小米备用识别完成");
                deliverAutoSubmit(callback);
            } catch (Exception error) {
                if (callback != null) callback.onError("小米 ASR 识别失败：" + cleanMessage(error));
            } finally {
                if (callback != null) callback.onClosed();
            }
        }, "mimo-asr-transcribe").start();
    }

    private void handleTencentMessage(String payload, Callback callback) {
        try {
            JSONObject json = new JSONObject(payload);
            if (json.optInt("code", 0) != 0) {
                String message = json.optString("message", json.optString("error", ""));
                running.set(false);
                stopRecorderOnly();
                byte[] pcm;
                synchronized (this) {
                    pcm = recordedPcm == null ? new byte[0] : recordedPcm.toByteArray();
                }
                if (latestText == null || latestText.trim().isEmpty()) {
                    fallbackAfterTencentFailure(pcm, callback, "腾讯返回错误：" + message);
                } else if (!message.trim().isEmpty() && callback != null) {
                    callback.onError("腾讯返回错误：" + message);
                }
                return;
            }
            JSONObject result = json.optJSONObject("result");
            if (result == null) return;
            String text = result.optString("voice_text_str", result.optString("text", "")).trim();
            if (text.isEmpty()) return;
            int sliceType = result.optInt("slice_type", 0);
            if (sliceType == 2) {
                if (finalText.length() > 0 && !text.startsWith(finalText.toString())) {
                    finalText.append(text);
                } else if (finalText.length() == 0) {
                    finalText.append(text);
                } else {
                    finalText.setLength(0);
                    finalText.append(text);
                }
                latestText = finalText.toString();
            } else if (finalText.length() > 0 && !text.startsWith(finalText.toString())) {
                latestText = finalText + text;
            } else {
                latestText = text;
            }
            status(callback, "步骤5/5：收到腾讯识别文本");
            if (callback != null) callback.onText(latestText, sliceType == 2);
        } catch (Exception error) {
            if (callback != null) callback.onError(error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage());
        }
    }

    private String normalizeTencentUrl(String value) {
        String url = value == null ? "" : value.trim();
        if (url.startsWith("\"") && url.endsWith("\"") && url.length() > 1) {
            url = url.substring(1, url.length() - 1);
        }
        return url
                .replace("\\/", "/")
                .replace("\\u0026", "&")
                .replace("&amp;", "&")
                .trim();
    }

    private void fallbackAfterTencentFailure(byte[] pcm, Callback callback, String reason) {
        closeTencentSocketQuietly();
        running.set(false);
        stopRecorderOnly();
        String clean = reason == null ? "腾讯实时识别不可用" : reason.trim();
        if (pcm != null && pcm.length > 0) {
            transcribeMimoAsync(pcm, callback, clean + "，改用小米备用转写");
            return;
        }
        try {
            status(callback, clean + "，改用小米备用录音");
            startMimoRecording(callback);
        } catch (Exception error) {
            if (callback != null) callback.onError(clean + "；小米备用录音启动失败：" + cleanMessage(error));
        }
    }

    private synchronized void closeTencentSocketQuietly() {
        if (socket == null) return;
        WebSocket closing = socket;
        socket = null;
        tencentAudioReady = false;
        activeTencentSocket.compareAndSet(closing, null);
        try {
            closing.close(1000, "fallback");
        } catch (Exception ignored) {
        }
    }

    private byte[] wavBytes(byte[] pcm) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int dataSize = pcm.length;
        int byteRate = SAMPLE_RATE * 2;
        out.write("RIFF".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        writeLeInt(out, 36 + dataSize);
        out.write("WAVEfmt ".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        writeLeInt(out, 16);
        writeLeShort(out, (short) 1);
        writeLeShort(out, (short) 1);
        writeLeInt(out, SAMPLE_RATE);
        writeLeInt(out, byteRate);
        writeLeShort(out, (short) 2);
        writeLeShort(out, (short) 16);
        out.write("data".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        writeLeInt(out, dataSize);
        out.write(pcm);
        return out.toByteArray();
    }

    private void writeLeInt(ByteArrayOutputStream out, int value) throws Exception {
        out.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array());
    }

    private void writeLeShort(ByteArrayOutputStream out, short value) throws Exception {
        out.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array());
    }

    private String trimForDisplay(String value) {
        String clean = value == null ? "" : value.trim();
        return clean.length() > 80 ? clean.substring(0, 80) + "..." : clean;
    }

    private String cleanMessage(Throwable error) {
        if (error == null) return "";
        String message = error.getMessage();
        if (message == null || message.trim().isEmpty()) return error.getClass().getSimpleName();
        return message.replace('\n', ' ').trim();
    }

    private String websocketFailureMessage(Throwable error, Response response) {
        String message = cleanMessage(error);
        if (response != null) {
            message = "HTTP " + response.code() + (message.isEmpty() ? "" : " · " + message);
        }
        return message.trim().isEmpty() ? "未知 WebSocket 错误" : message;
    }

    private void status(Callback callback, String message) {
        if (callback != null && message != null && !message.trim().isEmpty()) {
            callback.onStatus(message.trim());
        }
    }

    private void deliverAutoSubmit(Callback callback) {
        String text = latestText == null ? "" : latestText.trim();
        if (!autoSubmitRequested || autoSubmitDelivered || text.isEmpty() || callback == null) return;
        autoSubmitDelivered = true;
        callback.onAutoSubmit(text);
    }

    private float level(byte[] pcm, int length) {
        long sum = 0;
        int samples = 0;
        for (int i = 0; i + 1 < length; i += 2) {
            int sample = (pcm[i] & 0xff) | (pcm[i + 1] << 8);
            sum += Math.abs(sample);
            samples++;
        }
        return samples == 0 ? 0f : Math.min(10f, (sum / (float) samples) / 2500f);
    }

    private synchronized void stopRecorderOnly() {
        if (recordThread != null) {
            try {
                recordThread.interrupt();
            } catch (Exception ignored) {
            }
            recordThread = null;
        }
        if (recorder != null) {
            try {
                recorder.stop();
            } catch (Exception ignored) {
            }
            try {
                recorder.release();
            } catch (Exception ignored) {
            }
            recorder = null;
        }
    }
}
