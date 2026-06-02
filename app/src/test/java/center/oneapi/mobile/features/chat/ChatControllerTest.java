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
}
