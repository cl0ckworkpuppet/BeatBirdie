package com.example.it391_project_beatbirdie_for_android;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Provides various shuffle algorithms for playlists.
 */
public class ShuffleManager {
    private static final Random random = new Random();

    /**
     * Greatest Common Divisor helper method.
     */
    private static int gcd(int a, int b) {
        while (b != 0) {
            int temp = b;
            b = a % b;
            a = temp;
        }

        return a;
    }

    /**
     * Reverses the lowest 'bits' bits of a number.
     */
    private static int reverseBits(int value, int bits) {
        int reversed = 0;

        for (int i = 0; i < bits; i++) {
            reversed <<= 1;
            reversed |= (value & 1);
            value >>= 1;
        }

        return reversed;
    }

    /**
     * Standard Fisher-Yates shuffle algorithm.
     * Complexity: O(n)
     */
    public static List<Integer> fisherYatesShuffle(int size, int startingIndex) {
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

        if (startingIndex >= 0 && startingIndex < size) {
            indices.remove(Integer.valueOf(startingIndex));
            indices.add(0, startingIndex);
        }

        return indices;
    }

    /**
     * A "True Random, No Repeats" algorithm.
     * Picks a random item from the remaining list until all are picked.
     * Complexity: O(n^2) due to list removals.
     */
    public static List<Integer> trueRandomNoRepeats(int size, int startingIndex) {
        List<Integer> available = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            available.add(i);
        }
        List<Integer> result = new ArrayList<>();

        if (startingIndex >= 0 && startingIndex < size) {
            result.add(startingIndex);
            available.remove(Integer.valueOf(startingIndex));
        }

        while (!available.isEmpty()) {
            int index = random.nextInt(available.size());
            result.add(available.remove(index));
        }
        return result;
    }

    /**
     * Fair Play algorithm: Shuffles and then attempts to separate songs by the same artist or album.
     * Complexity: best case O(n), worst case O(n^2).
     */
    public static List<Integer> fairPlayShuffle(List<Song> songs, int startingIndex) {

        List<Integer> remaining = new ArrayList<>();

        for (int i = 0; i < songs.size(); i++) {
            remaining.add(i);
        }

        List<Integer> result = new ArrayList<>();

        // If a starting index is specified, use it as the first element.
        if (startingIndex >= 0 && startingIndex < songs.size()) {
            result.add(startingIndex);
            remaining.remove(Integer.valueOf(startingIndex));
        }

        while (!remaining.isEmpty()) {

            // First song can be anything (if not already set by startingIndex)
            if (result.isEmpty()) {

                int chosen = remaining.remove(random.nextInt(remaining.size()));
                result.add(chosen);

                continue;
            }

            Song previous = songs.get(result.get(result.size() - 1));

            List<Integer> differentAlbum = new ArrayList<>();
            List<Integer> differentArtist = new ArrayList<>();

            for (Integer index : remaining) {

                Song candidate = songs.get(index);

                boolean sameAlbum =
                        candidate.getAlbum().equalsIgnoreCase(previous.getAlbum());

                boolean sameArtist =
                        candidate.getArtist().equalsIgnoreCase(previous.getArtist());

                // Highest priority:
                // different album
                if (!sameAlbum) {
                    differentAlbum.add(index);
                }

                // Backup priority:
                // same album allowed, but different artist
                if (!sameArtist) {
                    differentArtist.add(index);
                }
            }

            int chosen;

            // Tier 1:
            // Prefer different album
            if (!differentAlbum.isEmpty()) {

                chosen = differentAlbum.get(
                        random.nextInt(differentAlbum.size()));

                // Tier 2:
                // Otherwise prefer different artist
            } else if (!differentArtist.isEmpty()) {

                chosen = differentArtist.get(
                        random.nextInt(differentArtist.size()));

                // Tier 3:
                // Otherwise anything goes
            } else {

                chosen = remaining.get(
                        random.nextInt(remaining.size()));
            }

            result.add(chosen);

            remaining.remove(Integer.valueOf(chosen));
        }

        return result;
    }

    /**
     * Sattolo's Algorithm
     * Produces a single cyclic permutation.
     * No element can remain in its original position.
     * Complexity: O(n)
     */
    public static List<Integer> sattoloShuffle(int size, int startingIndex) {
        List<Integer> indices = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            indices.add(i);
        }

        for (int i = size - 1; i > 0; i--) {
            // j is strictly less than i
            int j = random.nextInt(i);

            int temp = indices.get(i);
            indices.set(i, indices.get(j));
            indices.set(j, temp);
        }

        if (startingIndex >= 0 && startingIndex < size) {
            indices.remove(Integer.valueOf(startingIndex));
            indices.add(0, startingIndex);
        }

        return indices;
    }

    /**
     * Casino Shuffle (Riffle Shuffle)
     * Simulates shuffling cards by splitting the list
     * and probabilistically interleaving the halves.
     * Complexity: O(n)
     */
    public static List<Integer> casinoShuffle(int size, int startingIndex) {
        List<Integer> deck = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            deck.add(i);
        }

        // Random cut point near the middle
        int cut = size / 2 + random.nextInt(Math.max(1, size / 4)) - size / 8;

        List<Integer> left = new ArrayList<>(deck.subList(0, Math.max(0, cut)));
        List<Integer> right = new ArrayList<>(deck.subList(Math.max(0, cut), size));

        List<Integer> shuffled = new ArrayList<>();

        while (!left.isEmpty() || !right.isEmpty()) {

            // Probability weighted by remaining pile sizes
            boolean takeLeft;

            if (left.isEmpty()) {
                takeLeft = false;
            } else if (right.isEmpty()) {
                takeLeft = true;
            } else {
                takeLeft = random.nextInt(left.size() + right.size()) < left.size();
            }

            if (takeLeft) {
                shuffled.add(left.remove(0));
            } else {
                shuffled.add(right.remove(0));
            }
        }

        if (startingIndex >= 0 && startingIndex < size) {
            shuffled.remove(Integer.valueOf(startingIndex));
            shuffled.add(0, startingIndex);
        }

        return shuffled;
    }

    /**
     * Prime-Step Traversal Shuffle
     * Traverses the list circularly using a step size
     * that is coprime with the playlist size.
     *
     * Complexity: O(n)
     */
    public static List<Integer> primeStepShuffle(int size, int startingIndex) {
        List<Integer> result = new ArrayList<>();

        if (size <= 0) {
            return result;
        }

        int start = (startingIndex >= 0 && startingIndex < size) ? startingIndex : random.nextInt(size);

        // Find a step size coprime with size
        int step;

        do {
            step = random.nextInt(size - 1) + 1;
        } while (gcd(step, size) != 1);

        boolean[] visited = new boolean[size];

        int current = start;

        for (int i = 0; i < size; i++) {
            result.add(current);
            visited[current] = true;

            current = (current + step) % size;
        }

        return result;
    }

    /**
     * Bit-Reversal Shuffle
     * Reorders indices according to reversed binary bits.
     * Works best when size is a power of two.
     * Non-power-of-two sizes are truncated naturally.
     * NOTE: Deterministic. Should be starred.
     *
     * Complexity: O(n)
     */
    public static List<Integer> bitReversalShuffle(int size, int startingIndex) {
        List<Integer> result = new ArrayList<>();

        if (size <= 0) {
            return result;
        }

        int bits = 32 - Integer.numberOfLeadingZeros(size - 1);

        boolean[] used = new boolean[size];

        for (int i = 0; i < size; i++) {

            int reversed = reverseBits(i, bits);

            if (reversed < size && !used[reversed]) {
                result.add(reversed);
                used[reversed] = true;
            }
        }

        // Fill in any missing indices (for non-powers of two)
        for (int i = 0; i < size; i++) {
            if (!used[i]) {
                result.add(i);
            }
        }

        if (startingIndex >= 0 && startingIndex < size) {
            result.remove(Integer.valueOf(startingIndex));
            result.add(0, startingIndex);
        }

        return result;
    }

    /**
     * "Experimental" Shuffle (Cursed Awful Dogshit Shuffle)
     *
     * Generates every possible permutation,
     * stores them all in memory,
     * shuffles the permutations,
     * then picks one.
     *
     * Complexity:
     * O(n!)
     *
     * Do not use under any circumstance unless you enjoy making apps unresponsive
     */
    public static List<Integer> cursedAwfulDogshitShuffle(int size, int startingIndex) {
        if (size > 8) {
            throw new IllegalArgumentException("Complexity too high (O(n!)). Size " + size + " is dangerous.");
        }

        List<Integer> base = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            base.add(i);
        }

        List<List<Integer>> permutations = new ArrayList<>();

        generatePermutations(base, 0, permutations);

        Collections.shuffle(permutations);

        List<Integer> result =
                new ArrayList<>(permutations.get(0));

        if (startingIndex >= 0 && startingIndex < size) {
            result.remove(Integer.valueOf(startingIndex));
            result.add(0, startingIndex);
        }

        return result;
    }

    private static void generatePermutations(
            List<Integer> list,
            int index,
            List<List<Integer>> output) {

        if (index == list.size()) {
            output.add(new ArrayList<>(list));
            return;
        }

        for (int i = index; i < list.size(); i++) {

            Collections.swap(list, index, i);

            generatePermutations(list, index + 1, output);

            Collections.swap(list, index, i);
        }
    }
}
