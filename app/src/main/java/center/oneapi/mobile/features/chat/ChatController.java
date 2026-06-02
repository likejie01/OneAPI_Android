package center.oneapi.mobile.features.chat;

import org.json.JSONArray;
import org.json.JSONObject;

import center.oneapi.mobile.core.ApiClient;

public class ChatController {
    private final ApiClient api;

    public ChatController(ApiClient api) {
        this.api = api;
    }

    public JSONObject buildRequest(String model, String systemPrompt, JSONArray context, String prompt, String reasoningEffort) throws Exception {
        JSONObject body = new JSONObject();
        body.put("model", clean(model).isEmpty() ? "gpt-5.4" : model);
        body.put("stream", false);
        String reasoning = clean(reasoningEffort);
        if (!reasoning.isEmpty() && !"off".equals(reasoning)) {
            body.put("reasoning_effort", reasoning);
        }
        JSONArray messages = new JSONArray();
        if (!clean(systemPrompt).isEmpty()) {
            messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));
        }
        if (context != null) {
            for (int i = 0; i < context.length(); i++) {
                JSONObject item = context.optJSONObject(i);
                if (item != null) messages.put(item);
            }
        }
        messages.put(new JSONObject().put("role", "user").put("content", prompt));
        body.put("messages", messages);
        return body;
    }

    public String send(String model, String systemPrompt, JSONArray context, String prompt, String reasoningEffort) throws Exception {
        JSONObject response = api.post("/pg/chat/completions", buildRequest(model, systemPrompt, context, prompt, reasoningEffort));
        return extractText(response);
    }

    public void stream(String model, String systemPrompt, JSONArray context, String prompt, String reasoningEffort, DeltaHandler handler) throws Exception {
        JSONObject body = buildRequest(model, systemPrompt, context, prompt, reasoningEffort);
        body.put("stream", true);
        StringBuilder reasoning = new StringBuilder();
        StringBuilder answer = new StringBuilder();
        api.postStream("/pg/chat/completions", body, line -> {
            String clean = line == null ? "" : line.trim();
            if (!clean.startsWith("data:")) return;
            String data = clean.substring("data:".length()).trim();
            if (data.isEmpty() || "[DONE]".equals(data)) return;
            try {
                Delta delta = extractDeltaPart(new JSONObject(data));
                if (!delta.text.isEmpty()) {
                    if (delta.reasoning) reasoning.append(delta.text);
                    else answer.append(delta.text);
                    if (handler != null) handler.onDelta(joinReasoning(reasoning.toString(), answer.toString()), false);
                }
            } catch (Exception ignored) {
            }
        });
        if (handler != null) handler.onDelta(joinReasoning(reasoning.toString(), answer.toString()), true);
    }

    public static String extractText(JSONObject response) {
        if (response == null) return "";
        JSONArray choices = response.optJSONArray("choices");
        if (choices != null && choices.length() > 0) {
            JSONObject message = choices.optJSONObject(0) == null ? null : choices.optJSONObject(0).optJSONObject("message");
            if (message != null) {
                String reasoning = firstString(message, "reasoning_content", "reasoning", "reasoning_text", "thoughts", "thinking");
                if (reasoning.isEmpty()) reasoning = textFromParts(message.optJSONArray("reasoning_details"));
                Object content = message.opt("content");
                String text = "";
                if (content instanceof String) text = clean((String) content);
                if (content instanceof JSONArray) text = textFromParts((JSONArray) content).trim();
                return joinReasoning(reasoning, text).trim();
            }
        }
        return firstString(response, "text", "message", "content").trim();
    }

    private static String textFromParts(JSONArray parts) {
        if (parts == null) return "";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < parts.length(); i++) {
            Object raw = parts.opt(i);
            if (raw instanceof String) {
                String text = clean((String) raw);
                if (!text.isEmpty()) out.append(text);
                continue;
            }
            JSONObject part = parts.optJSONObject(i);
            if (part == null) continue;
            String text = firstString(part, "text", "content", "output_text", "input_text", "reasoning_text");
            if (text.isEmpty()) {
                Object nested = part.opt("content");
                if (nested instanceof JSONArray) text = textFromParts((JSONArray) nested);
            }
            if (!text.isEmpty()) out.append(text);
        }
        return out.toString();
    }

    private static String extractDelta(JSONObject response) {
        JSONArray choices = response.optJSONArray("choices");
        if (choices == null || choices.length() == 0) return "";
        JSONObject choice = choices.optJSONObject(0);
        if (choice == null) return "";
        JSONObject delta = choice.optJSONObject("delta");
        if (delta != null) {
            Object content = delta.opt("content");
            if (content instanceof String) return (String) content;
            if (content instanceof JSONArray) return textFromParts((JSONArray) content);
        }
        JSONObject message = choice.optJSONObject("message");
        if (message != null) {
            Object content = message.opt("content");
            if (content instanceof String) return (String) content;
            if (content instanceof JSONArray) return textFromParts((JSONArray) content);
        }
        return firstString(response, "text", "content", "message");
    }

    private static Delta extractDeltaPart(JSONObject response) {
        JSONArray choices = response.optJSONArray("choices");
        if (choices == null || choices.length() == 0) {
            String reasoning = firstString(response, "reasoning_content", "reasoning", "reasoning_text", "thoughts", "thinking");
            if (!reasoning.isEmpty()) return new Delta(reasoning, true);
            return new Delta(firstString(response, "text", "content", "message", "delta"), false);
        }
        JSONObject choice = choices.optJSONObject(0);
        if (choice == null) return new Delta("", false);
        JSONObject delta = choice.optJSONObject("delta");
        if (delta != null) {
            String reasoning = firstString(delta, "reasoning_content", "reasoning", "reasoning_text", "thoughts", "thinking");
            if (reasoning.isEmpty()) reasoning = textFromParts(delta.optJSONArray("reasoning_details"));
            if (!reasoning.isEmpty()) return new Delta(reasoning, true);
            Object content = delta.opt("content");
            if (content instanceof String) return new Delta(clean((String) content), false);
            if (content instanceof JSONArray) return new Delta(textFromParts((JSONArray) content), false);
        }
        JSONObject message = choice.optJSONObject("message");
        if (message != null) {
            String reasoning = firstString(message, "reasoning_content", "reasoning", "reasoning_text", "thoughts", "thinking");
            if (reasoning.isEmpty()) reasoning = textFromParts(message.optJSONArray("reasoning_details"));
            if (!reasoning.isEmpty()) return new Delta(reasoning, true);
            Object content = message.opt("content");
            if (content instanceof String) return new Delta(clean((String) content), false);
            if (content instanceof JSONArray) return new Delta(textFromParts((JSONArray) content), false);
        }
        return new Delta(firstString(response, "text", "content", "message"), false);
    }

    private static String firstString(JSONObject object, String... keys) {
        if (object == null) return "";
        for (String key : keys) {
            if (!object.has(key) || object.isNull(key)) continue;
            Object raw = object.opt(key);
            String value = raw instanceof String ? clean((String) raw) : clean(String.valueOf(raw));
            if ("null".equalsIgnoreCase(value)) continue;
            if (!value.isEmpty()) return value;
        }
        return "";
    }

    private static String joinReasoning(String reasoning, String answer) {
        String cleanReasoning = clean(reasoning);
        String cleanAnswer = answer == null ? "" : answer;
        if (cleanReasoning.isEmpty()) return cleanAnswer;
        return "<thinking>" + cleanReasoning + "</thinking>\n\n" + cleanAnswer;
    }

    private static class Delta {
        final String text;
        final boolean reasoning;

        Delta(String text, boolean reasoning) {
            this.text = text == null ? "" : text;
            this.reasoning = reasoning;
        }
    }

    public interface DeltaHandler {
        void onDelta(String fullText, boolean done);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
