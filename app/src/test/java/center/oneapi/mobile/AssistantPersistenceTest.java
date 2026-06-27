package center.oneapi.mobile;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AssistantPersistenceTest {
    @Test
    public void serializesCustomAssistantsAcrossRestart() throws Exception {
        AssistantPersistence.Store store = new AssistantPersistence.Store();
        store.upsert("我的助手", 7, "固定提示词");

        String encoded = AssistantPersistence.encode(store);
        AssistantPersistence.Store restored = AssistantPersistence.decode(encoded);

        assertEquals(1, restored.items().size());
        assertEquals("我的助手", restored.items().get(0).name);
        assertEquals(7, restored.items().get(0).order);
        assertEquals("固定提示词", restored.items().get(0).prompt);
    }

    @Test
    public void supportsRenameAndDeleteForCustomAssistants() {
        AssistantPersistence.Store store = new AssistantPersistence.Store();
        store.upsert("旧助手", 20, "旧提示");
        store.rename("旧助手", "新助手", 9, "新提示");

        assertEquals("", store.prompt("旧助手"));
        assertEquals("新提示", store.prompt("新助手"));

        store.remove("新助手");
        assertTrue(store.items().isEmpty());
    }

    @Test
    public void mergesCustomAssistantsAfterServerCatalog() {
        AssistantPersistence.Store store = new AssistantPersistence.Store();
        store.upsert("本地助手", 2, "本地提示");

        List<String> merged = store.mergeNames(java.util.Arrays.asList("服务端助手"));

        assertEquals("本地助手", merged.get(0));
        assertEquals("服务端助手", merged.get(1));
    }
}
