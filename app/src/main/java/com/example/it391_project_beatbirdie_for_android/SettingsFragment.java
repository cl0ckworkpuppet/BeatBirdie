package com.example.it391_project_beatbirdie_for_android;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

public class SettingsFragment extends PreferenceFragmentCompat {

    private Preference shufflePref;

    @Override

    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // for some reason the app INSISTS on making dark mode a boolean still. this is a fix for that
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        Object value = sharedPreferences.getAll().get("dark_mode");
        if (value instanceof Boolean) {
            sharedPreferences.edit().remove("dark_mode").apply();
        }

        setPreferencesFromResource(R.xml.preferences, rootKey);

        shufflePref = findPreference("shuffle_algorithms");
        updateShuffleSummary();
        if (shufflePref != null) {
            shufflePref.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(getContext(), ShuffleAlgorithmsActivity.class);
                startActivity(intent);
                return true;
            });
        }

        Preference blacklistPref = findPreference("manage_blacklist");
        if (blacklistPref != null) {
            blacklistPref.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(getContext(), BlacklistActivity.class);
                startActivity(intent);
                return true;
            });
        }

        ListPreference themePref = findPreference("dark_mode");
        if (themePref != null) {
            themePref.setOnPreferenceChangeListener((preference, newValue) -> {
                String themeValue = (String) newValue;
                applyTheme(themeValue);
                return true;
            });
        }

        SwitchPreferenceCompat keepScreenPref = findPreference("lock_screen_on");
        if (keepScreenPref != null) {
            keepScreenPref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean keepOn = (Boolean) newValue;
                if (getActivity() != null) {
                    if (keepOn) {
                        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    } else {
                        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }
                }
                return true;
            });
        }

        LongClickPreference versionPref = findPreference("version_info");
        if (versionPref != null) {
            try {
                PackageInfo pInfo = requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0);
                String version = pInfo.versionName;
                versionPref.setSummary(version);
            } catch (PackageManager.NameNotFoundException e) {
                versionPref.setSummary("Unknown");
            }

            versionPref.setOnPreferenceLongClickListener(preference -> {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
                boolean currentlyEnabled = prefs.getBoolean("experimental_shuffle_enabled", false);
                boolean newState = !currentlyEnabled;
                prefs.edit().putBoolean("experimental_shuffle_enabled", newState).apply();

                String msg = newState ? "Experimental shuffling algorithm has been added" : "Experimental shuffling algorithm has been removed";
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                return true;
            });
        }
    }

    private void updateShuffleSummary() {
        if (shufflePref != null) {
            shufflePref.setSummary("Currently using: " + PlaybackHandler.currentAlg());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateShuffleSummary();
    }

    private void applyTheme(String themeValue) {
        switch (themeValue) {
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case "default":
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
}
