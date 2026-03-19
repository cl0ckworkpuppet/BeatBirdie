package com.example.it391_project_beatbirdie_for_android;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Provides various shuffle algorithms for playlists.
 */
public class ShuffleManager {
    private static final Random random = new Random();

    /**
     * Standard Fisher-Yates shuffle algorithm.
     * Complexity: O(n)
     */
    public static List<Integer> fisherYatesShuffle(int size) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            indices.add(i);
        }
        for (int i = size - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temp = indices.get(i);
            indices.set(i, indices.get(j));
            indices.set(j, temp);
        }
        return indices;
    }

    /**
     * A "True Random, No Repeats" algorithm.
     * Picks a random item from the remaining list until all are picked.
     * Complexity: O(n^2) due to list removals.
     */
    public static List<Integer> trueRandomNoRepeats(int size) {
        List<Integer> available = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            available.add(i);
        }
        List<Integer> result = new ArrayList<>();
        while (!available.isEmpty()) {
            int index = random.nextInt(available.size());
            result.add(available.remove(index));
        }
        return result;
    }

    /**
     * Fair Play algorithm: Shuffles and then attempts to separate songs by the same artist or album.
     */
    public static List<Integer> fairPlayShuffle(List<Song> songs) {
        List<Integer> indices = fisherYatesShuffle(songs.size());
        
        if (songs.size() < 3) return indices;

        // Attempt to separate songs by the same artist or album
        for (int i = 0; i < indices.size() - 1; i++) {
            Song current = songs.get(indices.get(i));
            Song next = songs.get(indices.get(i + 1));
            
            // Check if next song is from same artist or same album
            if (current.getArtist().equalsIgnoreCase(next.getArtist()) || 
                current.getAlbum().equalsIgnoreCase(next.getAlbum())) {
                
                // Find a song further down the list with a different artist AND album to swap with
                for (int j = i + 2; j < indices.size(); j++) {
                    Song candidate = songs.get(indices.get(j));
                    if (!candidate.getArtist().equalsIgnoreCase(current.getArtist()) && 
                        !candidate.getAlbum().equalsIgnoreCase(current.getAlbum())) {

                        int temp = indices.get(i + 1);
                        indices.set(i + 1, indices.get(j));
                        indices.set(j, temp);
                        break;
                    }
                }
            }
        }
        return indices;
    }
}
