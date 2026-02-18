package com.example.it391_project_beatbirdie_for_android;

// handles playback, like pausing etc.
// created separately to reach across Main and Now Playing.
public class PlaybackHandler {
    // var if music is currently paused
    private static boolean isPaused = true;
    // var for which shuffle algorithm is currently being used
    private static String alg;

    public static void toggle() {
        isPaused = !isPaused;
    }

    public static boolean isPaused() {
        return isPaused;
    }

    public static String currentAlg() { return alg; }

    public static void setAlg(String algorithm) { alg = algorithm; }
}
