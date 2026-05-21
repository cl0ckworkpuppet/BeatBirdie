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
    @Ignore
    private Object thumbnailUri;

    public Playlist(String name) {
        this.name = name;
        this.songs = new ArrayList<>();
    }

    public Object getThumbnailUri() {
        return thumbnailUri;
    }

    public void setThumbnailUri(Object thumbnailUri) {
        this.thumbnailUri = thumbnailUri;
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

    /**
     * Finds the first song in the list of paths that has a valid album art.
     * Returns either an AudioCoverModel (for embedded art) or a Uri (for MediaStore art).
     */
    public static Object findEarliestSongWithCover(android.content.Context context, List<String> paths) {
        if (paths == null) return null;
        for (String path : paths) {
            android.net.Uri songUri = getSongUriByPath(context, path);
            if (songUri != null) {
                // Priority 1: Embedded Art (via AudioCoverModel)
                if (hasEmbeddedArt(context, songUri)) {
                    return new AudioCoverModel(songUri);
                }
                
                // Priority 2: MediaStore Album Art
                android.net.Uri albumArtUri = getAlbumArtUri(context, path);
                if (albumArtUri != null) {
                    return albumArtUri;
                }
            }
        }
        return null;
    }

    private static boolean hasEmbeddedArt(android.content.Context context, android.net.Uri songUri) {
        try (android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever()) {
            retriever.setDataSource(context, songUri);
            byte[] art = retriever.getEmbeddedPicture();
            return art != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static android.net.Uri getSongUriByPath(android.content.Context context, String path) {
        String[] projection = {android.provider.MediaStore.Audio.Media._ID};
        String selection = android.provider.MediaStore.Audio.Media.DATA + "=?";
        String[] selectionArgs = {path};

        try (android.database.Cursor cursor = context.getContentResolver().query(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                long id = cursor.getLong(0);
                return android.content.ContentUris.withAppendedId(
                        android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
            }
        }
        return null;
    }

    private static android.net.Uri getAlbumArtUri(android.content.Context context, String path) {
        String[] projection = {android.provider.MediaStore.Audio.Media.ALBUM_ID};
        String selection = android.provider.MediaStore.Audio.Media.DATA + "=?";
        String[] selectionArgs = {path};

        try (android.database.Cursor cursor = context.getContentResolver().query(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                long albumId = cursor.getLong(0);
                if (albumId > 0) {
                    android.net.Uri uri = android.content.ContentUris.withAppendedId(
                            android.net.Uri.parse("content://media/external/audio/albumart"),
                            albumId
                    );
                    // Verify if art actually exists for this albumId
                    try (java.io.InputStream is = context.getContentResolver().openInputStream(uri)) {
                        if (is != null) return uri;
                    } catch (Exception ignored) {}
                }
            }
        }
        return null;
    }
}
