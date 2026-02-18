package com.example.it391_project_beatbirdie_for_android;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsFragment extends PreferenceFragmentCompat {

    // basically this just calls the preferences.xml list and brings it to the activity.
    // nothing else needs to happen in this file
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }
}