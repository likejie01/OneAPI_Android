package center.oneapi.mobile.features.catalog;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AssistantCatalogTest {
    @Test
    public void fromMobileAssistants_splitsChatAndImageAssistants() throws Exception {
        JSONArray data = new JSONArray()
                .put(new JSONObject().put("name", "通用助手").put("scope", "chat").put("prompt", "chat prompt"))
                .put(new JSONObject().put("title", "图片助手").put("scope", "image").put("prompt", "image prompt"));

        AssistantCatalog.Result result = AssistantCatalog.fromMobileAssistants(data);

        assertEquals("通用助手", result.chat.get(0));
        assertEquals("图片助手", result.image.get(0));
        assertEquals("chat prompt", result.chatPrompts.get("通用助手"));
        assertEquals("image prompt", result.imagePrompts.get("图片助手"));
        assertTrue(result.chat.size() == 1);
        assertTrue(result.image.size() == 1);
    }
}
