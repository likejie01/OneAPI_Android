package center.oneapi.mobile.data;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ConversationRepositoryTest {
    @Test
    public void selectingLocalSessionAppliesSessionGroup() {
        ConversationRepository.LocalSessionSelection selection =
                ConversationRepository.resolveLocalSessionSelection("chat", "assistant-a", "session-1");

        assertEquals("chat", selection.mode);
        assertEquals("assistant-a", selection.group);
        assertEquals("session-1", selection.sessionId);
        assertEquals("selected_chat_assistant", selection.preferenceKey);
    }

    @Test
    public void duplicateSessionTitlesReceiveStableLabels() {
        List<ConversationRepository.SessionSummary> sessions = Arrays.asList(
                new ConversationRepository.SessionSummary("s1", "chat", "默认助手", "新会话", 1000L),
                new ConversationRepository.SessionSummary("s2", "chat", "默认助手", "新会话", 2000L),
                new ConversationRepository.SessionSummary("s3", "chat", "默认助手", "新会话", 2000L)
        );

        List<String> labels = ConversationRepository.uniqueSessionLabels(sessions);

        assertEquals("新会话", labels.get(0));
        assertEquals("新会话 · 1970-01-01 08:33", labels.get(1));
        assertEquals("新会话 · 1970-01-01 08:33 · s3", labels.get(2));
    }

    @Test
    public void latestPageStartsAtLastPageBoundary() {
        assertEquals(0, ConversationRepository.latestPageStart(0, 40));
        assertEquals(0, ConversationRepository.latestPageStart(12, 40));
        assertEquals(40, ConversationRepository.latestPageStart(80, 40));
        assertEquals(41, ConversationRepository.latestPageStart(81, 40));
    }
}
