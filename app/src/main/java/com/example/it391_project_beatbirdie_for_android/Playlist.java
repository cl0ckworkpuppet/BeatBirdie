package com.example.it391_project_beatbirdie_for_android;

import android.net.Uri;
import java.util.ArrayList;
import java.util.List;

public class Playlist {
    private String name;
    private List<Song> songs;
    private Uri thumbnailUri;

    public Playlist(String name) {
        this.name = name;
        this.songs = new ArrayList<>();
    }

    public Playlist(String name, List<Song> songs, Uri thumbnailUri) {
        this.name = name;
        this.songs = songs;
        this.thumbnailUri = thumbnailUri;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Song> getSongs() {
        return songs;
    }

    public void setSongs(List<Song> songs) {
        this.songs = songs;
    }

    public Uri getThumbnailUri() {
        return thumbnailUri;
    }

    public void setThumbnailUri(Uri thumbnailUri) {
        this.thumbnailUri = thumbnailUri;
    }

    public int getSize() {
        return songs != null ? songs.size() : 0;
    }
}
