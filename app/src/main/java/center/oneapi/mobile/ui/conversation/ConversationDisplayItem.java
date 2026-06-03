package center.oneapi.mobile.ui.conversation;

public class ConversationDisplayItem {
    public static final int MESSAGE = 1;
    public static final int LOG = 2;
    public static final int LOAD_EARLIER = 3;
    public static final int EMPTY = 4;

    public final long id;
    public final int type;
    public final String role;
    public final String text;
    public final long timestamp;
    public final String tokenText;

    private ConversationDisplayItem(long id, int type, String role, String text, long timestamp, String tokenText) {
        this.id = id;
        this.type = type;
        this.role = role == null ? "" : role;
        this.text = text == null ? "" : text;
        this.timestamp = timestamp;
        this.tokenText = tokenText == null ? "" : tokenText;
    }

    public static ConversationDisplayItem message(String role, String text, long timestamp, String tokenText, int index) {
        return new ConversationDisplayItem(stableId("message", role, "", timestamp, index), MESSAGE, role, text, timestamp, tokenText);
    }

    public static ConversationDisplayItem log(String text, long timestamp, int index) {
        return new ConversationDisplayItem(stableId("log", "", "", timestamp, index), LOG, "", text, timestamp, "");
    }

    public static ConversationDisplayItem loadEarlier(String text) {
        return new ConversationDisplayItem(stableId("load", "", text, 0, 0), LOAD_EARLIER, "", text, 0, "");
    }

    public static ConversationDisplayItem empty(String text) {
        return new ConversationDisplayItem(stableId("empty", "", text, 0, 0), EMPTY, "", text, 0, "");
    }

    private static long stableId(String kind, String role, String text, long timestamp, int index) {
        long id = 1125899906842597L;
        String value = kind + "|" + role + "|" + timestamp + "|" + index + "|" + text;
        for (int i = 0; i < value.length(); i++) {
            id = 31L * id + value.charAt(i);
        }
        return id == -1L ? -2L : id;
    }
}
