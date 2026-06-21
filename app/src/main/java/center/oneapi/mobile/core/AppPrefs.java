package center.oneapi.mobile.core;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

public class AppPrefs {
    public static final String DEFAULT_SERVER = "https://ai.oneapi.center";
    private static final String NAME = "oneapi_mobile";

    private final SharedPreferences prefs;

    public AppPrefs(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(NAME, Context.MODE_PRIVATE);
        ensureDefaults();
    }

    public String server() {
        return prefs.getString("server", DEFAULT_SERVER);
    }

    public void setServer(String server) {
        prefs.edit().putString("server", cleanServer(server)).apply();
    }

    public String token() {
        return prefs.getString("token", "");
    }

    public void setToken(String token) {
        prefs.edit().putString("token", token == null ? "" : token.trim()).apply();
    }

    public String appApiKey() {
        return prefs.getString("app_api_key", "");
    }

    public void setAppApiKey(String key) {
        prefs.edit().putString("app_api_key", key == null ? "" : key.trim()).apply();
    }

    public String cookie() {
        return prefs.getString("cookie", "");
    }

    public void setCookie(String cookie) {
        prefs.edit().putString("cookie", cookie == null ? "" : cookie.trim()).apply();
    }

    public String userId() {
        return prefs.getString("user_id", "");
    }

    public void setUserId(String userId) {
        prefs.edit().putString("user_id", userId == null ? "" : userId.trim()).apply();
    }

    public String username() {
        return prefs.getString("username", "");
    }

    public void setUsername(String username) {
        prefs.edit().putString("username", username == null ? "" : username.trim()).apply();
    }

    public boolean isLoggedIn() {
        return !token().isEmpty() || !cookie().isEmpty();
    }

    public String appId() {
        return prefs.getString("app_id", "");
    }

    public String boundDeviceId() {
        return prefs.getString("bound_device_id", "");
    }

    public void setBoundDeviceId(String deviceId) {
        prefs.edit().putString("bound_device_id", deviceId == null ? "" : deviceId.trim()).apply();
    }

    private void ensureDefaults() {
        SharedPreferences.Editor editor = prefs.edit();
        boolean changed = false;
        if (!prefs.contains("server")) {
            editor.putString("server", DEFAULT_SERVER);
            changed = true;
        }
        if (!prefs.contains("app_id") || prefs.getString("app_id", "").trim().isEmpty()) {
            editor.putString("app_id", "android-" + UUID.randomUUID());
            changed = true;
        }
        if (changed) {
            editor.apply();
        }
    }

    private static String cleanServer(String server) {
        String value = server == null ? "" : server.trim();
        if (value.isEmpty()) return DEFAULT_SERVER;
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
