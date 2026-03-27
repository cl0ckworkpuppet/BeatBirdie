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
    private long albumId;

    public Song(String title, String artist, String album, Uri uri, long albumId) {
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.uri = uri;
        this.albumId = albumId;
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

    public long getAlbumId() {
        return albumId;
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

