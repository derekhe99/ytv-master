package com.google.android.apps.watchme.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;

import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.core.app.ActivityCompat;
import android.util.AttributeSet;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class ContactListPreference extends ListPreference {

    public static final int REQUEST_READ_CONTACTS = 79;
    ContentResolver cr;
    Cursor cursor;

    public ContactListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        Log.e("Contacts---", "started");
        ArrayList[] lists = getAllContacts();
        Log.e("Contacts---", "gotten");

        List<CharSequence> entries = new ArrayList<CharSequence>();
        List<CharSequence> entriesValues = new ArrayList<CharSequence>();

        entries = lists[0];
        entriesValues = lists[1];

        Log.e("Contacts---", entriesValues.get(0).toString());
        Log.e("Contacts---", entriesValues.get(1).toString());
        Log.e("Contacts---", entriesValues.get(2).toString());
        Log.e("Contacts---", entriesValues.get(3).toString());
        Log.e("Contacts---", entriesValues.get(4).toString());
        Log.e("Contacts---", entriesValues.get(5).toString());

        setEntries(entries.toArray(new CharSequence[]{}));
        setEntryValues(entriesValues.toArray(new CharSequence[]{}));

    }

    private ArrayList[] getAllContacts() {
        ArrayList<String> nameList = new ArrayList<>();
        ArrayList<String> phoneList = new ArrayList<>();
        String phoneNo = null;
        String phoneNo2 = "";
        ContentResolver cr = getContext().getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);
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
                            new String[]{id}, null);
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