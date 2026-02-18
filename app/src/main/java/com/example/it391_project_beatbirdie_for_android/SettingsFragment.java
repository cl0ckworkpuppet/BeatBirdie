package com.example.it391_project_beatbirdie_for_android;

import android.content.Intent;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        Preference shufflePref = findPreference("shuffle_algorithms");

        shufflePref.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getContext(), ShuffleAlgorithmsActivity.class);
            startActivity(intent);
            return true;
        });
    }
}