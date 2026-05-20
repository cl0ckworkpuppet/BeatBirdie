package com.example.it391_project_beatbirdie_for_android;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "playlists")
public class Playlist {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private String customThumbnailPath;
    private int songCount;

    @Ignore
    private List<Song> songs;

    public Playlist(String name) {
        this.name = name;
        this.songs = new ArrayList<>();
    }

    @Ignore
    public Playlist(int id, String name, String customThumbnailPath) {
        this.id = id;
        this.name = name;
        this.customThumbnailPath = customThumbnailPath;
        this.songs = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCustomThumbnailPath() {
        return customThumbnailPath;
    }

    public void setCustomThumbnailPath(String customThumbnailPath) {
        this.customThumbnailPath = customThumbnailPath;
    }

    public int getSongCount() {
        return songCount;
    }

    public void setSongCount(int songCount) {
        this.songCount = songCount;
    }

    public List<Song> getSongs() {
        return songs;
    }

    public void setSongs(List<Song> songs) {
        this.songs = songs;
    }

    public int getSize() {
        return songs != null ? songs.size() : 0;
    }
}
