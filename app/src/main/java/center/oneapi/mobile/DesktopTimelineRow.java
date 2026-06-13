package center.oneapi.mobile;

public final class DesktopTimelineRow {
    public final ChatMessage message;
    public final String stableKey;
    public final long timestamp;
    public final int priority;
    public final int index;
    public final String jobId;

    public DesktopTimelineRow(ChatMessage message, String stableKey, long timestamp, int priority, int index) {
        this(message, stableKey, timestamp, priority, index, "");
    }

    public DesktopTimelineRow(ChatMessage message, String stableKey, long timestamp, int priority, int index, String jobId) {
        this.message = message;
        this.stableKey = stableKey == null ? "" : stableKey.trim();
        this.timestamp = timestamp;
        this.priority = priority;
        this.index = index;
        this.jobId = jobId == null ? "" : jobId.trim();
    }

    public boolean duplicates(DesktopTimelineRow other) {
        if (other == null || message == null || other.message == null) return false;
        if (message.log != other.message.log) return false;
        if (!message.role.equals(other.message.role)) return false;
        String text = normalizedText(message.text);
        String otherText = normalizedText(other.message.text);
        if (text.isEmpty() || !text.equals(otherText)) return false;
        return Math.abs(timestamp - other.timestamp) <= duplicateWindowMs(other);
    }

    private long duplicateWindowMs(DesktopTimelineRow other) {
        if (message != null && !message.log && "user".equals(message.role)
                && (stableKey.contains(":prompt") || (other != null && other.stableKey.contains(":prompt")))) {
            return 300000L;
        }
        return 3000L;
    }

    private String normalizedText(String value) {
        String text = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        return text.length() > 240 ? text.substring(0, 240) : text;
    }
}
