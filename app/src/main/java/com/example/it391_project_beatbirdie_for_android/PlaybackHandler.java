package com.example.it391_project_beatbirdie_for_android;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.PlaybackException;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton-like class that manages the ExoPlayer and playback state.
 */
public class PlaybackHandler {

    private static final String TAG = "PlaybackHandler";

    private static ExoPlayer player;
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

    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    public static void playSong(Context context, List<Song> playlist, int index) {
        if (playlist == null || index < 0 || index >= playlist.size()) return;

        currentPlaylist = playlist;
        generatePlaybackOrder(index);
        playCurrent(context);
    }

    private static void playCurrent(Context context) {
        if (currentPlaylist == null || playbackOrder == null || orderIndex < 0 || orderIndex >= playbackOrder.size()) return;

        int songIndex = playbackOrder.get(orderIndex);
        Song song = currentPlaylist.get(songIndex);

        Log.d(TAG, "Playing song: " + song.getTitle());

        // Release old player
        if (player != null) {
            player.release();
        }

        player = new ExoPlayer.Builder(context).build();

        MediaItem mediaItem = MediaItem.fromUri(song.getUri());
        player.setMediaItem(mediaItem);

        player.setRepeatMode(isLooping ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    player.play();
                    notifySongChanged();
                    startMusicService(appContext);
                } else if (state == Player.STATE_ENDED) {
                    if (!isLooping) next(appContext);
                }
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                Log.e(TAG, "ExoPlayer error: " + error.getMessage());
            }
        });

        player.prepare();
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

    public static void toggle() {
        if (player == null) return;

        if (player.isPlaying()) {
            player.pause();
        } else {
            player.play();
            startMusicService(appContext);
        }
    }

    public static void pause() {
        if (player != null && player.isPlaying()) {
            player.pause();
            notifySongChanged(); // Notify listeners to update UI (play/pause button)
        }
    }

    public static void next(Context context) {
        if (currentPlaylist == null || playbackOrder == null || playbackOrder.isEmpty()) return;
        orderIndex = (orderIndex + 1) % playbackOrder.size();
        playCurrent(context != null ? context : appContext);
    }

    public static void previous(Context context) {
        if (currentPlaylist == null || playbackOrder == null || playbackOrder.isEmpty()) return;

        if (player != null && player.getCurrentPosition() > 5000) {
            player.seekTo(0);
        } else {
            orderIndex = (orderIndex - 1 + playbackOrder.size()) % playbackOrder.size();
            playCurrent(context);
        }
    }

    public static boolean isPlaying() {
        return player != null && player.isPlaying();
    }

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
        if (player != null) {
            player.setRepeatMode(isLooping ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF);
        }
    }

    public static boolean isLooping() {
        return isLooping;
    }

    public static int getCurrentPosition() {
        return (player != null) ? (int) player.getCurrentPosition() : 0;
    }

    public static int getDuration() {
        return (player != null) ? (int) player.getDuration() : 0;
    }

    public static void seekTo(int msec) {
        if (player != null) {
            player.seekTo(msec);
        }
    }
}
