package com.example.it391_project_beatbirdie_for_android;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(tableName = "playlist_songs", primaryKeys = {"playlistId", "songPath"})
public class PlaylistSong {
    public int playlistId;
    @NonNull
    public String songPath;
    public long dateAdded;

    public PlaylistSong(int playlistId, @NonNull String songPath) {
        this.playlistId = playlistId;
        this.songPath = songPath;
        this.dateAdded = System.currentTimeMillis();
    }
}
