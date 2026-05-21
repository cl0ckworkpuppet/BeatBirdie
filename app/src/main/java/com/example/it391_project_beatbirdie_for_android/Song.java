package com.example.it391_project_beatbirdie_for_android;

import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.preference.PreferenceManager;

/**
 * Data model representing a single music track.
 * Optimized for high-performance RecyclerView binding.
 */
public class Song {
    private final String title;
    private final String artist;
    private final String album;
    private final Uri uri;
    private final String path;
    private final long albumId;
    private final int duration;
    
    // Pre-calculated fields to ensure zero-logic binding in adapters
    private final String subtitle;
    private final Uri albumArtUri;
    private final AudioCoverModel coverModel;

    public Song(String title, String artist, String album, Uri uri, String path, long albumId, int duration) {
        this.title = title != null ? title : "Unknown Title";
        this.artist = artist != null ? artist : "Unknown Artist";
        this.album = album != null ? album : "Unknown Album";
        this.uri = uri;
        this.path = path;
        this.albumId = albumId;
        this.duration = duration;
        
        // Pre-calculate to avoid string concatenation and object creation during scroll
        this.subtitle = this.artist + " - " + this.album;
        this.albumArtUri = albumId <= 0 ? null : ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId);
        this.coverModel = new AudioCoverModel(uri);
    }

    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getAlbum() { return album; }
    public Uri getUri() { return uri; }
    public String getPath() { return path; }
    public long getAlbumId() { return albumId; }
    public int getDuration() { return duration; }
    public String getSubtitle() { return subtitle; }
    public Uri getAlbumArtUri() { return albumArtUri; }
    public AudioCoverModel getCoverModel() { return coverModel; }

    /**
     * Resolves a file path to a Song object using MediaStore.
     */
    public static Song getByPath(Context context, String path) {
        if (path == null || BlacklistManager.isBlacklisted(context, path)) return null;
        if (path.toLowerCase().endsWith(".mp2") || path.toLowerCase().endsWith(".wma")) return null;

        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DISPLAY_NAME
        };
        String selection = MediaStore.Audio.Media.DATA + "=?";
        String[] selectionArgs = {path};

        try (Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                long id = cursor.getLong(0);
                String title = cursor.getString(1);
                String artist = cursor.getString(2);
                String album = cursor.getString(3);
                long albumId = cursor.getLong(4);
                int duration = cursor.getInt(5);
                String fileName = cursor.getString(6);

                if (duration <= 0) {
                    try (android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever()) {
                        retriever.setDataSource(path);
                        String durStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
                        if (durStr != null) duration = Integer.parseInt(durStr);
                    } catch (Exception ignored) {}
                }

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                int minDuration = Integer.parseInt(prefs.getString("filter_duration", "0"));
                if (duration < minDuration) return null;

                if (title == null || title.isEmpty()) title = fileName;
                if (artist == null || artist.isEmpty() || artist.equals("<unknown>")) artist = "Unknown Artist";
                if (album == null || album.isEmpty() || album.equals("<unknown>")) album = "Unknown Album";

                Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                return new Song(title, artist, album, contentUri, path, albumId, duration);
            }
        }
        return null;
    }
}
