package center.oneapi.mobile;

import java.util.Iterator;
import java.util.List;

final class ConversationPersistenceSnapshot {
    static final String CHAT_PLACEHOLDER = "正在思考...";
    static final String DESKTOP_PLACEHOLDER = "正在发送到桌面端...";
    static final String IMAGE_PLACEHOLDER = "oneapi://image-loading";
    static final String INTERRUPTED_TEXT = "上次请求已中断，请重新发送。";

    private ConversationPersistenceSnapshot() {
    }

    static void repairInterruptedMessages(List<ChatMessage> messages) {
        if (messages == null) return;
        Iterator<ChatMessage> iterator = messages.iterator();
        while (iterator.hasNext()) {
            ChatMessage message = iterator.next();
            String text = message == null || message.text == null ? "" : message.text.trim();
            if (IMAGE_PLACEHOLDER.equals(text)) {
                iterator.remove();
            } else if (CHAT_PLACEHOLDER.equals(text) || DESKTOP_PLACEHOLDER.equals(text)) {
                message.text = INTERRUPTED_TEXT;
            }
        }
    }
}
