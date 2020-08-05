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
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import android.provider.MediaStore;
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
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
            Manifest.permission.WRITE_SETTINGS, Manifest.permission.SYSTEM_ALERT_WINDOW};
    private static final int PERMISSION_ALL = 1;

    LocationManager locationManager;
    private String longi = "";
    private String lati = "";
    private String coords = "";

    private ListView mListView;
    private TextView mTextView;
    private MaterialButtonToggleGroup mToggleGroup;

    private Switch mSwitch;

    public static final String MyPREFERENCES = "nightModePrefs";
    public static final String KEY_ISNIGHTMODE="isNightMode";
    SharedPreferences displayPreferences;

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
            "Enable live streaming in your chosen account from the Youtube website or app (there will be a 24 hour delay to confirm)",
            "Select Up to Three Emergency Contacts to notify when you're stopped through the Settings tab at the top-right",
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

        // ----- DARK MODE IMPLEMENTATION -----
        displayPreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);

        mTextView = findViewById(R.id.text_view);
        mListView = findViewById(R.id.list_view);
        mSwitch = findViewById(R.id.switch1);

        checkNightModeActivated();

        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    saveNightModeState(true);
                    recreate();
                }else{
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    saveNightModeState(false);
                    recreate();
                }
            }
        });

        setupSharedPreferences();

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

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.mipmap.newic_launcher);

        mTextView = findViewById(R.id.text_view);

        mListView = findViewById(R.id.list_view);
        mToggleGroup = findViewById(R.id.toggleGroup);

        setUp();

        button1 = findViewById(R.id.button1);
        button2 = findViewById(R.id.button2);
        button3 = findViewById(R.id.button3);

        loadData();

        addNotification();
    }

    private void saveNightModeState(boolean nightMode) {

        SharedPreferences.Editor editor = displayPreferences.edit();
        editor.putBoolean(KEY_ISNIGHTMODE, nightMode);
        editor.apply();

    }

    public void checkNightModeActivated(){

        if(displayPreferences.getBoolean(KEY_ISNIGHTMODE, false)) {
            mSwitch.setChecked(true);
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }else{
            mSwitch.setChecked(false);
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

    }

    public void setUp(){
        SharedPreferences preferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this);
        boolean[] checks = {
                preferences.getBoolean("permissions", false),
                preferences.getBoolean("signin", false),
                preferences.getBoolean("enable", false),
                preferences.getBoolean("contacts", false),
                preferences.getBoolean("test", false)
        };
        boolean done = true;
        for (int i = 0; i < checks.length; i++){
            if (checks[i] == false){
                done = false;
            }
        }
        if (done){
            mToggleGroup.check(R.id.button2);
            mTextView.setText("Know Your Rights");
            mTextView.setTextSize(40);
            MyListAdapter adapter = new MyListAdapter(this, maintitle2, subtitle2);
            mListView.setAdapter(adapter);
        } else {
            mTextView.setText("Set-Up");

            SetupAdapter adapter = new SetupAdapter(this, maintitle, subtitle, checks);
            mListView.setAdapter(adapter);
        }

    }



    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_ALL: {
                boolean granted = true;
                for (int i = 0; i < (grantResults.length - 2); i++){
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED){
                        granted = false;
                    }
                }

                if (!granted){
                    new MaterialAlertDialogBuilder(MainActivity.this)
                            .setTitle("Missing Required Permissions")
                            .setMessage("Permissions are required to use the app")
                            .setPositiveButton("Ok", /* listener = */ new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, PERMISSION_ALL);
                                }
                            })
                            .setCancelable(false)
                            .show();
                } else {
                    SharedPreferences preferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this);
                    preferences.edit().putBoolean("permissions", true).apply();
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }



    private void addNotification() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("n", "n", NotificationManager.IMPORTANCE_HIGH);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, "n")
                        .setSmallIcon(R.drawable.newic_launcher_foreground) //set icon for notification
                        .setContentTitle("Pulled Over is running") //set title of notification
                        .setContentText("Click here to access quickly")//this is notification message
                        .setAutoCancel(false)
                        .setOngoing(true)// makes auto cancel of notification
                        .setPriority(NotificationCompat.PRIORITY_MAX); //set priority of notification


        Intent notificationIntent = new Intent(this, MainActivity.class);
        Log.e("Noti--", "Notification");
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        //notification message will get at NotificationView
        notificationIntent.putExtra("message", "This is a notification message");

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);


        // Add as notification
        NotificationManagerCompat manager = NotificationManagerCompat.from(this);
        manager.notify(999, builder.build());


    }

    public void button1(View view){
        mTextView.setText("Set-Up");
        SharedPreferences preferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this);

        boolean[] checks = {
                preferences.getBoolean("permissions", false),
                preferences.getBoolean("signin", false),
                preferences.getBoolean("enable", false),
                preferences.getBoolean("contacts", false),
                preferences.getBoolean("test", false)
        };

        SetupAdapter adapter = new SetupAdapter(this, maintitle, subtitle, checks);
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

        boolean shouldInsertData = sharedPreferences.getBoolean("shouldInsertData", true);

        if(shouldInsertData){

            //insert your data into the preferences
            sharedPreferences.edit().putBoolean("permissions", false).apply();
            sharedPreferences.edit().putBoolean("signin", false).apply();
            sharedPreferences.edit().putBoolean("enable", false).apply();
            sharedPreferences.edit().putBoolean("contacts", false).apply();
            sharedPreferences.edit().putBoolean("test", false).apply();

            sharedPreferences.edit().putBoolean("shouldInsertData", false).apply();

        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("select_contact")) {
            Set<String> selections = sharedPreferences.getStringSet("select_contact", null);
            String[] selected = selections.toArray(new String[] {});
            for (int i = 0; i < selected.length; i++){
                Log.e("Contact---", "Sending to " + selected[i]);
            }
        }
        if (key.equals("enable_dim")){
            if (sharedPreferences.getBoolean("enable_dim", false)){
                youDesirePermissionCode(this);
            }
        }
        if (key.equals("permissions")){
            if (sharedPreferences.getBoolean("permissions", false)){
                setUp();
            }
        }
        if (key.equals("signin")){
            if (sharedPreferences.getBoolean("permissions", false)){
                setUp();
            }
        }
        if (key.equals("enable")){
            if (sharedPreferences.getBoolean("enable", false)){
                setUp();
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

        if (event != null){
            SharedPreferences preferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this);
            boolean test = preferences.getBoolean("test", false);
            if (!test){
                preferences.edit().putBoolean("test", true).apply();
            }
            String broadcastId = event.getId();
            Log.e("Mainactivity", broadcastId);
            new StartEventTask().execute(broadcastId);

            Intent intent = new Intent(getApplicationContext(),
                    YouTubeStreamActivity.class);
            intent.putExtra(YouTubeApi.RTMP_URL_KEY, event.getIngestionAddress());
            intent.putExtra(YouTubeApi.BROADCAST_ID_KEY, broadcastId);
            Log.e("Stream---", broadcastId  +"  "+event.getIngestionAddress());

            startActivityForResult(intent, REQUEST_STREAMER);
        }

    }

    private void getLiveEvents() {
        if (mChosenAccountName == null) {
            return;
        }
        new GetLiveEventsTask().execute();
    }

    public void createEvent(View view) {
        SharedPreferences preferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this);
        boolean[] checks = {
                preferences.getBoolean("permissions", false),
                preferences.getBoolean("signin", false),
                preferences.getBoolean("enable", false),
                preferences.getBoolean("contacts", false)
        };

        for (int i = 0; i < checks.length; i++){
            if (checks[i] == false){
                new MaterialAlertDialogBuilder(MainActivity.this)
                        .setTitle("Missing Set-up")
                        .setMessage("Do the first 4 set-up tasks before streaming")
                        .setPositiveButton("Ok", /* listener = */ new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                            }
                        })
                        .show();
                return;
            }
        }

        ConnectivityManager cm =
                (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        if (isConnected != true){
            Toast.makeText(getBaseContext(), "No Internet Connection",
                    Toast.LENGTH_LONG).show();
        }
        new CreateLiveEventTask(this).execute(isConnected);
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
        Log.e("Loading---", "OnCreateView");
        ConnectivityManager cm =
                (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        if (isConnected != true){
            new MaterialAlertDialogBuilder(MainActivity.this)
                    .setTitle("No Internet Connection")
                    .setMessage("Functions will be limited")
                    .setPositiveButton("Ok", /* listener = */ new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                        }
                    })
                    .show();
            return;
        }
        getLiveEvents();
    }

    private void openSettings(){
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
            case R.id.settings:
                openSettings();
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
                        loadData();
                    }
                }
                if (mChosenAccountName == null){
                    chooseAccount();
                } else {
                    SharedPreferences preferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this);
                    preferences.edit().putBoolean("signin", true).apply();
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
                Log.e("TAG", "MainActivity.CODE_WRITE_SETTINGS_PERMISSION success");
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
                List<EventData> events = YouTubeApi.getLiveEvents(youtube);
                SharedPreferences preferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                preferences.edit().putBoolean("enable", true).apply();
                return events;
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
            AsyncTask<Boolean, Void, List<EventData>> {
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
                Boolean... params) {
            YouTube youtube = new YouTube.Builder(transport, jsonFactory,
                    credential).setApplicationName(APP_NAME)
                    .build();
            if (params[0] == true){
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
            }
            return null;
        }

        @Override
        protected void onPostExecute(
                List<EventData> fetchedEvents) {
            boolean internet = false;
            String watchUrl = null;
            if (fetchedEvents != null){
                internet = true;
            }

            Button buttonCreateEvent = (Button) findViewById(R.id.create_button);
            buttonCreateEvent.setEnabled(true);

            if (internet == true && fetchedEvents != null && fetchedEvents.get(fetchedEvents.size()-1).getIngestionAddress() != null){
                Log.e(MainActivity.APP_NAME, fetchedEvents.get(fetchedEvents.size()-1).getIngestionAddress());
                Log.e(MainActivity.APP_NAME, fetchedEvents.get(fetchedEvents.size()-1).getWatchUri());

                watchUrl = fetchedEvents.get(fetchedEvents.size()-1).getWatchUri();
                startStreaming(fetchedEvents.get(fetchedEvents.size()-1));
            } else {
                new MaterialAlertDialogBuilder(MainActivity.this)
                        .setTitle("No Internet Connection")
                        .setMessage("Sending SMS messages now. Open Camera to record.")
                        .setPositiveButton("Open Camera", /* listener = */ new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                String recPath = Environment.getExternalStorageDirectory().getPath() + "/stop"+Math.floor(Math.random() * 10000000)+".mp4";
                                Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                                intent.putExtra(MediaStore.EXTRA_OUTPUT, recPath);
                                startActivity(intent);
                            }
                        })
                        .setNegativeButton("Cancel", /* listener = */ new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                                dialog.cancel();
                            }
                        })
                        .show();
            }


            Context context = contextRef.get();

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

            boolean dim = preferences.getBoolean("enable_dim", false);

            Log.e("Dim--", String.valueOf(dim));

            if (dim == true){
                Settings.System.putInt(context.getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS, 0);
            }

            Location place = null;
            double latitude = Double.NaN;
            double longitude = Double.NaN;

            if (internet == true){
                place = getLocation();
                latitude = place.getLatitude();
                longitude = place.getLongitude();
            }

            Geocoder geocoder = null;
            List<Address> addresses;
            if (internet == true){
                geocoder = new Geocoder(context, Locale.getDefault());
            }

            String strAdd = null;

            if (internet == true){
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
            }

            if (latitude != Double.NaN){
                Log.e("SENDING", "Sending Message RIGHT NOW from " + latitude + " " + longitude);
            }


            if (strAdd != null){
                Log.e("SENDING", "Sending Message RIGHT NOW from " + strAdd);
            }

            Set<String> selections = preferences.getStringSet("select_contact", null);
            String[] selected = selections.toArray(new String[] {});

            String custom = preferences.getString("custom_text", "DEFAULT");

            if (selected != null && strAdd != null && watchUrl != null){
                String message = "I have been pulled over at " + strAdd + ". \nWatch live at " + watchUrl + "\n";
                ArrayList<String> parts = new ArrayList<>();
                parts.add(message);
                parts.add(custom);
                for (int i = 0; i < selected.length; i++){
                    Log.e("SENDING", "Sending Message RIGHT NOW to " + selected[i] + " from " + strAdd);
                    Log.e("CUSTOM", custom);
                    sendSMS(selected[i], parts);
                }
            } else if (selected != null && watchUrl != null){
                String message = "I have been pulled over. \nWatch live at " + watchUrl + "\n";
                ArrayList<String> parts = new ArrayList<>();
                parts.add(message);
                parts.add(custom);
                for (int i = 0; i < selected.length; i++){
                    Log.e("SENDING", "Sending Message RIGHT NOW to " + selected[i]);
                    Log.e("CUSTOM", custom);
                    sendSMS(selected[i], parts);
                }
            } else if (selected != null) {
                String message = "I have been pulled over.";
                ArrayList<String> parts = new ArrayList<>();
                parts.add(message);
                parts.add(custom);
                for (int i = 0; i < selected.length; i++){
                    Log.e("SENDING", "Sending Message RIGHT NOW to " + selected[i]);
                    Log.e("CUSTOM", custom);
                    sendSMS(selected[i], parts);
                }
            }

            progressDialog.dismiss();
        }
    }

    private void sendSMS(String phoneNumber, ArrayList<String> message)
    {
        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";

        ArrayList<PendingIntent> deliveryIntents = new ArrayList<PendingIntent>(2);
        ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>(2);

        sentIntents.add(PendingIntent.getBroadcast(this, 0,
                new Intent(SENT), 0));
        sentIntents.add(PendingIntent.getBroadcast(this, 0,
                new Intent(SENT), 0));

        deliveryIntents.add(PendingIntent.getBroadcast(this, 0,
                new Intent(DELIVERED), 0));
        deliveryIntents.add(PendingIntent.getBroadcast(this, 0,
                new Intent(DELIVERED), 0));

        //---when the SMS has been sent---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "SMS sent",
                                Toast.LENGTH_SHORT).show();
                        unregisterReceiver(this);
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(getBaseContext(), "Generic failure",
                                Toast.LENGTH_SHORT).show();
                        unregisterReceiver(this);
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(getBaseContext(), "No service",
                                Toast.LENGTH_SHORT).show();
                        unregisterReceiver(this);
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(getBaseContext(), "Null PDU",
                                Toast.LENGTH_SHORT).show();
                        unregisterReceiver(this);
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(getBaseContext(), "Radio off",
                                Toast.LENGTH_SHORT).show();
                        unregisterReceiver(this);
                        break;
                }
            }
        }, new IntentFilter(SENT));

        //---when the SMS has been delivered---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "SMS delivered",
                                Toast.LENGTH_SHORT).show();
                        unregisterReceiver(this);
                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(getBaseContext(), "SMS not delivered",
                                Toast.LENGTH_SHORT).show();
                        unregisterReceiver(this);
                        break;
                }
            }
        }, new IntentFilter(DELIVERED));

        SmsManager sms = SmsManager.getDefault();
        sms.sendMultipartTextMessage(phoneNumber, null, message, sentIntents, deliveryIntents);
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