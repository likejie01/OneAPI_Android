package center.oneapi.mobile.core;

import android.os.Looper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ApiClient {
    private final AppPrefs prefs;
    private final Set<HttpURLConnection> activeConnections = Collections.synchronizedSet(new HashSet<>());

    public ApiClient(AppPrefs prefs) {
        this.prefs = prefs;
    }

    public JSONObject get(String path) throws IOException, JSONException {
        return request("GET", path, null);
    }

    public JSONObject post(String path, JSONObject body) throws IOException, JSONException {
        return request("POST", path, body);
    }

    public JSONObject post(String path, JSONObject body, int readTimeoutMs) throws IOException, JSONException {
        return request("POST", path, body, readTimeoutMs);
    }

    public JSONObject postWithBearer(String path, JSONObject body, String bearerToken, int readTimeoutMs) throws IOException, JSONException {
        return request("POST", path, body, readTimeoutMs, clean(bearerToken), true);
    }

    public void postStream(String path, JSONObject body, StreamHandler handler) throws IOException, JSONException {
        postStream(path, body, handler, "", false);
    }

    public void postStream(String path, JSONObject body, StreamHandler handler, String bearerTokenOverride) throws IOException, JSONException {
        postStream(path, body, handler, bearerTokenOverride, false);
    }

    public void postStreamWithBearer(String path, JSONObject body, StreamHandler handler, String bearerToken) throws IOException, JSONException {
        postStream(path, body, handler, clean(bearerToken), true);
    }

    private void postStream(String path, JSONObject body, StreamHandler handler, String bearerTokenOverride, boolean requireBearerOverride) throws IOException, JSONException {
        ensureBackgroundThread();
        if (requireBearerOverride && clean(bearerTokenOverride).isEmpty()) {
            throw new IOException("App 专用 Key 未初始化");
        }
        HttpURLConnection connection = (HttpURLConnection) new URL(buildUrl(prefs.server(), path)).openConnection();
        activeConnections.add(connection);
        try {
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(180000);
            connection.setRequestProperty("Accept", "text/event-stream");
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            boolean usesBearerOverride = !clean(bearerTokenOverride).isEmpty();
            String token = requireBearerOverride ? clean(bearerTokenOverride) : bearerValue(prefs.token(), bearerTokenOverride);
            if (!token.isEmpty()) connection.setRequestProperty("Authorization", "Bearer " + token);
            if (!usesBearerOverride) {
                String cookie = prefs.cookie();
                if (!cookie.isEmpty()) connection.setRequestProperty("Cookie", cookie);
                String userId = prefs.userId();
                if (!userId.isEmpty()) connection.setRequestProperty("New-Api-User", userId);
            }
            if (body != null) {
                connection.setDoOutput(true);
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8))) {
                    writer.write(body.toString());
                }
            }
            int code = connection.getResponseCode();
            if (code < 200 || code >= 400) {
                String text = read(connection.getErrorStream());
                throw new IOException(text.trim().isEmpty() ? "HTTP " + code : text);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (Thread.currentThread().isInterrupted()) throw new IOException("请求已停止");
                    if (handler != null) handler.onLine(line);
                }
            }
        } finally {
            activeConnections.remove(connection);
            connection.disconnect();
        }
    }

    public JSONObject delete(String path) throws IOException, JSONException {
        return request("DELETE", path, null);
    }

    public JSONObject request(String method, String path, JSONObject body) throws IOException, JSONException {
        return request(method, path, body, 30000);
    }

    public JSONObject request(String method, String path, JSONObject body, int readTimeoutMs) throws IOException, JSONException {
        return request(method, path, body, readTimeoutMs, "", false);
    }

    public JSONObject request(String method, String path, JSONObject body, int readTimeoutMs, String bearerTokenOverride) throws IOException, JSONException {
        return request(method, path, body, readTimeoutMs, bearerTokenOverride, false);
    }

    private JSONObject request(String method, String path, JSONObject body, int readTimeoutMs, String bearerTokenOverride, boolean requireBearerOverride) throws IOException, JSONException {
        ensureBackgroundThread();
        if (requireBearerOverride && clean(bearerTokenOverride).isEmpty()) {
            throw new IOException("App 专用 Key 未初始化");
        }
        HttpURLConnection connection = (HttpURLConnection) new URL(buildUrl(prefs.server(), path)).openConnection();
        activeConnections.add(connection);
        try {
            connection.setRequestMethod(method);
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(readTimeoutMs);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            boolean usesBearerOverride = !clean(bearerTokenOverride).isEmpty();
            String token = requireBearerOverride ? clean(bearerTokenOverride) : bearerValue(prefs.token(), bearerTokenOverride);
            if (!token.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + token);
            }
            if (!usesBearerOverride) {
                String cookie = prefs.cookie();
                if (!cookie.isEmpty()) {
                    connection.setRequestProperty("Cookie", cookie);
                }
                String userId = prefs.userId();
                if (!userId.isEmpty()) {
                    connection.setRequestProperty("New-Api-User", userId);
                }
            }
            if (body != null) {
                connection.setDoOutput(true);
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8))) {
                    writer.write(body.toString());
                }
            }
            int code = connection.getResponseCode();
            String setCookie = connection.getHeaderField("Set-Cookie");
            if (setCookie != null && !setCookie.trim().isEmpty()) {
                prefs.setCookie(setCookie.split(";", 2)[0]);
            }
            String text = read(code >= 200 && code < 400 ? connection.getInputStream() : connection.getErrorStream());
            JSONObject json = text.trim().isEmpty() ? new JSONObject() : new JSONObject(text);
            if (code < 200 || code >= 400 || isErrorEnvelope(json)) {
                throw new IOException(errorMessage(json, code));
            }
            return json;
        } finally {
            activeConnections.remove(connection);
            connection.disconnect();
        }
    }

    public void cancelActiveRequests() {
        synchronized (activeConnections) {
            for (HttpURLConnection connection : activeConnections) {
                try {
                    connection.disconnect();
                } catch (Exception ignored) {
                }
            }
            activeConnections.clear();
        }
    }

    public static String buildUrl(String server, String path) {
        String base = server == null ? AppPrefs.DEFAULT_SERVER : server.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String route = path == null ? "" : path.trim();
        if (route.startsWith("http://") || route.startsWith("https://")) {
            return route;
        }
        if (!route.startsWith("/")) {
            route = "/" + route;
        }
        return base + route;
    }

    public static String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    public String server() {
        return prefs.server();
    }

    public String cookie() {
        return prefs.cookie();
    }

    public String userId() {
        return prefs.userId();
    }

    public String token() {
        return prefs.token();
    }

    public String appApiKey() {
        return prefs.appApiKey();
    }

    public static String bearerValue(String fallbackToken, String overrideToken) {
        String override = clean(overrideToken);
        return override.isEmpty() ? clean(fallbackToken) : override;
    }

    static boolean isErrorEnvelope(JSONObject json) {
        if (json == null) {
            return false;
        }
        if (json.has("success")) {
            return !json.optBoolean("success");
        }
        return "error".equalsIgnoreCase(json.optString("message", "").trim());
    }

    static String errorMessage(JSONObject json, int code) {
        if (json == null) {
            return "HTTP " + code;
        }
        String message = json.optString("message", json.optString("error", ""));
        String dataText = "";
        Object data = json.opt("data");
        if (data instanceof JSONObject) {
            dataText = ((JSONObject) data).optString("message", "");
        } else if (data instanceof String) {
            dataText = ((String) data).trim();
        }
        String cleanMessage = message.trim();
        if ((cleanMessage.isEmpty() || "error".equalsIgnoreCase(cleanMessage)) && !dataText.trim().isEmpty()) {
            message = dataText;
        }
        return message.trim().isEmpty() ? "HTTP " + code : message;
    }

    private static void ensureBackgroundThread() {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            throw new IllegalStateException("ApiClient must not run on the main thread");
        }
    }

    private static String read(InputStream stream) throws IOException {
        if (stream == null) return "";
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }
        }
        return out.toString();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public interface StreamHandler {
        void onLine(String line) throws IOException;
    }
}
