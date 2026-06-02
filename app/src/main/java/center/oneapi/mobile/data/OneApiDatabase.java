package center.oneapi.mobile.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = {
                ConversationSessionEntity.class,
                ConversationMessageEntity.class
        },
        version = 1,
        exportSchema = false
)
public abstract class OneApiDatabase extends RoomDatabase {
    private static volatile OneApiDatabase instance;

    public abstract ConversationDao conversationDao();

    public static OneApiDatabase get(Context context) {
        if (instance == null) {
            synchronized (OneApiDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    OneApiDatabase.class,
                                    "oneapi-native.db")
                            .build();
                }
            }
        }
        return instance;
    }
}
