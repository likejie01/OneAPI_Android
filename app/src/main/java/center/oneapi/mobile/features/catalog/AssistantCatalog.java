package center.oneapi.mobile.features.catalog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AssistantCatalog {
    private AssistantCatalog() {
    }

    public static Result fromMobileAssistants(JSONArray data) {
        Result result = new Result();
        if (data == null) return result;
        for (int i = 0; i < data.length(); i++) {
            JSONObject item = data.optJSONObject(i);
            if (item == null) continue;
            String name = first(item, "name", "title", "assistantName");
            if (name.isEmpty()) continue;
            String scope = item.optString("scope", "chat");
            String prompt = item.optString("prompt", "");
            if ("draw".equals(scope) || "image".equals(scope)) {
                addUnique(result.image, name);
                result.imagePrompts.put(name, prompt);
            } else {
                addUnique(result.chat, name);
                result.chatPrompts.put(name, prompt);
            }
        }
        return result;
    }

    private static void addUnique(List<String> list, String value) {
        if (!list.contains(value)) list.add(value);
    }

    private static String first(JSONObject object, String... keys) {
        if (object == null) return "";
        for (String key : keys) {
            String value = object.optString(key, "").trim();
            if (!value.isEmpty() && !"null".equalsIgnoreCase(value)) return value;
        }
        return "";
    }

    public static final class Result {
        public final List<String> chat = new ArrayList<>();
        public final List<String> image = new ArrayList<>();
        public final Map<String, String> chatPrompts = new HashMap<>();
        public final Map<String, String> imagePrompts = new HashMap<>();
    }
}
