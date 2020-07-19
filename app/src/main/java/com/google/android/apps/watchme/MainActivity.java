/*
 * Copyright (c) 2014 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.apps.watchme;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.provider.Settings;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.android.volley.toolbox.ImageLoader;
import com.google.android.apps.watchme.util.EventData;
import com.google.android.apps.watchme.util.NetworkSingleton;
import com.google.android.apps.watchme.util.Utils;
import com.google.android.apps.watchme.util.YouTubeApi;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.youtube.YouTube;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.view.View.NO_ID;

/**
 * @author Ibrahim Ulukaya <ulukaya@google.com>
 *         <p/>
 *         Main activity class which handles authorization and intents.
 */
public class MainActivity extends AppCompatActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String ACCOUNT_KEY = "accountName";
    public static final String APP_NAME = "watch";
    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 0;
    private static final int REQUEST_GMS_ERROR_DIALOG = 1;
    private static final int REQUEST_ACCOUNT_PICKER = 2;
    private static final int REQUEST_AUTHORIZATION = 3;
    private static final int REQUEST_STREAMER = 4;
    private static final int CODE_WRITE_SETTINGS_PERMISSION = 5;
    final HttpTransport transport = AndroidHttp.newCompatibleTransport();
    final JsonFactory jsonFactory = new GsonFactory();
    GoogleAccountCredential credential;
    private String mChosenAccountName;
    private ImageLoader mImageLoader;
    String[] PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO,
            Manifest.permission.GET_ACCOUNTS, Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.SEND_SMS, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_SETTINGS};
    int PERMISSION_ALL = 1;

    LocationManager locationManager;
    private String longi = "";
    private String lati = "";
    private String coords = "";

    private ListView mListView;
    private TextView mTextView;
    private MaterialButtonToggleGroup mToggleGroup;

    Button button1;
    Button button2;
    Button button3;

    String[] maintitle ={
            "Allow Permissions","Sign Into Google",
            "Enable Youtube Live Streaming","Select Emergency Contacts",
            "Test Run"
    };

    String[] subtitle ={
            "Allow permissions on installation, or go to your Settings app to allow them",
            "Choose your Google Account on installation, or change using the Accounts tab at the top-right",
            "Enable live streaming in your chosen account from the website or app (there will be a 24 hour delay to confirm)",
            "Select Two Emergency Contacts to notify when you're stopped",
            "Press the button to conduct a test run to ensure the whole process works for you and your contacts"
    };

    String[] maintitle2 ={
            "Right to Remain Silent","You do NOT have to consent to a Search",
            "You can ask if you're free to leave","You do NOT have to answer questions on your origin",
            "You have the right to a government-appointed lawyer"
    };

    String[] subtitle2 ={
            "Both driver and passengers have the right to remain silent. If you intend to exercise this right, say so outloud.",
            "Officers may not search your vehicle without consent, but they may pat you down if suspecting a weapon. Note: an officer may illegally force a search, in which case, clearly document your objection for later legal purposes",
            "If you're a passenger, you may ask if you're free to leave. If the answer is yes, silently leave.",
            "This applies to questions about where you were born, whether you are a U.S. citizen, or how you entered the country",
            "If you cannot afford a lawyer, you can legally be provided one"
    };

    String[] maintitle3 ={
            "Pull Over in a safe place",
            "Car off, Window Half-way Open, Hands on the Wheel",
            "Have license, registration, proof of insurance ready",
            "Stay Calm",
            "Avoid Sudden Movements"
    };

    String[] subtitle3 ={
            "Do so as quickly as possible",
            "If you're in the passenger seat, hands on the dashboard",
            "Only procure when asked",
            "Don't take anything the officer says personally, even if provoked",
            "Notify the officer whenever you reach for something"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

        credential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(Utils.SCOPES));
        // set exponential backoff policy
        credential.setBackOff(new ExponentialBackOff());

        if (savedInstanceState != null) {
            mChosenAccountName = savedInstanceState.getString(ACCOUNT_KEY);
        } else {
            loadAccount();
        }

        credential.setSelectedAccountName(mChosenAccountName);

        if (mChosenAccountName == null){
            chooseAccount();
        }

        setupSharedPreferences();

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.mipmap.newic_launcher);

        mTextView = findViewById(R.id.text_view);

        mTextView.setText("Start Guide");

        mListView = findViewById(R.id.list_view);

        MyListAdapter adapter = new MyListAdapter(this, maintitle, subtitle);
        mListView.setAdapter(adapter);

        button1 = findViewById(R.id.button1);
        button2 = findViewById(R.id.button2);
        button3 = findViewById(R.id.button3);
    }

    public void button1(View view){
        mTextView.setText("Start Guide");
        MyListAdapter adapter = new MyListAdapter(this, maintitle, subtitle);
        mListView.setAdapter(adapter);
    }

    public void button2(View view){
        mTextView.setText("Know Your Rights");
        mTextView.setTextSize(40);
        MyListAdapter adapter = new MyListAdapter(this, maintitle2, subtitle2);
        mListView.setAdapter(adapter);
    }

    public void button3(View view){
        mTextView.setText("Pulled Over?");
        MyListAdapter adapter = new MyListAdapter(this, maintitle3, subtitle3);
        mListView.setAdapter(adapter);
    }

    private void setupSharedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("select_contact")) {
            Log.e("Contact---", "Sending to " + sharedPreferences.getString("select_contact", "DEFAULT"));
        }
        if (key.equals("enable_dim")){
            if (sharedPreferences.getBoolean("enable_dim", false)){
                youDesirePermissionCode(this);
            }
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    public void startStreaming(EventData event) {


        String broadcastId = event.getId();
        Log.e("MAinactivity", broadcastId);
        new StartEventTask().execute(broadcastId);

        Intent intent = new Intent(getApplicationContext(),
                YouTubeStreamActivity.class);
        intent.putExtra(YouTubeApi.RTMP_URL_KEY, event.getIngestionAddress());
        intent.putExtra(YouTubeApi.BROADCAST_ID_KEY, broadcastId);
        Log.e("Stream---", broadcastId  +"  "+event.getIngestionAddress());

        startActivityForResult(intent, REQUEST_STREAMER);

    }

    private void getLiveEvents() {
        if (mChosenAccountName == null) {
            return;
        }
        new GetLiveEventsTask().execute();
    }

    public void createEvent(View view) {
        new CreateLiveEventTask(this).execute();
    }


    private void loadAccount() {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(this);
        mChosenAccountName = sp.getString(ACCOUNT_KEY, null);
        invalidateOptionsMenu();
    }

    private void saveAccount() {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(this);
        sp.edit().putString(ACCOUNT_KEY, mChosenAccountName).apply();
    }

    private void loadData() {
        if (mChosenAccountName == null) {
            return;
        }
        getLiveEvents();
    }

    private void openContacts(){
        Intent myIntent = new Intent(this, ContactList.class);
        startActivityForResult(myIntent, 0);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return super.onCreateOptionsMenu(menu);



        /*
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main, menu);
        return true;
         */
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_accounts:
                chooseAccount();
                return true;
            case R.id.emergency_contacts:
                openContacts();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void youDesirePermissionCode(Activity context){
        boolean permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permission = Settings.System.canWrite(context);
        } else {
            permission = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_SETTINGS) == PackageManager.PERMISSION_GRANTED;
        }
        if (permission) {

        }  else {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                context.startActivityForResult(intent, MainActivity.CODE_WRITE_SETTINGS_PERMISSION);
            } else {
                ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.WRITE_SETTINGS}, MainActivity.CODE_WRITE_SETTINGS_PERMISSION);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_GMS_ERROR_DIALOG:
                break;
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode == Activity.RESULT_OK) {
                    haveGooglePlayServices();
                } else {
                    checkGooglePlayServicesAvailable();
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode != Activity.RESULT_OK) {
                    chooseAccount();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == Activity.RESULT_OK && data != null
                        && data.getExtras() != null) {
                    String accountName = data.getExtras().getString(
                            AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        mChosenAccountName = accountName;
                        credential.setSelectedAccountName(accountName);
                        saveAccount();
                    }
                }
                break;
            case REQUEST_STREAMER:
                if (resultCode == Activity.RESULT_OK && data != null
                        && data.getExtras() != null) {
                    String broadcastId = data.getStringExtra(YouTubeApi.BROADCAST_ID_KEY);
                    if (broadcastId != null) {
                        new EndEventTask().execute(broadcastId);
                    }
                }
                break;
            case CODE_WRITE_SETTINGS_PERMISSION:
                if(Settings.System.canWrite(this))
                {
                Log.d("TAG", "MainActivity.CODE_WRITE_SETTINGS_PERMISSION success");
                //do your code
                }
                break;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ACCOUNT_KEY, mChosenAccountName);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    public void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        runOnUiThread(new Runnable() {
            public void run() {
                Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
                        connectionStatusCode, MainActivity.this,
                        REQUEST_GOOGLE_PLAY_SERVICES);
                dialog.show();
            }
        });
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     */
    private boolean checkGooglePlayServicesAvailable() {
        final int connectionStatusCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
            return false;
        }
        return true;
    }

    private void haveGooglePlayServices() {
        // check if there is already an account selected
        if (credential.getSelectedAccountName() == null) {
            // ask user to choose account
            chooseAccount();
        }
    }

    private void chooseAccount() {
        startActivityForResult(credential.newChooseAccountIntent(),
                REQUEST_ACCOUNT_PICKER);
    }

    @Override
    public void onBackPressed() {
    }


    public Location getLocation() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            Location lastKnownLocationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnownLocationGPS != null) {
                return lastKnownLocationGPS;
            } else {
                Location loc =  locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                System.out.println("1::"+loc);
                System.out.println("2::"+loc.getLatitude());
                return loc;
            }
        } else {
            return null;
        }
    }


    public void CheckPermission() {

    }


    private class GetLiveEventsTask extends
            AsyncTask<Void, Void, List<EventData>> {
        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(MainActivity.this, null,
                    getResources().getText(R.string.loadingEvents), true);
        }

        @Override
        protected List<EventData> doInBackground(
                Void... params) {
            YouTube youtube = new YouTube.Builder(transport, jsonFactory,
                    credential).setApplicationName(APP_NAME)
                    .build();
            try {
                return YouTubeApi.getLiveEvents(youtube);
            } catch (UserRecoverableAuthIOException e) {
                startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
            } catch (IOException e) {
                Log.e(MainActivity.APP_NAME, "", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(
                List<EventData> fetchedEvents) {
            if (fetchedEvents == null) {
                progressDialog.dismiss();
                return;
            }

            //mEventsListFragment.setEvents(fetchedEvents);
            progressDialog.dismiss();
        }
    }

    private class CreateLiveEventTask extends
            AsyncTask<Void, Void, List<EventData>> {
        private ProgressDialog progressDialog;

        private WeakReference<Context> contextRef;

        public CreateLiveEventTask(Context context) {
            contextRef = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(MainActivity.this, null,
                    getResources().getText(R.string.creatingEvent), true);
        }

        @Override
        protected List<EventData> doInBackground(
                Void... params) {
            YouTube youtube = new YouTube.Builder(transport, jsonFactory,
                    credential).setApplicationName(APP_NAME)
                    .build();
            try {
                String date = new Date().toString();
                YouTubeApi.createLiveEvent(youtube, "Event - " + date,
                        "A live streaming event - " + date);
                return YouTubeApi.getLiveEvents(youtube);

            } catch (UserRecoverableAuthIOException e) {
                startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
            } catch (IOException e) {
                Log.e(MainActivity.APP_NAME, "", e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(
                List<EventData> fetchedEvents) {

            Button buttonCreateEvent = (Button) findViewById(R.id.create_button);
            buttonCreateEvent.setEnabled(true);

            Log.e(MainActivity.APP_NAME, fetchedEvents.get(fetchedEvents.size()-1).getIngestionAddress());
            Log.e(MainActivity.APP_NAME, fetchedEvents.get(fetchedEvents.size()-1).getWatchUri());

            String watchUrl = fetchedEvents.get(fetchedEvents.size()-1).getWatchUri();
            startStreaming(fetchedEvents.get(fetchedEvents.size()-1));

            Context context = contextRef.get();

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

            boolean dim = preferences.getBoolean("enable_dim", false);

            Log.e("Dim--", String.valueOf(dim));

            if (dim == true){
                Settings.System.putInt(context.getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS, 0);
            }

            Location place = getLocation();
            double latitude = place.getLatitude();
            double longitude = place.getLongitude();

            Geocoder geocoder;
            List<Address> addresses;
            geocoder = new Geocoder(context, Locale.getDefault());
            String strAdd = null;

            try {
                addresses = geocoder.getFromLocation(latitude, longitude, 1);

                if (addresses != null) {
                    Address returnedAddress = addresses.get(0);
                    StringBuilder strReturnedAddress = new StringBuilder("");

                    strReturnedAddress.append(addresses.get(0).getAddressLine(0));

                    strAdd = strReturnedAddress.toString();
                    Log.e("Address---", strReturnedAddress.toString());
                } else {
                    Log.e("Address---", "No Address returned!");
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("Address--", "Can't get Address!");
            }

            Log.e("SENDING", "Sending Message RIGHT NOW from " + String.valueOf(latitude) + " " + String.valueOf(longitude));

            if (strAdd != null){
                Log.e("SENDING", "Sending Message RIGHT NOW from " + strAdd);
            }

            Log.e("SENDING", "Sending Message RIGHT NOW to " + preferences.getString("select_contact", "DEFAULT"));
            String phone = preferences.getString("select_contact", "DEFAULT");

            if (phone != null && strAdd != null){
                SmsManager smgr = SmsManager.getDefault();
                Log.e("SENDING", "Sending Message RIGHT NOW to " + phone + " from " + strAdd);
                smgr.sendTextMessage(phone,null, "I have been pulled over at " + strAdd + ". Watch live at " + watchUrl,null,null);
            } else if (phone != null){
                SmsManager smgr = SmsManager.getDefault();
                Log.e("SENDING", "Sending Message RIGHT NOW to " + phone);
                smgr.sendTextMessage(phone,null, "I have been pulled over. Watch live at " + watchUrl ,null,null);
            }

            progressDialog.dismiss();
        }
    }

    private class StartEventTask extends AsyncTask<String, Void, Void> {
        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(MainActivity.this, null,
                    getResources().getText(R.string.startingEvent), true);
        }

        @Override
        protected Void doInBackground(String... params) {
            YouTube youtube = new YouTube.Builder(transport, jsonFactory,
                    credential).setApplicationName(APP_NAME)
                    .build();
            try {
                YouTubeApi.startEvent(youtube, params[0]);
            } catch (UserRecoverableAuthIOException e) {
                startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
            } catch (IOException e) {
                Log.e(MainActivity.APP_NAME, "", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
            progressDialog.dismiss();
        }

    }

    private class EndEventTask extends AsyncTask<String, Void, Void> {
        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(MainActivity.this, null,
                    getResources().getText(R.string.endingEvent), true);
        }

        @Override
        protected Void doInBackground(String... params) {
            YouTube youtube = new YouTube.Builder(transport, jsonFactory,
                    credential).setApplicationName(APP_NAME)
                    .build();
            try {
                if (params.length >= 1) {
                    YouTubeApi.endEvent(youtube, params[0]);
                }
            } catch (UserRecoverableAuthIOException e) {
                startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
            } catch (IOException e) {
                Log.e(MainActivity.APP_NAME, "", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
            progressDialog.dismiss();
        }
    }
}