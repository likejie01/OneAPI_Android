package center.oneapi.mobile;

public final class ChatMessage {
    public final String role;
    public String text;
    public final long timestamp;
    public final boolean log;
    public String tokenText = "";

    public ChatMessage(String role, String text, long timestamp) {
        this.role = role;
        this.text = text;
        this.timestamp = timestamp;
        this.log = false;
    }

    private ChatMessage(String role, String text, long timestamp, boolean log) {
        this.role = role;
        this.text = text;
        this.timestamp = timestamp;
        this.log = log;
    }

    public static ChatMessage log(String text, long timestamp) {
        return new ChatMessage("assistant", text, timestamp, true);
    }
}
