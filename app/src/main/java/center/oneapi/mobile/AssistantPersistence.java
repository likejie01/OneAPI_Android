package center.oneapi.mobile;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AssistantPersistence {
    private AssistantPersistence() {
    }

    static String encode(Store store) {
        JSONArray array = new JSONArray();
        if (store != null) {
            for (Item item : store.items()) {
                try {
                    array.put(new JSONObject()
                            .put("name", item.name)
                            .put("order", item.order)
                            .put("prompt", item.prompt));
                } catch (Exception ignored) {
                }
            }
        }
        return array.toString();
    }

    static Store decode(String raw) {
        Store store = new Store();
        String clean = raw == null ? "" : raw.trim();
        if (clean.isEmpty()) return store;
        try {
            JSONArray array = new JSONArray(clean);
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.optJSONObject(i);
                if (item == null) continue;
                store.upsert(item.optString("name", ""), item.optInt("order", 100), item.optString("prompt", ""));
            }
        } catch (Exception ignored) {
        }
        return store;
    }

    static final class Store {
        private final LinkedHashMap<String, Item> items = new LinkedHashMap<>();

        void upsert(String name, int order, String prompt) {
            String clean = clean(name);
            if (clean.isEmpty() || "＋ 新建助手".equals(clean)) return;
            items.put(clean, new Item(clean, order, prompt == null ? "" : prompt));
        }

        void rename(String oldName, String newName, int order, String prompt) {
            String oldClean = clean(oldName);
            String newClean = clean(newName);
            if (!oldClean.isEmpty() && !oldClean.equals(newClean)) {
                items.remove(oldClean);
            }
            upsert(newClean, order, prompt);
        }

        void remove(String name) {
            items.remove(clean(name));
        }

        String prompt(String name) {
            Item item = items.get(clean(name));
            return item == null ? "" : item.prompt;
        }

        int order(String name, int fallback) {
            Item item = items.get(clean(name));
            return item == null ? fallback : item.order;
        }

        List<Item> items() {
            return new ArrayList<>(items.values());
        }

        List<String> mergeNames(List<String> serverNames) {
            LinkedHashMap<String, Integer> orderByName = new LinkedHashMap<>();
            if (serverNames != null) {
                for (String name : serverNames) {
                    String clean = clean(name);
                    if (!clean.isEmpty()) orderByName.put(clean, 100);
                }
            }
            for (Map.Entry<String, Item> entry : items.entrySet()) {
                orderByName.put(entry.getKey(), entry.getValue().order);
            }
            List<String> merged = new ArrayList<>(orderByName.keySet());
            merged.sort((a, b) -> Integer.compare(orderByName.getOrDefault(a, 100), orderByName.getOrDefault(b, 100)));
            return merged;
        }

        private static String clean(String value) {
            return value == null ? "" : value.trim();
        }
    }

    static final class Item {
        final String name;
        final int order;
        final String prompt;

        Item(String name, int order, String prompt) {
            this.name = name;
            this.order = order;
            this.prompt = prompt;
        }
    }
}
