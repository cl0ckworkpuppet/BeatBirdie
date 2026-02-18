package com.example.it391_project_beatbirdie_for_android;

// handles playback, like pausing etc.
// created separately to reach across Main and Now Playing.
public class PlaybackHandler {
    private static boolean isPaused = true;

    public static void toggle() {
        isPaused = !isPaused;
    }

    public static boolean isPaused() {
        return isPaused;
    }
}
