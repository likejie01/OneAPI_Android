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

public class ImageController {
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
        return extractImage(api.post("/pg/images/generations", buildGenerationRequest(prompt, size, quality, randomSeed)));
    }

    public String edit(Context context, Uri imageUri, String prompt, String size, String quality) throws Exception {
        String boundary = "----OneApiAndroid" + System.currentTimeMillis();
        HttpURLConnection connection = (HttpURLConnection) new URL(ApiClient.buildUrl(api.server(), "/v1/images/edits")).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(180000);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        if (!api.cookie().isEmpty()) connection.setRequestProperty("Cookie", api.cookie());
        if (!api.userId().isEmpty()) connection.setRequestProperty("New-Api-User", api.userId());
        if (!api.token().isEmpty()) connection.setRequestProperty("Authorization", "Bearer " + api.token());
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
            throw new java.io.IOException(json.optString("message", "图片编辑接口请求失败"));
        }
        return extractImage(json);
    }

    public static String extractImage(JSONObject response) {
        if (response == null) return "";
        JSONArray data = response.optJSONArray("data");
        if (data != null && data.length() > 0) {
            JSONObject first = data.optJSONObject(0);
            if (first != null) {
                String url = first.optString("url", first.optString("image_url", ""));
                if (!url.isEmpty()) return url;
                String b64 = first.optString("b64_json", first.optString("b64Json", ""));
                if (!b64.isEmpty()) return b64.startsWith("data:image/") ? b64 : "data:image/png;base64," + b64;
            }
        }
        String direct = response.optString("image", response.optString("result", ""));
        if (!direct.isEmpty() && !direct.startsWith("http") && !direct.startsWith("data:image/")) {
            return "data:image/png;base64," + direct;
        }
        return direct;
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
}
