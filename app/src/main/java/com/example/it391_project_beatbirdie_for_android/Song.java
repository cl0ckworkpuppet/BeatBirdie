package com.example.it391_project_beatbirdie_for_android;

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

    public Song(String title, String artist, String album, Uri uri) {
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.uri = uri;
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

    /**
     * @return The Uri pointing to the local file on the device.
     */
    public Uri getUri() {
        return uri;
    }
}
