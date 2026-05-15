package com.example.it391_project_beatbirdie_for_android;

import android.content.ContentUris;
import android.net.Uri;

/**
 * Data model representing a single music track.
 * Stores metadata retrieved from the device's MediaStore.
 */
public class Song {
    private String title;
    private String artist;
    private String album;
    private Uri uri; // The unique content URI used to play the file via MediaPlayer
    private String path; // The absolute file path on disk
    private long albumId;
    private int duration;

    public Song(String title, String artist, String album, Uri uri, String path, long albumId, int duration) {
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.uri = uri;
        this.path = path;
        this.albumId = albumId;
        this.duration = duration;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public Uri getUri() { return uri; }

    public String getPath() { return path; }

    public long getAlbumId() {
        return albumId;
    }

    public int getDuration() {
        return duration;
    }

    // gets the URI for the album art. needs a fancy metadata process
    public Uri getAlbumArtUri() {
        if (albumId <= 0) return null;
        return ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"),
                albumId
        );
    }
}

