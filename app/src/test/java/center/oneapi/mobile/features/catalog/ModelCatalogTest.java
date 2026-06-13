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
                .put(new JSONObject().put("model_name", "claude-sonnet-4-6"))
                .put(new JSONObject().put("model_name", "gpt-image-2"));

        ModelCatalog.Result result = ModelCatalog.fromPricing(data);

        assertTrue(result.chat.contains("gpt-5.4"));
        assertFalse(result.chat.contains("gpt-image-2"));
        assertTrue(result.codex.contains("deepseek-v4-pro"));
        assertFalse(result.codex.contains("deepseek-v3"));
        assertTrue(result.codex.contains("mimo-v2.5"));
        assertFalse(result.codex.contains("mimo-v2.4"));
        assertTrue(result.claude.contains("claude-sonnet-4-6"));
    }

    @Test
    public void providersFor_keepsOtherLastAndClaudeFirstWhenRequested() {
        assertEquals(
                Arrays.asList("全部", "Claude", "OpenAI", "Mimo", "其他"),
                ModelCatalog.providersFor(Arrays.asList("gpt-5.4", "claude-sonnet-4-6", "mimo-v2.5", "unknown"), true)
        );
    }
}
