package center.oneapi.mobile.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "conversation_sessions",
        indices = {
                @Index(value = {"mode", "group_name", "updated_at"}),
                @Index(value = {"mode", "updated_at"})
        }
)
public class ConversationSessionEntity {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "session_id")
    public String sessionId = "";

    @NonNull
    @ColumnInfo(name = "mode")
    public String mode = "";

    @NonNull
    @ColumnInfo(name = "group_name")
    public String groupName = "";

    @NonNull
    @ColumnInfo(name = "title")
    public String title = "";

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    @NonNull
    @ColumnInfo(name = "project_name")
    public String projectName = "";

    @NonNull
    @ColumnInfo(name = "project_path")
    public String projectPath = "";

    @NonNull
    @ColumnInfo(name = "preview")
    public String preview = "";
}
