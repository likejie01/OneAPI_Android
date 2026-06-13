package center.oneapi.mobile;

import java.util.List;

final class ChatSession {
    final String id;
    String title;
    String assistantLabel;
    final String projectLabel;
    final List<ChatMessage> messages;
    String status = "";

    ChatSession(String id, String title, String assistantLabel, String projectLabel, List<ChatMessage> messages) {
        this.id = id;
        this.title = title;
        this.assistantLabel = assistantLabel;
        this.projectLabel = projectLabel;
        this.messages = messages;
    }
}
