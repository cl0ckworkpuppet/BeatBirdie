package com.example.it391_project_beatbirdie_for_android;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface PlaylistDao {
    @Query("SELECT * FROM playlists")
    List<Playlist> getAllPlaylists();

    @Query("UPDATE playlists SET songCount = (SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId) WHERE id = :playlistId")
    void updateSongCount(int playlistId);

    @Insert
    long insert(Playlist playlist);

    @Update
    void update(Playlist playlist);

    @Delete
    void delete(Playlist playlist);

    @Query("SELECT * FROM playlists WHERE id = :id")
    Playlist getPlaylistById(int id);

    @Insert
    void addSongToPlaylist(PlaylistSong playlistSong);

    @Delete
    void removeSongFromPlaylist(PlaylistSong playlistSong);

    @Query("SELECT songPath FROM playlist_songs WHERE playlistId = :playlistId ORDER BY dateAdded ASC")
    List<String> getSongPathsForPlaylist(int playlistId);

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId")
    int getSongCountForPlaylist(int playlistId);

    @Query("SELECT EXISTS(SELECT 1 FROM playlist_songs WHERE playlistId = :playlistId AND songPath = :songPath)")
    boolean isSongInPlaylist(int playlistId, String songPath);

    @Query("SELECT playlistId FROM playlist_songs WHERE songPath = :songPath")
    List<Integer> getPlaylistIdsForSong(String songPath);

    @Query("SELECT songPath FROM playlist_songs WHERE playlistId = :playlistId ORDER BY dateAdded ASC LIMIT 1")
    String getFirstSongPath(int playlistId);

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    void deleteSongsByPlaylistId(int playlistId);
}
