package com.google.android.apps.watchme;

import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by Parzival on 18-03-2018.
 */

public class SettingsFragment extends PreferenceFragment {
    public static final int REQUEST_READ_CONTACTS = 79;
    ArrayList[] lists = new ArrayList[2];

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {


        try {
            addPreferencesFromResource(R.xml.settings_preferences);
        } catch (Exception e) {
            Log.e("Error--", e.toString());
        }

        final MultiSelectListPreference listPreference = (MultiSelectListPreference) findPreference("select_contact");

        Log.e("PREFERENCE", String.valueOf(listPreference == null));

        // THIS IS REQUIRED IF YOU DON'T HAVE 'entries' and 'entryValues' in your XML
        setListPreferenceData(listPreference);


        listPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                setListPreferenceData(listPreference);
                return true;
            }
        });

        listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Set<String> selections = ((Set<String>) newValue);
                if (selections.size() < 4){
                    return true;
                } else {
                    Toast.makeText(getActivity(), "Maximum 3 Contacts", Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        });

        final EditTextPreference editTextPreference = (EditTextPreference)findPreference("custom_text");

        editTextPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int val = newValue.toString().length();
                if (val < 160) {

                    Log.d("SAVED", "Value saved: " + val);
                    return true;
                }
                else {
                    // invalid you can show invalid message
                    Toast.makeText(getActivity(), "Message Exceeds 160 characters", Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        });

    }

    protected void setListPreferenceData(MultiSelectListPreference lp) {
        Log.e("Contacts---", "started");

        if (ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) {
            lists = getAllContacts();
        } else {
            requestPermission();
        }


        Log.e("Contacts---", "gotten");

        List<CharSequence> entries = new ArrayList<CharSequence>();
        List<CharSequence> entryValues = new ArrayList<CharSequence>();

        entries = lists[0];
        entryValues = lists[1];

        if (entries != null && entryValues != null){
            lp.setEntries(entries.toArray(new CharSequence[]{}));
            //lp.setDefaultValue("1");
            lp.setEntryValues(entryValues.toArray(new CharSequence[]{}));
        }

    }

    private void requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), android.Manifest.permission.READ_CONTACTS)) {
            // show UI part if you want here to show some rationale !!!
        } else {
            ActivityCompat.requestPermissions(getActivity(), new String[]{android.Manifest.permission.READ_CONTACTS},
                    REQUEST_READ_CONTACTS);
        }
        if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), android.Manifest.permission.READ_CONTACTS)) {
        } else {
            ActivityCompat.requestPermissions(getActivity(), new String[]{android.Manifest.permission.READ_CONTACTS},
                    REQUEST_READ_CONTACTS);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_READ_CONTACTS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    lists = getAllContacts();
                } else {
                    // permission denied,Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
        }
    }

    private ArrayList[] getAllContacts() {
        ArrayList<String> nameList = new ArrayList<>();
        ArrayList<String> phoneList = new ArrayList<>();
        String phoneNo = null;
        String phoneNo2 = "";
        ContentResolver cr = getContext().getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, "upper("+ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + ") ASC");
        if ((cur != null ? cur.getCount() : 0) > 0) {
            while (cur != null && cur.moveToNext()) {
                String id = cur.getString(
                        cur.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cur.getString(cur.getColumnIndex(
                        ContactsContract.Contacts.DISPLAY_NAME));
                if (cur.getInt(cur.getColumnIndex( ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    Cursor pCur = cr.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id}, "upper("+ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + ") ASC");
                    while (pCur.moveToNext()) {
                        phoneNo = pCur.getString(pCur.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER));

                    }


                    for (int i = 0; i < phoneNo.length(); i++){
                        char c = phoneNo.charAt(i);

                        if (Character.isDigit(c)){
                            phoneNo2 += c;
                        }
                        //Process char
                    }
                    if (phoneNo2.length() > 10){
                        int diff = phoneNo2.length()-10;
                        phoneNo2 = phoneNo2.substring(diff);
                    }
                    phoneList.add(phoneNo2);
                    phoneNo2 = "";
                    nameList.add(name);
                    pCur.close();

                }
            }
        }
        if (cur != null) {
            cur.close();
        }
        ArrayList[] lists = new ArrayList[2];
        lists[0] = nameList;
        lists[1] = phoneList;
        return lists;
    }
}
