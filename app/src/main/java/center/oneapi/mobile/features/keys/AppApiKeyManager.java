package center.oneapi.mobile.features.keys;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import center.oneapi.mobile.core.ApiClient;
import center.oneapi.mobile.core.AppPrefs;

public class AppApiKeyManager {
    private static final String TAG = "OneAPI-AppKey";
    public static final String CREATE_PATH = "/api/token/";
    private static final String APP_KEY_NAME = "OneAPIapp";

    private final Store store;

    public AppApiKeyManager(AppPrefs prefs) {
        this(new Store() {
            @Override
            public String appApiKey() {
                return prefs.appApiKey();
            }

            @Override
            public void setAppApiKey(String key) {
                prefs.setAppApiKey(key);
            }

            @Override
            public String appId() {
                return prefs.appId();
            }
        });
    }

    AppApiKeyManager(Store store) {
        this.store = store;
    }

    public void ensureOrThrow(ApiClient client) throws Exception {
        String current = store.appApiKey();
        if (!current.isEmpty()) {
            KeyValidation validation = validate(client, current);
            if (validation.status == KeyValidationStatus.VALID || validation.status == KeyValidationStatus.UNKNOWN) return;
            store.setAppApiKey("");
        }
        String key = fetchValidExisting(client, true);
        if (key.isEmpty()) {
            try {
                client.post(CREATE_PATH, buildCreatePayload(store.appId()));
            } catch (Exception error) {
                throw new IllegalStateException("创建 App 专用 Key 失败（" + CREATE_PATH + "）：" + message(error), error);
            }
            key = fetchValidExisting(client, false);
        }
        if (key.isEmpty()) {
            throw new IllegalStateException("服务器未返回可用 App 专用 Key。已调用 /api/token 创建，但 /api/token 列表或 /api/token/:id/key 未返回可用密钥。");
        }
        store.setAppApiKey(key);
    }

    public void invalidateAndEnsure(ApiClient client) throws Exception {
        store.setAppApiKey("");
        ensureOrThrow(client);
    }

    private KeyValidation validate(ApiClient client, String key) {
        if (key == null || key.trim().isEmpty()) {
            return new KeyValidation(KeyValidationStatus.INVALID, "empty key");
        }
        try {
            client.request("GET", "/api/usage/token", null, 15000, key);
            return new KeyValidation(KeyValidationStatus.VALID, "");
        } catch (Exception usageError) {
            String usageMessage = message(usageError);
            if (isInvalidKeyMessage(usageMessage)) {
                return new KeyValidation(KeyValidationStatus.INVALID, usageMessage);
            }
            try {
                client.request("GET", "/v1/models", null, 15000, key);
                return new KeyValidation(KeyValidationStatus.VALID, "");
            } catch (Exception modelsError) {
                String modelsMessage = message(modelsError);
                if (isInvalidKeyMessage(modelsMessage)) {
                    return new KeyValidation(KeyValidationStatus.INVALID, modelsMessage);
                }
                return new KeyValidation(KeyValidationStatus.UNKNOWN, usageMessage + "；/v1/models：" + modelsMessage);
            }
        }
    }

    private String fetchValidExisting(ApiClient client, boolean deleteInvalid) throws Exception {
        List<Integer> ids = fetchTokenIds(client);
        for (int id : ids) {
            String key = fetchTokenKey(client, id);
            if (key.isEmpty()) {
                continue;
            }
            KeyValidation validation = validate(client, key);
            if (validation.status == KeyValidationStatus.VALID || validation.status == KeyValidationStatus.UNKNOWN) {
                if (validation.status == KeyValidationStatus.UNKNOWN) {
                    Log.w(TAG, "app api key validation inconclusive, keeping key " + id + ": " + validation.message);
                }
                return key;
            }
            if (deleteInvalid && validation.status == KeyValidationStatus.INVALID) {
                try {
                    client.delete("/api/token/" + id);
                } catch (Exception error) {
                    Log.w(TAG, "failed to delete invalid app api key " + id, error);
                }
            }
        }
        return "";
    }

    private List<Integer> fetchTokenIds(ApiClient client) throws Exception {
        String appKeyName = name();
        try {
            JSONObject searchEnvelope = client.get("/api/token/search?keyword=" + ApiClient.enc(appKeyName) + "&p=1&size=100");
            List<Integer> searchIds = mutableTokenIds(searchEnvelope);
            if (!searchIds.isEmpty()) return searchIds;
        } catch (Exception error) {
            Log.w(TAG, "search app api key failed, fallback to token list", error);
        }
        JSONObject envelope;
        try {
            envelope = client.get("/api/token/?p=1&size=100");
        } catch (Exception error) {
            throw new IllegalStateException("读取 App 专用 Key 列表失败（/api/token）： " + message(error), error);
        }
        return mutableTokenIds(envelope);
    }

    private String fetchTokenKey(ApiClient client, int id) throws Exception {
        if (id <= 0) return "";
        JSONObject keyEnvelope;
        try {
            keyEnvelope = client.post("/api/token/" + id + "/key", new JSONObject());
        } catch (Exception error) {
            throw new IllegalStateException("读取 App 专用 Key 明文失败（/api/token/" + id + "/key）： " + message(error), error);
        }
        JSONObject data = keyEnvelope.optJSONObject("data");
        String key = data == null ? "" : first(data, "key", "token");
        if (key.isEmpty()) key = keyEnvelope.optString("key", "");
        return key.trim();
    }

    public static List<Integer> tokenIds(JSONArray tokens) {
        return mutableTokenIds(tokens);
    }

    public static List<Integer> mutableTokenIds(JSONObject envelope) {
        return mutableTokenIds(tokenItems(envelope));
    }

    public static List<Integer> mutableTokenIds(JSONArray tokens) {
        List<Integer> ids = new ArrayList<>();
        if (tokens == null) return ids;
        for (int i = 0; i < tokens.length(); i++) {
            JSONObject item = tokens.optJSONObject(i);
            if (!canMutate(item)) continue;
            int id = item.optInt("id", 0);
            if (id > 0) ids.add(id);
        }
        ids.sort((a, b) -> Integer.compare(b, a));
        return ids;
    }

    private static JSONArray tokenItems(JSONObject envelope) {
        Object data = envelope == null ? null : envelope.opt("data");
        if (data instanceof JSONArray) return (JSONArray) data;
        if (data instanceof JSONObject) {
            JSONObject object = (JSONObject) data;
            JSONArray items = object.optJSONArray("items");
            if (items != null) return items;
            JSONArray rows = object.optJSONArray("data");
            if (rows != null) return rows;
        }
        return new JSONArray();
    }

    public static JSONObject buildCreatePayload(String appId) throws Exception {
        JSONObject body = new JSONObject();
        body.put("name", name());
        body.put("expired_time", -1);
        body.put("remain_quota", 0);
        body.put("unlimited_quota", true);
        body.put("model_limits_enabled", false);
        body.put("model_limits", "");
        body.put("group", "");
        body.put("cross_group_retry", false);
        return body;
    }

    public static String name() {
        return APP_KEY_NAME;
    }

    public static boolean isAppToken(JSONObject item) {
        if (item == null) return false;
        String name = item.optString("name", "").trim();
        return name.equals(APP_KEY_NAME);
    }

    public static boolean isLegacyAppToken(JSONObject item) {
        if (item == null) return false;
        String name = item.optString("name", "").trim();
        return name.equals("OneAPI Android App") || name.startsWith("OneAPI Android App ");
    }

    public static boolean canMutate(JSONObject item) {
        return isAppToken(item) && item != null && item.optInt("id", 0) > 0;
    }

    public static boolean isInvalidKeyMessage(String message) {
        String normalized = message == null ? "" : message.trim().toLowerCase(Locale.ROOT);
        return isAuthExpiredMessage(message)
                || normalized.contains("token invalid")
                || normalized.contains("invalid token")
                || normalized.contains("token is invalid")
                || normalized.contains("token not provided")
                || normalized.contains("token expired")
                || normalized.contains("token exhausted")
                || normalized.contains("令牌无效")
                || normalized.contains("令牌不存在")
                || normalized.contains("令牌已过期")
                || normalized.contains("令牌额度已用尽");
    }

    private static boolean isAuthExpiredMessage(String message) {
        String normalized = message == null ? "" : message.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("not logged in and no access token provided")
                || normalized.contains("access token invalid")
                || normalized.contains("未登录且未提供 access token")
                || normalized.contains("access token 无效");
    }

    private static String first(JSONObject item, String... keys) {
        for (String key : keys) {
            String value = item.optString(key, "").trim();
            if (!value.isEmpty()) return value;
        }
        return "";
    }

    private static String message(Exception error) {
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }

    interface Store {
        String appApiKey();

        void setAppApiKey(String key);

        String appId();
    }

    private enum KeyValidationStatus {
        VALID,
        INVALID,
        UNKNOWN
    }

    private static final class KeyValidation {
        final KeyValidationStatus status;
        final String message;

        KeyValidation(KeyValidationStatus status, String message) {
            this.status = status;
            this.message = message == null ? "" : message;
        }
    }
}
