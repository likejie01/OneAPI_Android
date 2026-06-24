package center.oneapi.mobile.features.keys;

import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import center.oneapi.mobile.core.ApiClient;
import center.oneapi.mobile.core.AppPrefs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class AppApiKeyManagerTest {
    @Test
    public void ensureOrThrow_keepsValidCachedKeyWithoutListingServerTokens() throws Exception {
        MemoryStore store = new MemoryStore("sk-valid", "android-test");
        RecordingApiClient client = new RecordingApiClient();

        new AppApiKeyManager(store).ensureOrThrow(client);

        assertEquals("sk-valid", store.key);
        assertEquals(1, client.paths.size());
        assertEquals("GET /api/usage/token bearer=sk-valid", client.paths.get(0));
        assertFalse(client.paths.toString(), client.paths.toString().contains("/api/token"));
    }

    private static final class MemoryStore implements AppApiKeyManager.Store {
        String key;
        final String appId;

        MemoryStore(String key, String appId) {
            this.key = key;
            this.appId = appId;
        }

        @Override
        public String appApiKey() {
            return key == null ? "" : key;
        }

        @Override
        public void setAppApiKey(String key) {
            this.key = key == null ? "" : key;
        }

        @Override
        public String appId() {
            return appId;
        }
    }

    private static final class RecordingApiClient extends ApiClient {
        final List<String> paths = new ArrayList<>();

        RecordingApiClient() {
            super(null);
        }

        @Override
        public JSONObject request(String method, String path, JSONObject body, int readTimeoutMs, String bearerTokenOverride) throws IOException {
            paths.add(method + " " + path + " bearer=" + (bearerTokenOverride == null ? "" : bearerTokenOverride));
            if ("/api/usage/token".equals(path)) return new JSONObject();
            throw new IOException("unexpected path: " + path);
        }

        @Override
        public JSONObject get(String path) throws IOException {
            paths.add("GET " + path);
            throw new IOException("token list should not be requested");
        }

        @Override
        public JSONObject post(String path, JSONObject body) throws IOException {
            paths.add("POST " + path);
            throw new IOException("token creation should not be requested");
        }
    }
}
