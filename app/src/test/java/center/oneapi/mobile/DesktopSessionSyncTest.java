package center.oneapi.mobile;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DesktopSessionSyncTest {
    @Test
    public void mergePendingDesktopMessages_dropsStaleLocalSessionsMissingFromServer() {
        long now = 200_000L;
        ChatSession staleDeleted = session("deleted", "old", "D:/deleted", now - 180_000L);
        staleDeleted.status = "complete";
        ChatSession server = session("server", "server", "D:/server", now - 20_000L);

        List<ChatSession> result = DesktopSessionSync.mergePendingDesktopMessages(
                Arrays.asList(staleDeleted),
                new ArrayList<>(Arrays.asList(server)),
                now
        );

        assertEquals(1, result.size());
        assertEquals("server", result.get(0).id);
    }

    @Test
    public void mergePendingDesktopMessages_keepsOnlyRecentPendingLocalSession() {
        long now = 200_000L;
        ChatSession pending = session("pending", "pending", "D:/pending", now - 5_000L);
        pending.status = "queued";

        List<ChatSession> result = DesktopSessionSync.mergePendingDesktopMessages(
                Arrays.asList(pending),
                new ArrayList<>(),
                now
        );

        assertEquals(1, result.size());
        assertEquals("pending", result.get(0).id);
    }

    @Test
    public void recentProjectGroups_returnsFiveNewestProjectsWithFiveNewestSessionsEach() {
        List<ChatSession> sessions = new ArrayList<>();
        for (int project = 1; project <= 6; project++) {
            for (int item = 1; item <= 6; item++) {
                long timestamp = project * 10_000L + item;
                sessions.add(session("p" + project + "-s" + item, "s" + item, "D:/project-" + project, timestamp));
            }
        }

        LinkedHashMap<String, List<ChatSession>> groups = DesktopSessionSync.recentProjectGroups(sessions, 5, 5);

        assertEquals(5, groups.size());
        assertFalse(groups.containsKey("D:/project-1"));
        assertTrue(groups.containsKey("D:/project-6"));
        for (List<ChatSession> groupSessions : groups.values()) {
            assertEquals(5, groupSessions.size());
            assertTrue(groupSessions.get(0).messages.get(0).timestamp > groupSessions.get(4).messages.get(0).timestamp);
        }
    }

    private static ChatSession session(String id, String title, String project, long timestamp) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("user", title, timestamp));
        return new ChatSession(id, title, "Codex", project, messages);
    }
}
