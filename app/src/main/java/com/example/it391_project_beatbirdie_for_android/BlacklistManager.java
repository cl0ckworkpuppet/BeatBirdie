package com.example.it391_project_beatbirdie_for_android;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import java.util.HashSet;
import java.util.Set;

/**
 * Manages the list of blacklisted audio files using SharedPreferences.
 */
public class BlacklistManager {
    private static final String PREF_BLACKLIST = "blacklisted_files";

    public static void add(Context context, String path) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> blacklist = new HashSet<>(prefs.getStringSet(PREF_BLACKLIST, new HashSet<>()));
        blacklist.add(path);
        prefs.edit().putStringSet(PREF_BLACKLIST, blacklist).apply();
    }

    public static void remove(Context context, String path) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> blacklist = new HashSet<>(prefs.getStringSet(PREF_BLACKLIST, new HashSet<>()));
        blacklist.remove(path);
        prefs.edit().putStringSet(PREF_BLACKLIST, blacklist).apply();
    }

    public static boolean isBlacklisted(Context context, String path) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> blacklist = prefs.getStringSet(PREF_BLACKLIST, new HashSet<>());
        return blacklist.contains(path);
    }

    public static Set<String> getBlacklist(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getStringSet(PREF_BLACKLIST, new HashSet<>());
    }
}
