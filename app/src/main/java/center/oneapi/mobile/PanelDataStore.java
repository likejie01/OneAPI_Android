package center.oneapi.mobile;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class PanelDataStore {
    private static final String NAME = "oneapi_mobile_panel_cache";
    private static final String DATA_PREFIX = "data:";
    private static final String TIME_PREFIX = "time:";

    private final SharedPreferences prefs;
    private final Map<String, JSONObject> memory = new ConcurrentHashMap<>();
    private final Map<String, Long> memoryAt = new ConcurrentHashMap<>();

    PanelDataStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    Entry get(String key) {
        String cleanKey = clean(key);
        if (cleanKey.isEmpty()) return null;
        JSONObject cached = memory.get(cleanKey);
        long cachedAt = memoryAt.getOrDefault(cleanKey, 0L);
        if (cached != null && cachedAt > 0L) {
            return new Entry(copy(cached), cachedAt);
        }
        String raw = prefs.getString(DATA_PREFIX + cleanKey, "");
        long savedAt = prefs.getLong(TIME_PREFIX + cleanKey, 0L);
        if (raw == null || raw.trim().isEmpty() || savedAt <= 0L) return null;
        try {
            JSONObject parsed = new JSONObject(raw);
            memory.put(cleanKey, copy(parsed));
            memoryAt.put(cleanKey, savedAt);
            return new Entry(parsed, savedAt);
        } catch (Exception ignored) {
            remove(cleanKey);
            return null;
        }
    }

    void put(String key, JSONObject value, long savedAt) {
        String cleanKey = clean(key);
        if (cleanKey.isEmpty() || value == null) return;
        JSONObject copy = copy(value);
        memory.put(cleanKey, copy);
        memoryAt.put(cleanKey, savedAt);
        prefs.edit()
                .putString(DATA_PREFIX + cleanKey, copy.toString())
                .putLong(TIME_PREFIX + cleanKey, savedAt)
                .apply();
    }

    void remove(String key) {
        String cleanKey = clean(key);
        if (cleanKey.isEmpty()) return;
        memory.remove(cleanKey);
        memoryAt.remove(cleanKey);
        prefs.edit()
                .remove(DATA_PREFIX + cleanKey)
                .remove(TIME_PREFIX + cleanKey)
                .apply();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static JSONObject copy(JSONObject value) {
        if (value == null) return new JSONObject();
        try {
            return new JSONObject(value.toString());
        } catch (Exception ignored) {
            return new JSONObject();
        }
    }

    static final class Entry {
        final JSONObject data;
        final long savedAt;

        Entry(JSONObject data, long savedAt) {
            this.data = data == null ? new JSONObject() : data;
            this.savedAt = savedAt;
        }

        boolean fresh(long now, long ttlMs) {
            return savedAt > 0L && ttlMs > 0L && now - savedAt < ttlMs;
        }
    }
}
