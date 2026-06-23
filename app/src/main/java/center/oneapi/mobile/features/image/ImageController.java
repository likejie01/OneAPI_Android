package center.oneapi.mobile.features.image;

import android.content.Context;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import center.oneapi.mobile.core.ApiClient;
import center.oneapi.mobile.features.chat.ChatController;

public class ImageController {
    private static final int IMAGE_READ_TIMEOUT_MS = 10 * 60 * 1000;
    public static final String IMAGE_GENERATIONS_PATH = "/v1/images/generations";
    public static final String IMAGE_EDITS_PATH = "/v1/images/edits";
    private final ApiClient api;

    public ImageController(ApiClient api) {
        this.api = api;
    }

    public JSONObject buildGenerationRequest(String prompt, String size, String quality, boolean randomSeed) throws Exception {
        JSONObject body = new JSONObject();
        body.put("model", "gpt-image-2");
        body.put("prompt", prompt == null ? "" : prompt);
        body.put("n", 1);
        body.put("size", clean(size).isEmpty() ? "1024x1024" : size);
        body.put("quality", clean(quality).isEmpty() ? "medium" : quality);
        body.put("response_format", "b64_json");
        if (randomSeed) {
            body.put("seed", System.currentTimeMillis() % 1000000);
        }
        return body;
    }

    public String generate(String prompt, String size, String quality, boolean randomSeed) throws Exception {
        return generateResult(prompt, size, quality, randomSeed).image;
    }

    public ImageResult generateResult(String prompt, String size, String quality, boolean randomSeed) throws Exception {
        return extractResult(api.postWithBearer(IMAGE_GENERATIONS_PATH, buildGenerationRequest(prompt, size, quality, randomSeed), api.appApiKey(), IMAGE_READ_TIMEOUT_MS));
    }

    public String edit(Context context, Uri imageUri, String prompt, String size, String quality) throws Exception {
        return editResult(context, imageUri, prompt, size, quality).image;
    }

    public ImageResult editResult(Context context, Uri imageUri, String prompt, String size, String quality) throws Exception {
        String boundary = "----OneApiAndroid" + System.currentTimeMillis();
        HttpURLConnection connection = (HttpURLConnection) new URL(ApiClient.buildUrl(api.server(), IMAGE_EDITS_PATH)).openConnection();
        try {
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(IMAGE_READ_TIMEOUT_MS);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            String relayKey = api.appApiKey();
            if (relayKey.isEmpty()) {
                throw new java.io.IOException("App 专用 Key 未初始化，无法调用图片编辑接口");
            }
            connection.setRequestProperty("Authorization", "Bearer " + relayKey);
            connection.setDoOutput(true);
            String mime = context.getContentResolver().getType(imageUri);
            if (mime == null || mime.trim().isEmpty()) mime = "image/png";
            String fileName = "image." + (mime.contains("jpeg") ? "jpg" : mime.contains("webp") ? "webp" : "png");
            try (DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
                writeField(out, boundary, "model", "gpt-image-2");
                writeField(out, boundary, "prompt", prompt);
                writeField(out, boundary, "size", clean(size).isEmpty() ? "1024x1024" : size);
                writeField(out, boundary, "quality", clean(quality).isEmpty() ? "medium" : quality);
                writeField(out, boundary, "response_format", "b64_json");
                out.writeBytes("--" + boundary + "\r\n");
                out.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"" + fileName + "\"\r\n");
                out.writeBytes("Content-Type: " + mime + "\r\n\r\n");
                try (InputStream input = context.getContentResolver().openInputStream(imageUri)) {
                    if (input == null) throw new java.io.IOException("图片读取失败");
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = input.read(buffer)) != -1) out.write(buffer, 0, read);
                }
                out.writeBytes("\r\n--" + boundary + "--\r\n");
            }
            int code = connection.getResponseCode();
            String raw = read(code >= 400 ? connection.getErrorStream() : connection.getInputStream());
            JSONObject json = raw.trim().isEmpty() ? new JSONObject() : new JSONObject(raw);
            if (code >= 400 || (json.has("success") && !json.optBoolean("success"))) {
                throw new java.io.IOException(errorMessage(json, code));
            }
            return extractResult(json);
        } finally {
            connection.disconnect();
        }
    }

    public static String extractImage(JSONObject response) {
        return extractResult(response).image;
    }

    public static ImageResult extractResult(JSONObject response) {
        if (response == null) return new ImageResult("", "");
        String image = "";
        JSONArray data = response.optJSONArray("data");
        if (data != null && data.length() > 0) {
            JSONObject first = data.optJSONObject(0);
            if (first != null) {
                String url = first.optString("url", first.optString("image_url", ""));
                if (!url.isEmpty()) image = url;
                String b64 = first.optString("b64_json", first.optString("b64Json", ""));
                if (image.isEmpty() && !b64.isEmpty()) image = b64.startsWith("data:image/") ? b64 : "data:image/png;base64," + b64;
            }
        }
        if (image.isEmpty()) {
            String direct = response.optString("image", response.optString("result", ""));
            if (!direct.isEmpty() && !direct.startsWith("http") && !direct.startsWith("data:image/")) {
                image = "data:image/png;base64," + direct;
            } else {
                image = direct;
            }
        }
        ChatController.Usage usage = ChatController.Usage.from(response.optJSONObject("usage"));
        return new ImageResult(image, usage.displayText());
    }

    public static final class ImageResult {
        public final String image;
        public final String tokenText;

        ImageResult(String image, String tokenText) {
            this.image = image == null ? "" : image;
            this.tokenText = tokenText == null ? "" : tokenText;
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static void writeField(DataOutputStream out, String boundary, String name, String value) throws Exception {
        out.writeBytes("--" + boundary + "\r\n");
        out.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n");
        out.write(value == null ? new byte[0] : value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        out.writeBytes("\r\n");
    }

    private static String read(InputStream stream) throws Exception {
        if (stream == null) return "";
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = stream.read(buffer)) != -1) output.write(buffer, 0, read);
        return output.toString("UTF-8");
    }

    static String errorMessage(JSONObject json, int code) {
        if (json == null) return "图片编辑接口请求失败（HTTP " + code + "）";
        String message = json.optString("message", "");
        JSONObject error = json.optJSONObject("error");
        if (message.trim().isEmpty() && error != null) {
            message = error.optString("message", "");
        }
        JSONObject data = json.optJSONObject("data");
        if (message.trim().isEmpty() && data != null) {
            message = data.optString("message", "");
        }
        return message.trim().isEmpty() ? "图片编辑接口请求失败（HTTP " + code + "）" : message;
    }
}
