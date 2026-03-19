package com.example.it391_project_beatbirdie_for_android;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.util.Log;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton-like class that manages the MediaPlayer and playback state.
 * This class allows consistent music control across different Activities.
 */
public class PlaybackHandler {

    private static final String TAG = "PlaybackHandler";
    private static MediaPlayer mediaPlayer;
    private static List<Song> currentPlaylist;
    private static List<Integer> playbackOrder;
    private static int orderIndex = -1;
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
        
        // When a specific song is selected, we regenerate the shuffle order
        // and ensure the selected song is at the current position.
        generatePlaybackOrder(index);
        
        playCurrent(context);
    }

    private static void playCurrent(Context context) {
        if (currentPlaylist == null || playbackOrder == null || orderIndex < 0 || orderIndex >= playbackOrder.size()) return;

        int songIndex = playbackOrder.get(orderIndex);
        Song song = currentPlaylist.get(songIndex);

        Log.d(TAG, "Playing song: " + song.getTitle() + " (Order index: " + orderIndex + ", Song index: " + songIndex + ")");

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

    private static void generatePlaybackOrder(int startingSongIndex) {
        if (currentPlaylist == null) return;
        
        int size = currentPlaylist.size();
        List<Integer> newOrder;

        switch (alg) {
            case "Fisher-Yates":
                newOrder = ShuffleManager.fisherYatesShuffle(size);
                break;
            case "True Random, No Repeats":
                newOrder = ShuffleManager.trueRandomNoRepeats(size);
                break;
            case "Fair Play":
                newOrder = ShuffleManager.fairPlayShuffle(currentPlaylist);
                break;
            default:
                newOrder = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    newOrder.add(i);
                }
                break;
        }

        // If a specific song was requested to start, move it to the front or find it
        if (startingSongIndex != -1) {
            int posInOrder = -1;
            for (int i = 0; i < newOrder.size(); i++) {
                if (newOrder.get(i) == startingSongIndex) {
                    posInOrder = i;
                    break;
                }
            }
            
            if (posInOrder != -1) {
                if (alg.equals("Default")) {
                    orderIndex = startingSongIndex;
                } else {
                    orderIndex = posInOrder;
                }
            }
        } else {
            orderIndex = 0;
        }
        
        playbackOrder = newOrder;
        logPlaybackOrder();
    }

    private static void logPlaybackOrder() {
        if (playbackOrder == null || currentPlaylist == null) return;
        
        StringBuilder sb = new StringBuilder();
        sb.append("Shuffle Applied: ").append(alg).append("\n");
        sb.append("New Playback Order (Song Titles):\n");
        for (int i = 0; i < playbackOrder.size(); i++) {
            int songIndex = playbackOrder.get(i);
            String title = currentPlaylist.get(songIndex).getTitle();
            sb.append(i + 1).append(". ").append(title);
            if (i == orderIndex) {
                sb.append(" <--- CURRENTLY PLAYING");
            }
            sb.append("\n");
        }
        Log.i(TAG, sb.toString());
    }

    public static List<String> getQueueTitles() {
        List<String> titles = new ArrayList<>();
        if (playbackOrder != null && currentPlaylist != null) {
            for (int i = 0; i < playbackOrder.size(); i++) {
                int songIndex = playbackOrder.get(i);
                String prefix = (i == orderIndex) ? "▶ " : "";
                titles.add(prefix + currentPlaylist.get(songIndex).getTitle());
            }
        }
        return titles;
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
        if (currentPlaylist == null || playbackOrder == null || playbackOrder.isEmpty()) return;
        orderIndex = (orderIndex + 1) % playbackOrder.size();
        playCurrent(context);
    }

    /**
     * Rewinds to the start of the song or skips to the previous track.
     */
    public static void previous(Context context) {
        if (currentPlaylist == null || playbackOrder == null || playbackOrder.isEmpty()) return;
        
        // If the song is more than 5 seconds in, restart the current song
        if (mediaPlayer != null && mediaPlayer.getCurrentPosition() > 5000) {
            mediaPlayer.seekTo(0);
        } else {
            // Otherwise go to previous track
            orderIndex = (orderIndex - 1 + playbackOrder.size()) % playbackOrder.size();
            playCurrent(context);
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
        if (currentPlaylist != null && playbackOrder != null && orderIndex >= 0 && orderIndex < playbackOrder.size()) {
            int songIndex = playbackOrder.get(orderIndex);
            return currentPlaylist.get(songIndex);
        }
        return null;
    }

    public static String currentAlg() {
        return alg;
    }

    public static void setAlg(String algorithm) {
        alg = algorithm;
        // If music is already playing, we might want to reshuffle from the current song
        if (currentPlaylist != null) {
            int currentSongIndex = -1;
            if (playbackOrder != null && orderIndex >= 0 && orderIndex < playbackOrder.size()) {
                currentSongIndex = playbackOrder.get(orderIndex);
            }
            generatePlaybackOrder(currentSongIndex);
        }
    }

    // get position of scrubber bar
    public static int getCurrentPosition() {
        return (mediaPlayer != null) ? mediaPlayer.getCurrentPosition() : 0;
    }

    // get duration of song
    public static int getDuration() {
        return (mediaPlayer != null) ? mediaPlayer.getDuration() : 0;
    }

    // seek to a specific position of song on scrubber bar
    public static void seekTo(int msec) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(msec);
        }
    }
}
