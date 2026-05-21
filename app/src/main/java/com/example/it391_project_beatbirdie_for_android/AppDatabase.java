package com.example.it391_project_beatbirdie_for_android;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Playlist.class, PlaylistSong.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;

    public abstract PlaylistDao playlistDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    AppDatabase.class, "beatbirdie_db")
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries() // For simplicity in this stage
                    .build();
        }
        return instance;
    }
}
