package center.oneapi.mobile.data;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ConversationRepository {
    public static final int DEFAULT_PAGE_SIZE = 40;

    private final ConversationDao dao;

    public ConversationRepository(Context context) {
        this(OneApiRoomDatabase.get(context).conversationDao());
    }

    public ConversationRepository(ConversationDao dao) {
        this.dao = dao;
    }

    public List<ConversationSessionEntity> sessionsForMode(String mode) {
        return dao.sessionsForMode(mode);
    }

    public ConversationSessionEntity sessionById(String sessionId) {
        return dao.sessionById(sessionId);
    }

    public List<ConversationMessageEntity> latestMessages(String sessionId, int pageSize) {
        int count = dao.messageCount(sessionId);
        int start = latestPageStart(count, pageSize);
        return dao.messagePageFromEnd(sessionId, pageSize, Math.max(0, count - start - pageSize));
    }

    public static LocalSessionSelection resolveLocalSessionSelection(String mode, String group, String sessionId) {
        String cleanMode = clean(mode);
        String cleanGroup = clean(group);
        String cleanSessionId = clean(sessionId);
        String key = "image".equals(cleanMode) ? "selected_image_assistant" : "selected_chat_assistant";
        return new LocalSessionSelection(cleanMode, cleanGroup, cleanSessionId, key);
    }

    public static int latestPageStart(int totalCount, int pageSize) {
        if (totalCount <= 0 || pageSize <= 0 || totalCount <= pageSize) {
            return 0;
        }
        return Math.max(0, totalCount - pageSize);
    }

    public static List<String> uniqueSessionLabels(List<SessionSummary> sessions) {
        List<String> out = new ArrayList<>();
        Set<String> used = new HashSet<>();
        if (sessions == null) {
            return out;
        }
        for (SessionSummary session : sessions) {
            String base = clean(session == null ? "" : session.title);
            if (base.isEmpty()) {
                base = "未命名会话";
            }
            String label = base;
            if (used.contains(label)) {
                String time = session == null || session.updatedAt <= 0 ? "" : formatDateTime(session.updatedAt);
                label = time.isEmpty() ? base : base + " · " + time;
            }
            if (used.contains(label)) {
                String id = shortId(session == null ? "" : session.sessionId);
                if (!id.isEmpty()) {
                    label = label + " · " + id;
                }
            }
            int suffix = 2;
            String candidate = label;
            while (used.contains(candidate)) {
                candidate = label + " (" + suffix + ")";
                suffix++;
            }
            used.add(candidate);
            out.add(candidate);
        }
        return out;
    }

    public static String formatDateTime(long timestamp) {
        long ms = timestamp > 10000000000L ? timestamp : timestamp * 1000L;
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(ms);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String shortId(String id) {
        String clean = clean(id);
        return clean.length() <= 8 ? clean : clean.substring(0, 8);
    }

    public static class LocalSessionSelection {
        public final String mode;
        public final String group;
        public final String sessionId;
        public final String preferenceKey;

        public LocalSessionSelection(String mode, String group, String sessionId, String preferenceKey) {
            this.mode = mode;
            this.group = group;
            this.sessionId = sessionId;
            this.preferenceKey = preferenceKey;
        }
    }

    public static class SessionSummary {
        public final String sessionId;
        public final String mode;
        public final String group;
        public final String title;
        public final long updatedAt;

        public SessionSummary(String sessionId, String mode, String group, String title, long updatedAt) {
            this.sessionId = sessionId;
            this.mode = mode;
            this.group = group;
            this.title = title;
            this.updatedAt = updatedAt;
        }
    }
}
