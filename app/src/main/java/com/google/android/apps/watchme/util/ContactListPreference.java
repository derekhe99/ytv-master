package com.google.android.apps.watchme.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.support.v14.preference.MultiSelectListPreference;
import android.support.v4.app.ActivityCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class ContactListPreference extends MultiSelectListPreference {

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

        entries = lists[0].subList(0,4);
        entriesValues = lists[1].subList(0,4);

        Log.e("Contacts---", entriesValues.get(0).toString());

        //setEntries(entries.toArray(new CharSequence[]{}));
        //setEntryValues(entriesValues.toArray(new CharSequence[]{}));

    }

    private ArrayList[] getAllContacts() {
        ArrayList<String> nameList = new ArrayList<>();
        ArrayList<String> phoneList = new ArrayList<>();
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
                        String phoneNo = pCur.getString(pCur.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER));
                        phoneList.add(phoneNo);
                        nameList.add(name);
                    }
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