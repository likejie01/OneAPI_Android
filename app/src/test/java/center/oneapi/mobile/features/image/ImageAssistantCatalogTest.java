package center.oneapi.mobile.features.image;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ImageAssistantCatalogTest {
    @Test
    public void builtInsMirrorDesktopGptImage2SkillCatalog() {
        assertEquals(31, ImageAssistantCatalog.builtIns().size());
        Set<String> titles = new HashSet<>();
        for (ImageAssistantCatalog.Assistant item : ImageAssistantCatalog.builtIns()) {
            assertFalse(item.name.trim().isEmpty());
            assertFalse(item.prompt.trim().isEmpty());
            titles.add(item.name);
        }
        assertEquals(31, titles.size());
        assertTrue(titles.contains("动画与漫画"));
        assertTrue(titles.contains("研究论文图表"));
        assertTrue(titles.contains("屏幕摄影"));
    }
}
