package center.oneapi.mobile.features.chat;

import org.json.JSONObject;
import org.json.JSONArray;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ChatControllerTest {
    @Test
    public void extractText_readsOpenAiChoice() throws Exception {
        JSONObject response = new JSONObject("{\"choices\":[{\"message\":{\"content\":\"hello\"}}]}");
        assertEquals("hello", ChatController.extractText(response));
    }

    @Test
    public void extractDeltaPart_classifiesResponsesReasoningEvents() throws Exception {
        JSONObject response = new JSONObject("{\"type\":\"response.reasoning_summary_text.delta\",\"delta\":\"thinking text\"}");
        ChatController.Delta delta = ChatController.extractDeltaPart(response);

        assertEquals("thinking text", delta.text);
        assertEquals(true, delta.reasoning);
    }

    @Test
    public void extractDeltaPart_readsChoiceReasoningContent() throws Exception {
        JSONObject response = new JSONObject("{\"choices\":[{\"delta\":{\"reasoning_content\":\"hidden\"}}]}");
        ChatController.Delta delta = ChatController.extractDeltaPart(response);

        assertEquals("hidden", delta.text);
        assertEquals(true, delta.reasoning);
    }

    @Test
    public void extractDeltaPart_readsArrayContent() throws Exception {
        JSONObject response = new JSONObject("{\"choices\":[{\"delta\":{\"content\":[{\"type\":\"text\",\"text\":\"hello\"},{\"type\":\"text\",\"text\":\" world\"}]}}]}");
        ChatController.Delta delta = ChatController.extractDeltaPart(response);

        assertEquals("hello world", delta.text);
        assertEquals(false, delta.reasoning);
    }

    @Test
    public void extractDeltaPart_readsObjectMessageContent() throws Exception {
        JSONObject response = new JSONObject("{\"message\":{\"content\":[{\"text\":\"fallback reply\"}]}}");
        ChatController.Delta delta = ChatController.extractDeltaPart(response);

        assertEquals("fallback reply", delta.text);
        assertEquals(false, delta.reasoning);
    }

    @Test
    public void parseStreamPayload_detectsDoneSentinel() {
        ChatController.StreamPayload payload = ChatController.parseStreamPayload("[DONE]");

        assertEquals(true, payload.done);
    }

    @Test
    public void usage_formatsPromptCompletionAndTotalTokens() throws Exception {
        JSONObject usageJson = new JSONObject("{\"prompt_tokens\":1719,\"completion_tokens\":1912,\"total_tokens\":3631,\"prompt_tokens_details\":{\"cached_tokens\":128}}");
        ChatController.Usage usage = ChatController.Usage.from(usageJson);

        assertEquals("Token 输入：1719 | 输出：1912 | 缓存：128", usage.displayText());
    }

    @Test
    public void buildRequest_includesContextBeforeCurrentUserMessage() throws Exception {
        ChatController controller = new ChatController(null);
        JSONArray context = new JSONArray()
                .put(new JSONObject().put("role", "user").put("content", "上一轮问题"))
                .put(new JSONObject().put("role", "assistant").put("content", "上一轮回答"));

        JSONObject request = controller.buildRequest("gpt-test", "system", context, "当前问题", "off");
        JSONArray messages = request.getJSONArray("messages");

        assertEquals("system", messages.getJSONObject(0).getString("role"));
        assertEquals("上一轮问题", messages.getJSONObject(1).getString("content"));
        assertEquals("上一轮回答", messages.getJSONObject(2).getString("content"));
        assertEquals("当前问题", messages.getJSONObject(3).getString("content"));
    }
}
