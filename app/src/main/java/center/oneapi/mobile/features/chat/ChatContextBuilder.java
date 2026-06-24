package center.oneapi.mobile.features.chat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import center.oneapi.mobile.ChatMessage;

public final class ChatContextBuilder {
    private ChatContextBuilder() {
    }

    public static JSONArray build(List<ChatMessage> messages, String contextWindow) {
        JSONArray context = new JSONArray();
        List<JSONObject> valid = validContextMessages(messages, limitFor(contextWindow));
        for (JSONObject item : valid) {
            context.put(item);
        }
        return context;
    }

    static int limitFor(String contextWindow) {
        String value = contextWindow == null ? "" : contextWindow.trim();
        if ("短上下文".equals(value)) return 8;
        if ("长上下文".equals(value)) return 40;
        return 20;
    }

    static String contextText(ChatMessage message) {
        String text = message == null ? "" : message.text;
        if (text == null) return "";
        String clean = textWithoutImageUris(text).trim();
        if (clean.equals("正在思考...") || clean.equals("oneapi://image-loading") || clean.equals("正在发送到桌面端...")) {
            return "";
        }
        clean = clean.replaceAll("(?is)<thinking>.*?</thinking>", "").trim();
        if (clean.length() > 4000) {
            clean = clean.substring(clean.length() - 4000).trim();
        }
        return clean;
    }

    private static List<JSONObject> validContextMessages(List<ChatMessage> messages, int limit) {
        List<JSONObject> out = new ArrayList<>();
        if (messages == null || messages.isEmpty() || limit <= 0) return out;
        for (int i = messages.size() - 1; i >= 0 && out.size() < limit; i--) {
            ChatMessage message = messages.get(i);
            if (message == null || message.log) continue;
            if (!"user".equals(message.role) && !"assistant".equals(message.role)) continue;
            String content = contextText(message);
            if (content.isEmpty()) continue;
            try {
                out.add(new JSONObject()
                        .put("role", message.role)
                        .put("content", content));
            } catch (Exception ignored) {
            }
        }
        Collections.reverse(out);
        return out;
    }

    private static String textWithoutImageUris(String text) {
        if (text == null) return "";
        StringBuilder out = new StringBuilder();
        for (String line : text.split("\\n")) {
            if (!isImageText(line.trim())) {
                if (out.length() > 0) out.append('\n');
                out.append(line);
            }
        }
        return out.toString().trim();
    }

    private static boolean isImageText(String text) {
        if (text == null) return false;
        String value = text.trim().toLowerCase(java.util.Locale.ROOT);
        return value.startsWith("data:image/")
                || value.startsWith("content:")
                || value.startsWith("file:")
                || value.endsWith(".png")
                || value.endsWith(".jpg")
                || value.endsWith(".jpeg")
                || value.endsWith(".webp");
    }
}
