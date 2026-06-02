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
public abstract class OneApiRoomDatabase extends RoomDatabase {
    private static volatile OneApiRoomDatabase instance;

    public abstract ConversationDao conversationDao();

    public static OneApiRoomDatabase get(Context context) {
        if (instance == null) {
            synchronized (OneApiRoomDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    OneApiRoomDatabase.class,
                                    "oneapi-mobile.db")
                            .build();
                }
            }
        }
        return instance;
    }
}
