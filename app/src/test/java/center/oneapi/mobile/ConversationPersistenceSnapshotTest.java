package center.oneapi.mobile;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ConversationPersistenceSnapshotTest {
    @Test
    public void replacesUnfinishedChatPlaceholderWhenRestoring() {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("user", "你好", 1L));
        messages.add(new ChatMessage("assistant", "正在思考...", 2L));

        ConversationPersistenceSnapshot.repairInterruptedMessages(messages);

        assertEquals("上次请求已中断，请重新发送。", messages.get(1).text);
    }

    @Test
    public void removesUnfinishedImagePlaceholderWhenRestoring() {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("user", "画一张图", 1L));
        messages.add(new ChatMessage("assistant", "oneapi://image-loading", 2L));

        ConversationPersistenceSnapshot.repairInterruptedMessages(messages);

        assertEquals(1, messages.size());
        assertEquals("user", messages.get(0).role);
    }
}
