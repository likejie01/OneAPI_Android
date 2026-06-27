package center.oneapi.mobile;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MainActivityNetworkPolicySourceTest {
    @Test
    public void criticalPanelsAndPaymentsUseCriticalExecutor() throws Exception {
        String source = source();

        assertTrue(source.contains("executeNetwork(NetworkRequestPolicy.Priority.CRITICAL, () -> {"));
        assertTrue(source.contains("panelDataStore.put(title, result"));
        assertTrue(source.contains("panelDataStore.remove(\"钱包与消耗\")"));
        assertTrue(source.contains("CompletableFuture<JSONObject> profile = supplyCriticalJson"));
        assertFalse(source.contains("panelDataCache"));
    }

    @Test
    public void desktopSyncAndUserActionsHaveSeparateChannels() throws Exception {
        String source = source();

        assertTrue(source.contains("private final ExecutorService criticalNetwork"));
        assertTrue(source.contains("private final ExecutorService userActionNetwork"));
        assertTrue(source.contains("private final ExecutorService desktopNetwork"));
        assertTrue(source.contains("desktopApiClient = new ApiClient(prefs)"));
        assertTrue(source.contains("activeSendTask = userActionNetwork.submit"));
        assertTrue(source.contains("userActionApiClient.cancelActiveRequests()"));
    }

    @Test
    public void landscapeConversationHidesNavigationAndComposer() throws Exception {
        String source = source();

        assertTrue(source.contains("landscapeConversationChromeHidden = conversation && landscape"));
        assertTrue(source.contains("aiChatHeader.setVisibility(conversation && landscape ? View.GONE : View.VISIBLE)"));
        assertTrue(source.contains("composerView.setVisibility(conversation && !landscape ? View.VISIBLE : View.GONE)"));
        assertTrue(source.contains("android:configChanges=\"orientation|screenSize|keyboardHidden\"")
                || manifest().contains("android:configChanges=\"orientation|screenSize|keyboardHidden\""));
    }

    private static String source() throws Exception {
        return new String(Files.readAllBytes(Paths.get("src/main/java/center/oneapi/mobile/MainActivity.java")), StandardCharsets.UTF_8);
    }

    private static String manifest() throws Exception {
        return new String(Files.readAllBytes(Paths.get("src/main/AndroidManifest.xml")), StandardCharsets.UTF_8);
    }
}
