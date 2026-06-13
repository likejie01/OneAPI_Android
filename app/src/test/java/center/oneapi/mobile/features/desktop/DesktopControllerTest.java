package center.oneapi.mobile.features.desktop;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DesktopControllerTest {
    @Test
    public void buildJob_keepsDesktopBridgeFields() throws Exception {
        DesktopController controller = new DesktopController(null);
        JSONArray extensions = new JSONArray()
                .put(new JSONObject().put("id", "skill-a").put("kind", "skill").put("name", "Skill A"));
        JSONArray attachments = new JSONArray()
                .put(new JSONObject().put("name", "photo.jpg").put("kind", "image").put("dataUrl", "data:image/jpeg;base64,abc"));

        JSONObject body = controller.buildJob(
                "codex",
                "device-1",
                "session-1",
                "prompt",
                "gpt-5.4-codex",
                "high",
                "full",
                extensions,
                "D:/WorkSpace/NewAPI/OneAPI_Android",
                "mobile",
                "android-1",
                attachments
        );

        assertEquals("codex", body.getString("client"));
        assertEquals("device-1", body.getString("deviceId"));
        assertEquals("session-1", body.getString("sessionId"));
        assertEquals("gpt-5.4-codex", body.getString("model"));
        assertEquals("high", body.getString("reasoningEffort"));
        assertEquals("full", body.getString("permissionMode"));
        assertEquals("mobile", body.getString("origin"));
        assertEquals("mobile", body.getString("source"));
        assertEquals("android-1", body.getString("clientRequestId"));
        assertEquals("skill-a", body.getJSONArray("extensionRefs").getJSONObject(0).getString("id"));
        assertEquals("data:image/jpeg;base64,abc", body.getJSONArray("attachments").getJSONObject(0).getString("dataUrl"));
    }
}
