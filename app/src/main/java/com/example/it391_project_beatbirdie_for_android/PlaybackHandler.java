package com.example.it391_project_beatbirdie_for_android;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import java.io.IOException;
import java.util.List;

/**
 * Singleton-like class that manages the MediaPlayer and playback state.
 * This class allows consistent music control across different Activities.
 */
public class PlaybackHandler {

    private static MediaPlayer mediaPlayer;
    private static List<Song> currentPlaylist;
    private static int currentIndex = -1;
    private static String alg = "Default";
    private static Context appContext;

    /**
     * Initializes the handler with the application context.
     * @param context Activity or Application context.
     */
    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    /**
     * Loads and starts playing a specific song from a playlist.
     * @param context Context used for data source.
     * @param playlist List of songs available to play.
     * @param index Position of the song in the playlist.
     */
    public static void playSong(Context context, List<Song> playlist, int index) {
        if (playlist == null || index < 0 || index >= playlist.size()) return;

        currentPlaylist = playlist;
        currentIndex = index;
        Song song = playlist.get(index);

        // Stop and release previous player to free up system resources
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }

        mediaPlayer = new MediaPlayer();
        
        // Set attributes for music playback
        mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
        );

        try {
            mediaPlayer.setDataSource(context, song.getUri());
            mediaPlayer.prepare(); // Synchronous prepare for local files
            mediaPlayer.start();
            
            // Automatically play next song when current one finishes
            mediaPlayer.setOnCompletionListener(mp -> next(context));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Toggles between play and pause.
     */
    public static void toggle() {
        if (mediaPlayer == null) return;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        } else {
            mediaPlayer.start();
        }
    }

    /**
     * Skips to the next song in the playlist.
     */
    public static void next(Context context) {
        if (currentPlaylist == null || currentPlaylist.isEmpty()) return;
        currentIndex = (currentIndex + 1) % currentPlaylist.size();
        playSong(context, currentPlaylist, currentIndex);
    }

    /**
     * Rewinds to the start of the song or skips to the previous track.
     */
    public static void previous(Context context) {
        if (currentPlaylist == null || currentPlaylist.isEmpty()) return;
        
        // If the song is more than 5 seconds in, restart the current song
        if (mediaPlayer != null && mediaPlayer.getCurrentPosition() > 5000) {
            mediaPlayer.seekTo(0);
        } else {
            // Otherwise go to previous track
            currentIndex = (currentIndex - 1 + currentPlaylist.size()) % currentPlaylist.size();
            playSong(context, currentPlaylist, currentIndex);
        }
    }

    /**
     * @return True if music is currently playing.
     */
    public static boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    /**
     * @return The Song object currently loaded.
     */
    public static Song getCurrentSong() {
        if (currentPlaylist != null && currentIndex >= 0 && currentIndex < currentPlaylist.size()) {
            return currentPlaylist.get(currentIndex);
        }
        return null;
    }

    public static String currentAlg() {
        return alg;
    }

    public static void setAlg(String algorithm) {
        alg = algorithm;
    }
}
