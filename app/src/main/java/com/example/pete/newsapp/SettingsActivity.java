package com.example.pete.newsapp;

import android.content.SharedPreferences;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class SettingsActivity extends AppCompatActivity {

    private static SettingsActivity settingsActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        settingsActivity = this;
    }

    public static class GuardianPreferenceFragment extends PreferenceFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Set up the preference fragment
            addPreferencesFromResource(R.xml.settings);

            // Set all the summary preferences
            initializeSummary(getPreferenceScreen());
        }

        @Override
        public void onResume() {
            super.onResume();

            // Set the listener for preferences changed
            // (Listener isn't set automatically for some reason)
            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();

            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        // Set the "summary" attribute of preference items to the currently selected value
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            updatePreferenceSummary(findPreference(key));
        }

        /*
        Initialize the preferences screen summaries (show the currently selected value)
        This method is self-calling to drill down into the Preference Screen(s)
        */
        private void initializeSummary(Preference preference) {
            if (preference instanceof PreferenceGroup) {
                PreferenceGroup preferenceGroup = (PreferenceGroup) preference;
                for (int i = 0; i < preferenceGroup.getPreferenceCount(); i++) {
                    initializeSummary(preferenceGroup.getPreference(i));
                }
            } else {
                updatePreferenceSummary(preference);
            }
        }

        private void updatePreferenceSummary(Preference preference) {
            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                listPreference.setSummary(listPreference.getEntry());
            } else if (preference instanceof EditTextPreference) {
                EditTextPreference editTextPreference = (EditTextPreference) preference;
                editTextPreference.setSummary(editTextPreference.getText());
            } else {
                Log.d("updatePreferenceSummary", "Unknown preference type: " + preference.getTitle().toString());
            }
        }
    }
}
