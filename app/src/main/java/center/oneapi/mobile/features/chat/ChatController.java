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
        return buildRequest(model, systemPrompt, context, (Object) prompt, reasoningEffort);
    }

    public JSONObject buildRequest(String model, String systemPrompt, JSONArray context, Object userContent, String reasoningEffort) throws Exception {
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
        messages.put(new JSONObject().put("role", "user").put("content", userContent == null ? "" : userContent));
        body.put("messages", messages);
        return body;
    }

    public String send(String model, String systemPrompt, JSONArray context, String prompt, String reasoningEffort) throws Exception {
        return send(model, systemPrompt, context, (Object) prompt, reasoningEffort);
    }

    public String send(String model, String systemPrompt, JSONArray context, Object userContent, String reasoningEffort) throws Exception {
        JSONObject response = api.post("/pg/chat/completions", buildRequest(model, systemPrompt, context, userContent, reasoningEffort));
        return extractText(response);
    }

    public void stream(String model, String systemPrompt, JSONArray context, String prompt, String reasoningEffort, DeltaHandler handler) throws Exception {
        stream(model, systemPrompt, context, (Object) prompt, reasoningEffort, handler);
    }

    public void stream(String model, String systemPrompt, JSONArray context, Object userContent, String reasoningEffort, DeltaHandler handler) throws Exception {
        JSONObject body = buildRequest(model, systemPrompt, context, userContent, reasoningEffort);
        body.put("stream", true);
        body.put("stream_options", new JSONObject().put("include_usage", true));
        StringBuilder reasoning = new StringBuilder();
        StringBuilder answer = new StringBuilder();
        api.postStream("/pg/chat/completions", body, line -> {
            String clean = line == null ? "" : line.trim();
            if (!clean.startsWith("data:")) return;
            String data = clean.substring("data:".length()).trim();
            if (data.isEmpty() || "[DONE]".equals(data)) return;
            try {
                JSONObject event = new JSONObject(data);
                Usage usage = Usage.from(event.optJSONObject("usage"));
                if (!usage.isEmpty() && handler != null) handler.onUsage(usage);
                Delta delta = extractDeltaPart(event);
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
                String reasoning = reasoningFromObject(message);
                Object content = message.opt("content");
                String text = "";
            if (content instanceof String) text = nonNull((String) content);
            if (content instanceof JSONArray) text = textFromParts((JSONArray) content);
                return joinReasoning(reasoning, text).trim();
            }
        }
        String reasoning = reasoningFromObject(response);
        String text = firstString(response, "text", "message", "content").trim();
        if (text.isEmpty()) text = textFromParts(response.optJSONArray("output"));
        return joinReasoning(reasoning, text).trim();
    }

    private static String textFromParts(JSONArray parts) {
        if (parts == null) return "";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < parts.length(); i++) {
            Object raw = parts.opt(i);
            if (raw instanceof String) {
                String text = nonNull((String) raw);
                if (!text.isEmpty()) out.append(text);
                continue;
            }
            JSONObject part = parts.optJSONObject(i);
            if (part == null) continue;
            String text = firstString(part, "text", "content", "output_text", "input_text", "reasoning_text", "summary");
            if (text.isEmpty()) {
                Object nested = part.opt("content");
                if (nested instanceof JSONArray) text = textFromParts((JSONArray) nested);
            }
            if (text.isEmpty()) text = textFromParts(part.optJSONArray("summary"));
            if (text.isEmpty()) text = textFromParts(part.optJSONArray("parts"));
            if (!text.isEmpty()) out.append(text);
        }
        return out.toString();
    }

    private static String reasoningFromObject(JSONObject object) {
        if (object == null) return "";
        String reasoning = firstString(object,
                "reasoning_content",
                "reasoningContent",
                "reasoning",
                "reasoning_text",
                "reasoningText",
                "reasoning_summary",
                "thinking",
                "thinking_content",
                "thinkingContent",
                "thoughts");
        if (!reasoning.isEmpty()) return reasoning;
        reasoning = textFromParts(object.optJSONArray("reasoning_details"));
        if (!reasoning.isEmpty()) return reasoning;
        reasoning = textFromParts(object.optJSONArray("reasoningDetails"));
        if (!reasoning.isEmpty()) return reasoning;
        return reasoningFromParts(object.optJSONArray("output"));
    }

    private static String reasoningFromParts(JSONArray parts) {
        if (parts == null) return "";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < parts.length(); i++) {
            JSONObject part = parts.optJSONObject(i);
            if (part == null) continue;
            String type = firstString(part, "type", "kind").toLowerCase(java.util.Locale.ROOT);
            if (!type.contains("reason") && !type.contains("think")) continue;
            String text = firstString(part, "text", "content", "summary", "reasoning_text");
            if (text.isEmpty()) text = textFromParts(part.optJSONArray("summary"));
            if (text.isEmpty()) text = textFromParts(part.optJSONArray("content"));
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

    static Delta extractDeltaPart(JSONObject response) {
        JSONArray choices = response.optJSONArray("choices");
        if (choices == null || choices.length() == 0) {
            if (isReasoningEvent(response)) {
                String text = firstString(response, "delta", "text", "content", "message", "summary", "reasoning_text");
                if (!text.isEmpty()) return new Delta(text, true);
            }
            String reasoning = reasoningFromObject(response);
            if (!reasoning.isEmpty()) return new Delta(reasoning, true);
            return new Delta(firstString(response, "text", "content", "message", "delta"), false);
        }
        JSONObject choice = choices.optJSONObject(0);
        if (choice == null) return new Delta("", false);
        JSONObject delta = choice.optJSONObject("delta");
        if (delta != null) {
            String reasoning = reasoningFromObject(delta);
            if (!reasoning.isEmpty()) return new Delta(reasoning, true);
            Object content = delta.opt("content");
            if (content instanceof String) return new Delta(nonNull((String) content), false);
            if (content instanceof JSONArray) return new Delta(textFromParts((JSONArray) content), false);
        }
        JSONObject message = choice.optJSONObject("message");
        if (message != null) {
            String reasoning = reasoningFromObject(message);
            if (!reasoning.isEmpty()) return new Delta(reasoning, true);
            Object content = message.opt("content");
            if (content instanceof String) return new Delta(nonNull((String) content), false);
            if (content instanceof JSONArray) return new Delta(textFromParts((JSONArray) content), false);
        }
        return new Delta(firstString(response, "text", "content", "message"), false);
    }

    private static boolean isReasoningEvent(JSONObject response) {
        String type = firstString(response, "type", "event", "kind").toLowerCase(java.util.Locale.ROOT);
        return type.contains("reason") || type.contains("think");
    }

    private static String firstString(JSONObject object, String... keys) {
        if (object == null) return "";
        for (String key : keys) {
            if (!object.has(key) || object.isNull(key)) continue;
            Object raw = object.opt(key);
            String value = raw instanceof String ? nonNull((String) raw) : nonNull(String.valueOf(raw));
            if (!value.trim().isEmpty()) return value;
        }
        return "";
    }

    private static String joinReasoning(String reasoning, String answer) {
        String cleanReasoning = clean(reasoning);
        String cleanAnswer = answer == null ? "" : answer;
        if (cleanReasoning.isEmpty()) return cleanAnswer;
        return "<thinking>" + cleanReasoning + "</thinking>\n\n" + cleanAnswer;
    }

    static class Delta {
        final String text;
        final boolean reasoning;

        Delta(String text, boolean reasoning) {
            this.text = text == null ? "" : text;
            this.reasoning = reasoning;
        }
    }

    public interface DeltaHandler {
        void onDelta(String fullText, boolean done);

        default void onUsage(Usage usage) {
        }
    }

    public static final class Usage {
        public final int promptTokens;
        public final int completionTokens;
        public final int cachedTokens;
        public final int totalTokens;

        private Usage(int promptTokens, int completionTokens, int cachedTokens, int totalTokens) {
            this.promptTokens = Math.max(0, promptTokens);
            this.completionTokens = Math.max(0, completionTokens);
            this.cachedTokens = Math.max(0, cachedTokens);
            this.totalTokens = Math.max(0, totalTokens);
        }

        public static Usage from(JSONObject usage) {
            if (usage == null) return new Usage(0, 0, 0, 0);
            int prompt = firstPositive(usage, "prompt_tokens", "input_tokens");
            int completion = firstPositive(usage, "completion_tokens", "output_tokens");
            int cached = firstPositive(usage, "cached_tokens", "cache_read_input_tokens", "cache_creation_input_tokens");
            JSONObject promptDetails = usage.optJSONObject("prompt_tokens_details");
            if (promptDetails != null) {
                cached = Math.max(cached, firstPositive(promptDetails, "cached_tokens"));
            }
            JSONObject inputDetails = usage.optJSONObject("input_token_details");
            if (inputDetails != null) {
                cached = Math.max(cached, firstPositive(inputDetails, "cache_read", "cache_creation", "cached_tokens"));
            }
            int total = firstPositive(usage, "total_tokens");
            if (total <= 0 && (prompt > 0 || completion > 0)) total = prompt + completion;
            return new Usage(prompt, completion, cached, total);
        }

        public boolean isEmpty() {
            return promptTokens <= 0 && completionTokens <= 0 && cachedTokens <= 0 && totalTokens <= 0;
        }

        public String displayText() {
            if (isEmpty()) return "";
            String text = "输入：" + promptTokens + " | 输出：" + completionTokens + " | 缓存：" + cachedTokens;
            if (totalTokens > 0 && promptTokens <= 0 && completionTokens <= 0) {
                text += " | 总计：" + totalTokens;
            }
            return text;
        }

        private static int firstPositive(JSONObject object, String... keys) {
            for (String key : keys) {
                int value = object.optInt(key, 0);
                if (value > 0) return value;
            }
            return 0;
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String nonNull(String value) {
        if (value == null) return "";
        return "null".equalsIgnoreCase(value.trim()) ? "" : value;
    }
}
