package center.oneapi.mobile.features.chat;

import org.json.JSONObject;
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
    public void usage_formatsPromptCompletionAndTotalTokens() throws Exception {
        JSONObject usageJson = new JSONObject("{\"prompt_tokens\":1719,\"completion_tokens\":1912,\"total_tokens\":3631}");
        ChatController.Usage usage = ChatController.Usage.from(usageJson);

        assertEquals("Tokens: 1719 1912 3631", usage.displayText());
    }
}
