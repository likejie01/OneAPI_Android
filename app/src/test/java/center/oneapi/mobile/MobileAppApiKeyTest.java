package center.oneapi.mobile;

import org.json.JSONObject;
import org.json.JSONArray;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MobileAppApiKeyTest {
    @Test
    public void appApiKeyCreatePath_matchesServerPostRoute() {
        assertEquals("/api/token/", MainActivity.APP_API_KEY_CREATE_PATH);
    }

    @Test
    public void buildAppApiKeyCreatePayload_usesAllChannelGroups() throws Exception {
        JSONObject body = MainActivity.buildAppApiKeyCreatePayload("android-1");

        assertEquals("OneAPIapp", body.getString("name"));
        assertEquals("", body.getString("group"));
        assertEquals(-1, body.getLong("expired_time"));
        assertTrue(body.getBoolean("unlimited_quota"));
        assertFalse(body.getBoolean("model_limits_enabled"));
        assertFalse(body.getBoolean("cross_group_retry"));
    }

    @Test
    public void buildAppApiKeyCreatePayload_keepsNameWithinServerLimit() throws Exception {
        JSONObject body = MainActivity.buildAppApiKeyCreatePayload("android-12345678-1234-1234-1234-123456789abc");

        assertEquals("OneAPIapp", body.getString("name"));
        assertTrue(body.getString("name").length() <= 50);
    }

    @Test
    public void isAppApiKeyToken_matchesStableMobileTokenName() throws Exception {
        assertTrue(MainActivity.isAppApiKeyToken(new JSONObject().put("name", "OneAPIapp")));
        assertTrue(MainActivity.isLegacyAppApiKeyToken(new JSONObject().put("name", "OneAPI Android App android-1")));
        assertFalse(MainActivity.isAppApiKeyToken(new JSONObject().put("name", "OneAPI Android App android-1")));
        assertFalse(MainActivity.isAppApiKeyToken(new JSONObject().put("name", "desktop key")));
    }

    @Test
    public void appApiKeyTokenIds_returnsCurrentAppKeysOnly() throws Exception {
        JSONArray tokens = new JSONArray()
                .put(new JSONObject().put("id", 2).put("name", "desktop key"))
                .put(new JSONObject().put("id", 3).put("name", "OneAPI Android App old"))
                .put(new JSONObject().put("id", 7).put("name", "OneAPIapp"));

        assertEquals(Arrays.asList(7), MainActivity.appApiKeyTokenIds(tokens));
    }

    @Test
    public void appApiKeyMutableTokenIds_excludesEveryNonAppKey() throws Exception {
        JSONArray tokens = new JSONArray()
                .put(new JSONObject().put("id", 1).put("name", "OneAPI Desktop App"))
                .put(new JSONObject().put("id", 2).put("name", "My OneAPI Android App backup"))
                .put(new JSONObject().put("id", 3).put("name", "OneAPI Android Application"))
                .put(new JSONObject().put("id", 4).put("name", ""))
                .put(new JSONObject().put("id", 0).put("name", "OneAPI Android App invalid"))
                .put(new JSONObject().put("id", 8).put("name", "OneAPIapp"))
                .put(new JSONObject().put("id", 5).put("name", "OneAPI Android App"));

        assertEquals(Arrays.asList(8), MainActivity.appApiKeyMutableTokenIds(tokens));
    }

    @Test
    public void appApiKeyMutableTokenIds_readsSearchEnvelopeItems() throws Exception {
        JSONObject envelope = new JSONObject()
                .put("success", true)
                .put("data", new JSONObject()
                        .put("page", 1)
                        .put("page_size", 20)
                        .put("items", new JSONArray()
                                .put(new JSONObject().put("id", 42).put("name", "OneAPIapp"))
                                .put(new JSONObject().put("id", 41).put("name", "OneAPI Desktop App"))));

        assertEquals(Arrays.asList(42), MainActivity.appApiKeyMutableTokenIds(envelope));
    }
}
