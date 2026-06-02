package center.oneapi.mobile.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "conversation_messages",
        foreignKeys = @ForeignKey(
                entity = ConversationSessionEntity.class,
                parentColumns = "session_id",
                childColumns = "session_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index(value = {"session_id", "sort_index"}),
                @Index(value = {"session_id", "timestamp"})
        }
)
public class ConversationMessageEntity {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "message_id")
    public String messageId = "";

    @NonNull
    @ColumnInfo(name = "session_id")
    public String sessionId = "";

    @NonNull
    @ColumnInfo(name = "mode")
    public String mode = "";

    @NonNull
    @ColumnInfo(name = "kind")
    public String kind = "message";

    @NonNull
    @ColumnInfo(name = "role")
    public String role = "";

    @NonNull
    @ColumnInfo(name = "content_type")
    public String contentType = "text";

    @NonNull
    @ColumnInfo(name = "text")
    public String text = "";

    @ColumnInfo(name = "timestamp")
    public long timestamp;

    @ColumnInfo(name = "sort_index")
    public int sortIndex;

    @NonNull
    @ColumnInfo(name = "raw_json")
    public String rawJson = "";
}
