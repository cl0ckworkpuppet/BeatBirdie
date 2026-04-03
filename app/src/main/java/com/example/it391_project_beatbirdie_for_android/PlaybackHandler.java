package com.example.it391_project_beatbirdie_for_android;

import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.PowerManager;
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
    private static boolean isLooping = false;
    private static Context appContext;

    public interface PlaybackListener {
        void onSongChanged();
    }

    private static final List<PlaybackListener> listeners = new ArrayList<>();

    public static void addListener(PlaybackListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static void removeListener(PlaybackListener listener) {
        listeners.remove(listener);
    }

    private static void notifySongChanged() {
        for (PlaybackListener listener : new ArrayList<>(listeners)) {
            listener.onSongChanged();
        }
    }

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

        if (mediaPlayer != null) {
            mediaPlayer.reset(); // Use reset instead of release for efficiency if possible
        } else {
            mediaPlayer = new MediaPlayer();
        }

        mediaPlayer.setLooping(isLooping);
        
        // Keep CPU awake during playback
        mediaPlayer.setWakeMode(appContext, PowerManager.PARTIAL_WAKE_LOCK);
        
        mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
        );

        try {
            mediaPlayer.setDataSource(appContext, song.getUri());
            
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                notifySongChanged();
                // Refresh/Start the foreground service
                startMusicService(appContext);
            });

            // i'm messing with this mediaPlayer function right now.
            // if it looks ugly that's because It Is.
            mediaPlayer.setOnCompletionListener(mp -> {
                int pos = mp.getCurrentPosition();
                int dur = mp.getDuration();

                Log.d(TAG, "Completion fired at " + pos + " / " + dur);

                if (dur > 0 && pos < dur - 1000) {
                    Log.w(TAG, "Premature completion detected, attempting resume");

                    int resumePos = Math.max(0, pos - 150); // go back slightly. tweak as needed

                    try {
                        mp.seekTo(resumePos);
                        mp.start();
                    } catch (IllegalStateException e) {
                        Log.e(TAG, "Resume failed, skipping instead", e);
                        if (!isLooping) next(appContext);
                    }

                } else {
                    if (!isLooping) next(appContext);
                }
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error: what=" + what + ", extra=" + extra);
                return true; 
            });

            mediaPlayer.prepareAsync(); 
            
        } catch (IOException e) {
            Log.e(TAG, "Error setting data source", e);
        }
    }

    private static void startMusicService(Context context) {

        if (context == null) return;
        Intent serviceIntent = new Intent(context, MusicService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
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
            startMusicService(appContext);
        }
    }

    /**
     * Skips to the next song in the playlist.
     */
    public static void next(Context context) {
        if (currentPlaylist == null || playbackOrder == null || playbackOrder.isEmpty()) return;
        orderIndex = (orderIndex + 1) % playbackOrder.size();
        playCurrent(context != null ? context : appContext);
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
        if (currentPlaylist != null) {
            int currentSongIndex = -1;
            if (playbackOrder != null && orderIndex >= 0 && orderIndex < playbackOrder.size()) {
                currentSongIndex = playbackOrder.get(orderIndex);
            }
            generatePlaybackOrder(currentSongIndex);
        }
    }

    public static void setLooping(boolean looping) {
        isLooping = looping;
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(isLooping);
        }
    }

    public static boolean isLooping() {
        return isLooping;
    }

    // get position of scrubber bar
    public static int getCurrentPosition() {
        try {
            return (mediaPlayer != null) ? mediaPlayer.getCurrentPosition() : 0;
        } catch (IllegalStateException e) {
            return 0;
        }
    }

    // get duration of song
    public static int getDuration() {
        try {
            return (mediaPlayer != null) ? mediaPlayer.getDuration() : 0;
        } catch (IllegalStateException e) {
            return 0;
        }
    }

    // seek to a specific position of song on scrubber bar
    public static void seekTo(int msec) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(msec);
        }
    }
}
