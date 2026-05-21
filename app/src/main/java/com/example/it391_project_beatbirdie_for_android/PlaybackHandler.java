package com.example.it391_project_beatbirdie_for_android;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.util.UnstableApi;

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
    private static int playingPlaylistId = -1; // -1 for Main Library, or the Room ID
    private static String alg = "*Play in Order";
    private static boolean isLooping = false;
    private static Context appContext;

    public interface PlaybackListener {
        void onSongChanged();
        void onQueueModified();
        void onError(String message);
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

    private static void notifyError(String message) {
        for (PlaybackListener listener : new ArrayList<>(listeners)) {
            listener.onError(message);
        }
    }

    public static void init(Context context) {
        appContext = context.getApplicationContext();
        android.content.SharedPreferences prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(appContext);
        alg = prefs.getString("shuffle_alg", "*Play in Order");
        
        // Migration and safety checks
        if ("Default".equals(alg) || "Play in Order".equals(alg)) alg = "*Play in Order";
        if ("Bit-Reversal".equals(alg)) alg = "*Bit-Reversal";
        if ("Sattolo".equals(alg)) alg = "Sattolo's Algorithm";
        if ("Casino".equals(alg)) alg = "Casino Shuffle";
        
        // Ensure experimental algorithm isn't used as a persistent default on boot
        if ("EXPERIMENTAL SHUFFLE".equals(alg)) {
            alg = "*Play in Order";
            prefs.edit().putString("shuffle_alg", alg).apply();
        }
        
        isLooping = prefs.getBoolean("is_looping", false);
    }

    public static void playSong(Context context, List<Song> playlist, int index, int playlistId) {
        if (playlist == null || index < 0 || index >= playlist.size()) return;

        playingPlaylistId = playlistId;
        currentPlaylist = new ArrayList<>(playlist);
        generatePlaybackOrder(index);
        
        playCurrent(context);
    }

    /**
     * Updates the underlying playlist (e.g., when the user re-sorts the list in the UI or adds/removes songs).
     * Synchronizes the playback order to keep playing the same song and handles additions/removals.
     */
    public static void updatePlaylist(List<Song> newList, int playlistId) {
        if (newList == null || playingPlaylistId != playlistId) return;

        // Remember what we're currently playing and if it was playing
        Song currentSong = getCurrentSong();
        boolean wasPlaying = isPlaying();

        if (alg.equals("*Play in Order")) {
            currentPlaylist = new ArrayList<>(newList);
            playbackOrder = new ArrayList<>();
            for (int i = 0; i < currentPlaylist.size(); i++) {
                playbackOrder.add(i);
            }

            // Find current song in the NEW list
            int newGlobalIndex = -1;
            if (currentSong != null) {
                for (int i = 0; i < newList.size(); i++) {
                    if (newList.get(i).getUri().equals(currentSong.getUri())) {
                        newGlobalIndex = i;
                        break;
                    }
                }
            }

            if (newGlobalIndex != -1) {
                orderIndex = newGlobalIndex;
            } else if (currentSong != null && !currentPlaylist.isEmpty()) {
                // Current song was removed. orderIndex now points to what was the next song.
                if (orderIndex >= currentPlaylist.size()) {
                    orderIndex = 0;
                }
                if (wasPlaying) {
                    playCurrent(appContext);
                } else {
                    notifySongChanged();
                }
            } else if (currentPlaylist.isEmpty()) {
                orderIndex = -1;
                if (player != null) player.stop();
            }
        } else {
            // Shuffled or other algorithms
            syncPlaybackOrder(newList, currentSong);

            // Check if the current song changed (meaning it was removed)
            if (currentSong != null) {
                Song nowCurrent = getCurrentSong();
                if (nowCurrent == null || !nowCurrent.getUri().equals(currentSong.getUri())) {
                    if (wasPlaying) {
                        playCurrent(appContext);
                    } else {
                        notifySongChanged();
                    }
                }
            }
        }
        notifyQueueModified();
    }

    private static void syncPlaybackOrder(List<Song> newList, Song currentSong) {
        int oldOrderIndex = orderIndex;

        // Map URIs to NEW indices
        java.util.Map<android.net.Uri, Integer> uriToNewIndex = new java.util.HashMap<>();
        for (int i = 0; i < newList.size(); i++) {
            uriToNewIndex.put(newList.get(i).getUri(), i);
        }

        List<Integer> newPlaybackOrder = new ArrayList<>();
        java.util.Set<android.net.Uri> handledUris = new java.util.HashSet<>();

        // 1. Keep the existing order for songs that are still present
        if (playbackOrder != null && currentPlaylist != null) {
            for (int oldIdx : playbackOrder) {
                if (oldIdx >= 0 && oldIdx < currentPlaylist.size()) {
                    Song s = currentPlaylist.get(oldIdx);
                    Integer newIdx = uriToNewIndex.get(s.getUri());
                    if (newIdx != null) {
                        newPlaybackOrder.add(newIdx);
                        handledUris.add(s.getUri());
                    }
                }
            }
        }

        // 2. Add any NEW songs to the end of the playback order
        for (int i = 0; i < newList.size(); i++) {
            Song s = newList.get(i);
            if (!handledUris.contains(s.getUri())) {
                newPlaybackOrder.add(i);
            }
        }

        // 3. Update current state
        currentPlaylist = new ArrayList<>(newList);
        playbackOrder = newPlaybackOrder;

        // 4. Find where the current song ended up in the new order
        orderIndex = -1;
        if (currentSong != null) {
            for (int i = 0; i < playbackOrder.size(); i++) {
                if (currentPlaylist.get(playbackOrder.get(i)).getUri().equals(currentSong.getUri())) {
                    orderIndex = i;
                    break;
                }
            }
        }
        
        // If we lost the current song in the order, fallback to the same index (points to next song in shuffle)
        if (orderIndex == -1 && currentSong != null && !playbackOrder.isEmpty()) {
            orderIndex = oldOrderIndex;
            if (orderIndex < 0 || orderIndex >= playbackOrder.size()) {
                orderIndex = 0;
            }
        }
    }

    @UnstableApi
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
            DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(context.getApplicationContext())
                    .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER);
            
            player = new ExoPlayer.Builder(context.getApplicationContext(), renderersFactory).build();
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

        try {
            switch (alg) {
                case "Fisher-Yates":
                    newOrder = ShuffleManager.fisherYatesShuffle(size, startingSongIndex);
                    break;
                case "True Random, No Repeats":
                    newOrder = ShuffleManager.trueRandomNoRepeats(size, startingSongIndex);
                    break;
                case "Fair Play":
                    newOrder = ShuffleManager.fairPlayShuffle(currentPlaylist, startingSongIndex);
                    break;
                case "Sattolo's Algorithm":
                    newOrder = ShuffleManager.sattoloShuffle(size, startingSongIndex);
                    break;
                case "Casino Shuffle":
                    newOrder = ShuffleManager.casinoShuffle(size, startingSongIndex);
                    break;
                case "Prime-Step":
                    newOrder = ShuffleManager.primeStepShuffle(size, startingSongIndex);
                    break;
                case "*Bit-Reversal":
                    newOrder = ShuffleManager.bitReversalShuffle(size, startingSongIndex);
                    break;
                case "EXPERIMENTAL SHUFFLE":
                    newOrder = ShuffleManager.cursedAwfulDogshitShuffle(size, startingSongIndex);
                    break;
                case "*Play in Order":
                default:
                    newOrder = new ArrayList<>();
                    for (int i = 0; i < size; i++) {
                        newOrder.add(i);
                    }
                    break;
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error generating playback order with algorithm: " + alg, t);
            // Fallback to "Play in Order"
            newOrder = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                newOrder.add(i);
            }
            alg = "*Play in Order";
            notifyError("Encountered a problem while trying to shuffle your songs. Error: " + t.getClass().getSimpleName());
        }

        if (startingSongIndex != -1) {
            if (alg.equals("*Play in Order")) {
                orderIndex = startingSongIndex;
            } else {
                // Shuffled algorithms now place the starting song at index 0
                orderIndex = 0;
            }
        } else {
            orderIndex = -1;
        }

        playbackOrder = newOrder;
        notifyQueueModified();
    }

    public static int getPlayingPlaylistId() {
        return playingPlaylistId;
    }

    public static void setPlayingPlaylistId(int id) {
        playingPlaylistId = id;
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

        boolean wasPlaying = isPlaying();
        int removedGlobalIndex = playbackOrder.remove(position);

        if (removedGlobalIndex == currentSongGlobalIndex) {
            if (playbackOrder.isEmpty()) {
                orderIndex = -1;
                if (player != null) player.stop();
            } else {
                if (orderIndex >= playbackOrder.size()) {
                    orderIndex = 0;
                }
                // Skip to the next song if it was playing
                if (wasPlaying) {
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
