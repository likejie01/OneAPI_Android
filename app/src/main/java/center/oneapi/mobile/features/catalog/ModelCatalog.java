package center.oneapi.mobile.features.catalog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ModelCatalog {
    private ModelCatalog() {
    }

    public static Result fromPricing(JSONArray data) {
        return fromPricingAndUserModels(data, null);
    }

    public static Result fromPricingAndUserModels(JSONArray pricingData, JSONArray userModels) {
        Result result = new Result();
        Set<String> seen = new LinkedHashSet<>();
        if (pricingData != null) {
            for (int i = 0; i < pricingData.length(); i++) {
                JSONObject item = pricingData.optJSONObject(i);
                String model = first(item, "model_name", "model", "name", "id");
                if (model.isEmpty() || seen.contains(model)) continue;
                seen.add(model);
                addModel(result, item, model);
            }
        }
        if (userModels != null) {
            for (int i = 0; i < userModels.length(); i++) {
                String model = userModels.optString(i, "").trim();
                if (model.isEmpty() || seen.contains(model)) continue;
                seen.add(model);
                addModel(result, null, model);
            }
        }
        return result;
    }

    public static Result fromAvailableModels(JSONArray models) {
        Result result = new Result();
        Set<String> seen = new LinkedHashSet<>();
        if (models == null) return result;
        for (int i = 0; i < models.length(); i++) {
            String model = models.optString(i, "").trim();
            if (model.isEmpty() || seen.contains(model)) continue;
            seen.add(model);
            addModel(result, null, model);
        }
        return result;
    }

    private static void addModel(Result result, JSONObject item, String model) {
        if (model == null || model.trim().isEmpty()) return;
        String clean = model.trim();
        if (supportsChat(item, clean)) result.addChat(clean);
        if (supportsCodex(item, clean)) result.addCodex(clean);
        if (supportsClaude(item, clean)) result.addClaude(clean);
    }

    public static String providerForModel(String model) {
        String value = model == null ? "" : model.toLowerCase(Locale.ROOT);
        if (value.contains("claude")) return "Claude";
        if (value.contains("deepseek")) return "DeepSeek";
        if (value.contains("mimo")) return "Mimo";
        if (value.contains("gemini")) return "Gemini";
        if (value.contains("grok")) return "Grok";
        if (value.contains("qwen") || value.contains("通义")) return "Qwen";
        if (value.contains("gpt") || value.contains("openai") || value.contains("codex") || value.contains("o1") || value.contains("o3") || value.contains("o4")) {
            return "OpenAI";
        }
        return "其他";
    }

    public static List<String> providersFor(List<String> models, boolean claudeFirst) {
        Set<String> found = new LinkedHashSet<>();
        if (models != null) {
            for (String model : models) found.add(providerForModel(model));
        }
        List<String> order = new ArrayList<>(Arrays.asList("OpenAI", "Gemini", "DeepSeek", "Qwen", "Claude", "Mimo", "Grok"));
        if (claudeFirst) {
            order.remove("Claude");
            order.add(0, "Claude");
        }
        List<String> providers = new ArrayList<>();
        providers.add("全部");
        for (String item : order) {
            if (found.contains(item)) providers.add(item);
        }
        for (String item : found) {
            if (!providers.contains(item) && !"其他".equals(item)) providers.add(item);
        }
        if (found.contains("其他")) providers.add("其他");
        return providers;
    }

    public static boolean isAllowedDesktopModel(String model) {
        String n = model == null ? "" : model.toLowerCase(Locale.ROOT);
        if (n.contains("deepseek")) {
            return n.contains("deepseek-v4-pro") || n.contains("deepseek-v4-flash");
        }
        if (n.contains("mimo") || n.contains("xiaomi")) {
            String mimo = normalizeMimoModelFamily(n);
            return "mimo-v2.5-pro".equals(mimo) || "mimo-v2.5".equals(mimo);
        }
        return true;
    }

    private static boolean supportsChat(JSONObject item, String model) {
        if (hasCapability(item, "chat")) return true;
        String n = model.toLowerCase(Locale.ROOT);
        return !n.contains("image") && !n.contains("midjourney") && !n.contains("dall") && !n.contains("stable");
    }

    private static boolean supportsCodex(JSONObject item, String model) {
        if (!isAllowedDesktopModel(model)) return false;
        if (hasCapability(item, "codex")) return true;
        String n = model.toLowerCase(Locale.ROOT);
        return n.startsWith("gpt-") || n.contains("codex") || n.contains("deepseek") || n.contains("mimo") || n.contains("xiaomi");
    }

    private static boolean supportsClaude(JSONObject item, String model) {
        if (!isAllowedDesktopModel(model)) return false;
        if (hasCapability(item, "claude")) return true;
        String n = model.toLowerCase(Locale.ROOT);
        if (n.startsWith("claude") || n.contains("deepseek")) return true;
        if (n.contains("mimo") || n.contains("xiaomi")) {
            return "mimo-v2.5-pro".equals(normalizeMimoModelFamily(n));
        }
        return false;
    }

    private static String normalizeMimoModelFamily(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        normalized = normalized.replaceFirst("^xiaomi-?mimo-?", "mimo-");
        normalized = normalized.replaceFirst("^xiaomimimo-?", "mimo-");
        return normalized;
    }

    private static boolean hasCapability(JSONObject item, String capability) {
        if (item == null || capability == null) return false;
        String clean = capability.trim().toLowerCase(Locale.ROOT);
        for (String key : Arrays.asList("capabilities", "metadata", "features", "mobileScopes", "mobile_scopes", "scopes")) {
            Object raw = item.opt(key);
            if (raw instanceof JSONArray) {
                JSONArray array = (JSONArray) raw;
                for (int i = 0; i < array.length(); i++) {
                    if (clean.equals(String.valueOf(array.opt(i)).trim().toLowerCase(Locale.ROOT))) return true;
                }
            } else if (raw instanceof JSONObject) {
                JSONObject object = (JSONObject) raw;
                if (object.optBoolean(clean, false)) return true;
                if ("true".equalsIgnoreCase(object.optString(clean))) return true;
            } else if (raw instanceof String) {
                for (String part : ((String) raw).split("[,|\\s]+")) {
                    if (clean.equals(part.trim().toLowerCase(Locale.ROOT))) return true;
                }
            }
        }
        return false;
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
        public final List<String> codex = new ArrayList<>();
        public final List<String> claude = new ArrayList<>();

        private void addChat(String value) {
            addUnique(chat, value);
        }

        private void addCodex(String value) {
            addUnique(codex, value);
        }

        private void addClaude(String value) {
            addUnique(claude, value);
        }

        private void addUnique(List<String> list, String value) {
            if (!list.contains(value)) list.add(value);
        }
    }
}
