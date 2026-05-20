package com.example.it391_project_beatbirdie_for_android;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
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
        void onQueueModified();
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

    private static void notifyQueueModified() {
        for (PlaybackListener listener : new ArrayList<>(listeners)) {
            listener.onQueueModified();
        }
    }

    public static void init(Context context) {
        appContext = context.getApplicationContext();
        android.content.SharedPreferences prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(appContext);
        alg = prefs.getString("shuffle_alg", "Default");
        isLooping = prefs.getBoolean("is_looping", false);
    }

    public static void playSong(Context context, List<Song> playlist, int index) {
        if (playlist == null || index < 0 || index >= playlist.size()) return;

        currentPlaylist = new ArrayList<>(playlist);
        // Instead of just generating order and playing, we ensure the selected song is at the front
        // of a NEW shuffle if shuffle is enabled, or just at its position in Default mode.
        generatePlaybackOrder(index);
        
        if (!alg.equals("Default")) {
            // Move the selected song to the front of the playback order
            moveOrderIndexToFront(orderIndex);
        }
        
        playCurrent(context);
    }

    private static void moveOrderIndexToFront(int indexInOrder) {
        if (playbackOrder == null || indexInOrder < 0 || indexInOrder >= playbackOrder.size()) return;
        Integer val = playbackOrder.remove(indexInOrder);
        playbackOrder.add(0, val);
        orderIndex = 0;
    }

    /**
     * Updates the underlying playlist (e.g., when the user re-sorts the list in the UI).
     * Synchronizes the playback order to keep playing the same song.
     */
    public static void updatePlaylist(List<Song> newList) {
        if (newList == null) return;

        // Remember what we're currently playing
        Song currentSong = getCurrentSong();
        
        // Update the source list
        currentPlaylist = new ArrayList<>(newList);

        // Find current song's new index in the source list
        int newGlobalIndex = -1;
        if (currentSong != null) {
            for (int i = 0; i < currentPlaylist.size(); i++) {
                if (currentPlaylist.get(i).getUri().equals(currentSong.getUri())) {
                    newGlobalIndex = i;
                    break;
                }
            }
        }

        if (newGlobalIndex != -1) {
            // Re-sync the queue based on the new list but keeping the current song
            if (alg.equals("Default")) {
                playbackOrder = new ArrayList<>();
                for (int i = 0; i < currentPlaylist.size(); i++) {
                    playbackOrder.add(i);
                }
                orderIndex = newGlobalIndex;
            } else {
                generatePlaybackOrder(newGlobalIndex);
            }
        } else {
            // Current song is gone or nothing was playing; reset queue
            if (!currentPlaylist.isEmpty()) {
                generatePlaybackOrder(-1);
            } else {
                playbackOrder = new ArrayList<>();
                orderIndex = -1;
                if (player != null) player.stop();
            }
        }
        notifyQueueModified();
    }

    private static void playCurrent(Context context) {
        if (currentPlaylist == null || playbackOrder == null || orderIndex < 0 || orderIndex >= playbackOrder.size()) return;

        int songIndex = playbackOrder.get(orderIndex);
        Song song = currentPlaylist.get(songIndex);

        // Check for invalid/corrupted song or filtered song
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context != null ? context : appContext);
        int minDurationThreshold = Integer.parseInt(prefs.getString("filter_duration", "0"));

        if (song.getDuration() <= 0 || song.getDuration() < minDurationThreshold) {
            String reason = song.getDuration() <= 0 ? "invalid" : "too short";
            Log.w(TAG, "Skipping " + reason + " song: " + song.getTitle());
            if (context != null) {
                android.widget.Toast.makeText(context, "Skipping " + reason + " song: " + song.getTitle(), android.widget.Toast.LENGTH_SHORT).show();
            }
            next(context != null ? context : appContext);
            return;
        }

        Log.d(TAG, "Playing song: " + song.getTitle());

        // Initialize player once and reuse it
        if (player == null) {
            player = new ExoPlayer.Builder(context.getApplicationContext()).build();
            player.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_READY) {
                        notifySongChanged(); // Update UI when duration is known
                        startMusicService(appContext);
                    } else if (state == Player.STATE_ENDED) {
                        if (!isLooping) next(appContext);
                    }
                }

                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    notifySongChanged(); // Update Play/Pause buttons
                }

                @Override
                public void onPlayerError(@NonNull PlaybackException error) {
                    Log.e(TAG, "ExoPlayer error: " + error.getMessage());
                }
            });
        }

        MediaItem mediaItem = MediaItem.fromUri(song.getUri());
        player.setMediaItem(mediaItem);
        player.setRepeatMode(isLooping ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF);
        player.setPlayWhenReady(true);
        player.prepare();

        // Notify immediately to update metadata (Title, Artist, Art)
        notifySongChanged();
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
            orderIndex = -1;
        }

        playbackOrder = newOrder;
        notifyQueueModified();
    }

    public static List<Song> getFullQueue() {
        List<Song> queue = new ArrayList<>();
        if (playbackOrder != null && currentPlaylist != null) {
            for (int songIndex : playbackOrder) {
                queue.add(currentPlaylist.get(songIndex));
            }
        }
        return queue;
    }

    public static int getQueueOrderIndex() {
        return orderIndex;
    }

    public static void moveSongToFront(int position) {
        if (playbackOrder == null || position < 0 || position >= playbackOrder.size()) return;

        int currentSongGlobalIndex = -1;
        if (orderIndex >= 0 && orderIndex < playbackOrder.size()) {
            currentSongGlobalIndex = playbackOrder.get(orderIndex);
        }

        Integer songIndexToMove = playbackOrder.remove(position);

        // Update orderIndex to point to the same song after removal
        if (currentSongGlobalIndex != -1) {
            for (int i = 0; i < playbackOrder.size(); i++) {
                if (playbackOrder.get(i) == currentSongGlobalIndex) {
                    orderIndex = i;
                    break;
                }
            }
        }

        // Move to immediately after the current song (Play Next)
        int targetIndex = (orderIndex >= 0) ? orderIndex + 1 : 0;
        if (targetIndex > playbackOrder.size()) targetIndex = playbackOrder.size();

        playbackOrder.add(targetIndex, songIndexToMove);

        // Final sync of orderIndex (in case the insertion shifted it)
        if (currentSongGlobalIndex != -1) {
            for (int i = 0; i < playbackOrder.size(); i++) {
                if (playbackOrder.get(i) == currentSongGlobalIndex) {
                    orderIndex = i;
                    break;
                }
            }
        }

        notifyQueueModified();
    }

    public static void removeSongFromQueue(int position) {
        if (playbackOrder == null || position < 0 || position >= playbackOrder.size()) return;

        int currentSongGlobalIndex = -1;
        if (orderIndex >= 0 && orderIndex < playbackOrder.size()) {
            currentSongGlobalIndex = playbackOrder.get(orderIndex);
        }

        int removedGlobalIndex = playbackOrder.remove(position);

        if (removedGlobalIndex == currentSongGlobalIndex) {
            if (playbackOrder.isEmpty()) {
                orderIndex = -1;
                if (player != null) player.stop();
            } else {
                if (orderIndex >= playbackOrder.size()) {
                    orderIndex = 0;
                }
                // Play the new song at current index if it was playing
                if (player != null && player.isPlaying()) {
                    playCurrent(appContext);
                }
            }
        } else {
            if (currentSongGlobalIndex != -1) {
                for (int i = 0; i < playbackOrder.size(); i++) {
                    if (playbackOrder.get(i) == currentSongGlobalIndex) {
                        orderIndex = i;
                        break;
                    }
                }
            }
        }
        notifyQueueModified();
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
        if (player == null) return false;
        // Check playWhenReady so the UI button stays as "Pause" during buffering/loading
        return player.getPlayWhenReady();
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
        if (appContext != null) {
            android.content.SharedPreferences prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(appContext);
            prefs.edit().putString("shuffle_alg", alg).apply();
        }
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
        if (appContext != null) {
            android.content.SharedPreferences prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(appContext);
            prefs.edit().putBoolean("is_looping", isLooping).apply();
        }
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
        if (player != null && player.getDuration() > 0) {
            return (int) player.getDuration();
        }
        Song current = getCurrentSong();
        return (current != null) ? current.getDuration() : 0;
    }

    public static void seekTo(int msec) {
        if (player != null) {
            player.seekTo(msec);
        }
    }
}
