package center.oneapi.mobile.data;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConversationRepository {
    public static final int DEFAULT_PAGE_SIZE = 40;
    private final ConversationDao dao;

    public ConversationRepository(Context context) {
        this(OneApiDatabase.get(context).conversationDao());
    }

    public ConversationRepository(ConversationDao dao) {
        this.dao = dao;
    }

    public List<ConversationSessionEntity> sessionsForMode(String mode) {
        return dao.sessionsForMode(clean(mode));
    }

    public List<ConversationSessionEntity> sessionsForGroup(String mode, String groupName) {
        return dao.sessionsForGroup(clean(mode), clean(groupName));
    }

    public ConversationSessionEntity sessionById(String sessionId) {
        return dao.sessionById(clean(sessionId));
    }

    public List<ConversationMessageEntity> latestMessages(String sessionId, int limit) {
        return dao.latestMessages(clean(sessionId), positiveLimit(limit));
    }

    public List<ConversationMessageEntity> earlierMessages(String sessionId, int beforeSort, int limit) {
        List<ConversationMessageEntity> desc = dao.earlierMessagesDesc(clean(sessionId), beforeSort, positiveLimit(limit));
        List<ConversationMessageEntity> asc = new ArrayList<>(desc);
        Collections.reverse(asc);
        return asc;
    }

    public void replaceSessionMessages(ConversationSessionEntity session, List<ConversationMessageEntity> messages) {
        dao.replaceSessionMessages(session, messages);
    }

    public static LocalSessionSelection resolveLocalSessionSelection(String mode, String group, String sessionId) {
        String cleanMode = clean(mode);
        String key = "image".equals(cleanMode) ? "selected_image_assistant" : "selected_chat_assistant";
        return new LocalSessionSelection(cleanMode, clean(group), clean(sessionId), key);
    }

    private static int positiveLimit(int limit) {
        return limit <= 0 ? DEFAULT_PAGE_SIZE : Math.min(limit, 200);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
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
}
