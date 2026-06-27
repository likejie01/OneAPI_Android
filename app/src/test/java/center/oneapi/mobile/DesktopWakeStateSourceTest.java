package center.oneapi.mobile;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class DesktopWakeStateSourceTest {
    @Test
    public void desktopJobStateChangesRefreshKeepScreenOnImmediately() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get("src/main/java/center/oneapi/mobile/MainActivity.java")), StandardCharsets.UTF_8);

        assertTrue(source.contains("session.status = \"queued\";\n                updateDesktopWakeState();"));
        assertTrue(source.contains("session.status = statusFromEvents;\n            DesktopSessionSync.normalizeDesktopStatusFromMessages(session);"));
        assertTrue(source.contains("updateDesktopWakeState();"));
        assertTrue(source.contains("|| \"stopped\".equals(type)"));
        assertTrue(source.contains("if (waitingInteraction) return \"waiting_interaction\";"));
        assertTrue(source.contains("getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);"));
        assertTrue(source.contains("getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);"));
    }
}
