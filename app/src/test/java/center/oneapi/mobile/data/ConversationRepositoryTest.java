package center.oneapi.mobile.data;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ConversationRepositoryTest {
    @Test
    public void localChatSelection_usesCurrentSessionAssistantKey() {
        ConversationRepository.LocalSessionSelection selection =
                ConversationRepository.resolveLocalSessionSelection("chat", "文档助手", "session-1");
        assertEquals("chat", selection.mode);
        assertEquals("文档助手", selection.group);
        assertEquals("session-1", selection.sessionId);
        assertEquals("selected_chat_assistant", selection.preferenceKey);
    }

    @Test
    public void localImageSelection_usesImageAssistantKey() {
        ConversationRepository.LocalSessionSelection selection =
                ConversationRepository.resolveLocalSessionSelection("image", "image", "session-2");
        assertEquals("selected_image_assistant", selection.preferenceKey);
    }
}
