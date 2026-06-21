package center.oneapi.mobile.features.catalog;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ModelCatalogTest {
    @Test
    public void fromPricing_usesCompatibilityMatrix() throws Exception {
        JSONArray data = new JSONArray()
                .put(new JSONObject().put("model_name", "gpt-5.4"))
                .put(new JSONObject().put("model_name", "deepseek-v4-pro"))
                .put(new JSONObject().put("model_name", "deepseek-v3"))
                .put(new JSONObject().put("model_name", "mimo-v2.5"))
                .put(new JSONObject().put("model_name", "mimo-v2.4"))
                .put(new JSONObject().put("model_name", "xiaomimimo-v2.5-pro"))
                .put(new JSONObject().put("model_name", "xiaomi-mimo-v2.5"))
                .put(new JSONObject().put("model_name", "xiaomimimo-v2-pro"))
                .put(new JSONObject().put("model_name", "claude-sonnet-4-6"))
                .put(new JSONObject().put("model_name", "gpt-image-2"));

        ModelCatalog.Result result = ModelCatalog.fromPricing(data);

        assertTrue(result.chat.contains("gpt-5.4"));
        assertFalse(result.chat.contains("gpt-image-2"));
        assertTrue(result.codex.contains("deepseek-v4-pro"));
        assertFalse(result.codex.contains("deepseek-v3"));
        assertTrue(result.codex.contains("mimo-v2.5"));
        assertTrue(result.codex.contains("xiaomimimo-v2.5-pro"));
        assertTrue(result.codex.contains("xiaomi-mimo-v2.5"));
        assertFalse(result.codex.contains("xiaomimimo-v2-pro"));
        assertFalse(result.codex.contains("mimo-v2.4"));
        assertTrue(result.claude.contains("claude-sonnet-4-6"));
        assertTrue(result.claude.contains("xiaomimimo-v2.5-pro"));
        assertFalse(result.claude.contains("xiaomi-mimo-v2.5"));
    }

    @Test
    public void providersFor_keepsOtherLastAndClaudeFirstWhenRequested() {
        assertEquals(
                Arrays.asList("全部", "Claude", "OpenAI", "Mimo", "其他"),
                ModelCatalog.providersFor(Arrays.asList("gpt-5.4", "claude-sonnet-4-6", "mimo-v2.5", "unknown"), true)
        );
    }

    @Test
    public void fromPricingAndUserModels_appendsXiaomiMimoUserModelsMissingFromPricing() throws Exception {
        JSONArray pricing = new JSONArray()
                .put(new JSONObject().put("model_name", "gpt-5.4"));
        JSONArray userModels = new JSONArray()
                .put("xiaomimimo-v2.5-pro")
                .put("xiaomi-mimo-v2.5")
                .put("xiaomimimo-v2-pro");

        ModelCatalog.Result result = ModelCatalog.fromPricingAndUserModels(pricing, userModels);

        assertTrue(result.codex.contains("xiaomimimo-v2.5-pro"));
        assertTrue(result.codex.contains("xiaomi-mimo-v2.5"));
        assertFalse(result.codex.contains("xiaomimimo-v2-pro"));
        assertTrue(result.claude.contains("xiaomimimo-v2.5-pro"));
        assertFalse(result.claude.contains("xiaomi-mimo-v2.5"));
    }

    @Test
    public void fromAvailableModels_excludesPricingOnlyModels() throws Exception {
        JSONArray availableModels = new JSONArray()
                .put("deepseek-v4-flash")
                .put("mimo-v2.5");

        ModelCatalog.Result result = ModelCatalog.fromAvailableModels(availableModels);

        assertFalse(result.chat.contains("gpt-5.4"));
        assertTrue(result.chat.contains("deepseek-v4-flash"));
        assertTrue(result.chat.contains("mimo-v2.5"));
    }
}
