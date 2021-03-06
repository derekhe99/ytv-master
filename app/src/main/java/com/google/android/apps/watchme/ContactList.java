package com.google.android.apps.watchme;

import androidx.appcompat.app.ActionBar;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import android.view.MenuItem;
import android.widget.ListView;

import java.util.ArrayList;

public class ContactList extends AppCompatActivity {
    public static final int REQUEST_READ_CONTACTS = 79;
    ListView list;
    ArrayList mobileArray;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);



        /*
        list = findViewById(R.id.settings);
        ArrayAdapter adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, mobileArray);
        list.setAdapter(adapter);
        */


        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

    }

    public boolean onOptionsItemSelected(MenuItem item){
        Intent myIntent = new Intent(getApplicationContext(), MainActivity.class);
        startActivityForResult(myIntent, 0);
        return true;
    }
}
