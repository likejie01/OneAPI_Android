package center.oneapi.mobile.core;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ApiClientTest {
    @Test
    public void buildUrl_joinsBaseAndPath() {
        assertEquals("https://ai.oneapi.center/api/status", ApiClient.buildUrl("https://ai.oneapi.center/", "/api/status"));
        assertEquals("https://ai.oneapi.center/api/status", ApiClient.buildUrl("https://ai.oneapi.center", "api/status"));
    }

    @Test
    public void errorMessage_prefersJsonMessage() throws Exception {
        assertEquals("bad token", ApiClient.errorMessage(new JSONObject("{\"message\":\"bad token\"}"), 401));
    }
}
