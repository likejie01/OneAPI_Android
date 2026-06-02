package center.oneapi.mobile.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public abstract class ConversationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void upsertSession(ConversationSessionEntity session);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void upsertMessages(List<ConversationMessageEntity> messages);

    @Query("SELECT * FROM conversation_sessions WHERE mode = :mode ORDER BY updated_at DESC")
    public abstract List<ConversationSessionEntity> sessionsForMode(String mode);

    @Query("SELECT * FROM conversation_sessions WHERE mode = :mode AND group_name = :groupName ORDER BY updated_at DESC")
    public abstract List<ConversationSessionEntity> sessionsForGroup(String mode, String groupName);

    @Query("SELECT * FROM conversation_sessions WHERE session_id = :sessionId LIMIT 1")
    public abstract ConversationSessionEntity sessionById(String sessionId);

    @Query("SELECT COUNT(*) FROM conversation_messages WHERE session_id = :sessionId")
    public abstract int messageCount(String sessionId);

    @Query("SELECT * FROM conversation_messages WHERE session_id = :sessionId AND sort_index < :beforeSort ORDER BY sort_index DESC LIMIT :limit")
    public abstract List<ConversationMessageEntity> earlierMessagesDesc(String sessionId, int beforeSort, int limit);

    @Query("SELECT * FROM (SELECT * FROM conversation_messages WHERE session_id = :sessionId ORDER BY sort_index DESC LIMIT :limit) ORDER BY sort_index ASC")
    public abstract List<ConversationMessageEntity> latestMessages(String sessionId, int limit);

    @Query("DELETE FROM conversation_messages WHERE session_id = :sessionId")
    public abstract void deleteMessagesForSession(String sessionId);

    @Query("DELETE FROM conversation_sessions WHERE session_id = :sessionId")
    public abstract void deleteSession(String sessionId);

    @Transaction
    public void replaceSessionMessages(ConversationSessionEntity session, List<ConversationMessageEntity> messages) {
        upsertSession(session);
        deleteMessagesForSession(session.sessionId);
        if (messages != null && !messages.isEmpty()) {
            upsertMessages(messages);
        }
    }
}
