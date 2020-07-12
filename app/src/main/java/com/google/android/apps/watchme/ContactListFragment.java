package com.google.android.apps.watchme;

import android.os.Bundle;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

/**
 * Created by Parzival on 18-03-2018.
 */

public class ContactListFragment extends PreferenceFragment {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.contact_list_preferences);

    }
}
