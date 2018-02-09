package com.example.nikhil.gps;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class Login extends AppCompatActivity {

    EditText email, mobile;
    Button login;
    ProgressDialog pDialog;
    GPSTracker gps;
    String address;
    int check = 0;
    LocationManager manager;
    String countryName;
    private Context context;
    private Activity activity;
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static GoogleApiClient mGoogleApiClient;
    private View view;
    private static final int REQUEST_CHECK_SETTINGS = 0x1;
    private static final String BROADCAST_ACTION = "android.location.PROVIDERS_CHANGED";
    SharedPreferences.Editor editor;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editor = getSharedPreferences("KTM", MODE_PRIVATE).edit();
        prefs = getSharedPreferences("KTM", MODE_PRIVATE);

        context = getApplicationContext();
        activity = this;
        manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        gps = new GPSTracker(getApplicationContext());
        initGoogleAPIClient();
        email = (EditText) findViewById(R.id.email);
        mobile = (EditText) findViewById(R.id.mobile);
        login = (Button) findViewById(R.id.login);

        email.setText(prefs.getString("username", null));
        mobile.setText(prefs.getString("password", null));

        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (email.getText().toString().trim().matches("")) {
                    Toast.makeText(getApplicationContext(), "Please enter your username", Toast.LENGTH_SHORT).show();
                } else if (mobile.getText().toString().trim().matches("")) {
                    Toast.makeText(getApplicationContext(), "Please enter your mobile number", Toast.LENGTH_SHORT).show();
                } else {
                    ConnectivityManager conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo netInfo = conMgr.getActiveNetworkInfo();
                    if (netInfo != null) {
                        new JSONtask().execute("https://bajajktmpbdss.gladminds.co/service/apis/dss/newservices/mobile_app/android/login.php?format=json&email=");
                    } else {
                        Toast.makeText(getApplication(), "Connect to the internet", Toast.LENGTH_LONG).show();

                    }
                }
            }
        });


    }


    private void showSettingDialog() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);//Setting priotity of Location request to high
        locationRequest.setInterval(30 * 1000);
        locationRequest.setFastestInterval(5 * 1000);//5 sec Time interval for location update
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setAlwaysShow(true); //this is the key ingredient to show dialog always when GPS is off

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                final LocationSettingsStates state = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can initialize location
                        // requests here.

                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(Login.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            e.printStackTrace();
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                        break;
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case RESULT_OK:
                        Log.e("Settings", "Result OK");

                        //startLocationUpdates();
                        break;
                    case RESULT_CANCELED:
                        Log.e("Settings", "Result Cancel");
                        finish();
                        //updateGPSStatus("GPS is Disabled in your device");
                        break;
                }
                break;
        }
    }

    private Runnable sendUpdatesToUI = new Runnable() {
        public void run() {
            showSettingDialog();
        }
    };

    /* Broadcast receiver to check status of GPS */
    private BroadcastReceiver gpsLocationReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            //If Action is Location
            if (intent.getAction().matches(BROADCAST_ACTION)) {
                LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                //Check if GPS is turned ON or OFF
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {


                } else {
                    //If GPS turned OFF show Location Dialog
                    new Handler().postDelayed(sendUpdatesToUI, 10);
                    // showSettingDialog();

                    // Toast.makeText(context,"Permission Denied, You cannot access location data.",Toast.LENGTH_LONG).show();
                }

            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(gpsLocationReceiver, new IntentFilter(BROADCAST_ACTION));//Register broadcast receiver to check the status of GPS

        showSettingDialog();
    }

    private void initGoogleAPIClient() {
        //Without Google API Client Auto Location Dialog will not work
        mGoogleApiClient = new GoogleApiClient.Builder(Login.this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }


    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
        if (result == PackageManager.PERMISSION_GRANTED) {


            return true;

        } else {


            return false;

        }
    }

    private void requestPermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {

            // Toast.makeText(context,"GPS permission allows us to access location data. Please allow in App Settings for additional functionality.",Toast.LENGTH_LONG).show();

        } else {

            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Toast.makeText(context, "Permission Granted, Now you can access location data.", Toast.LENGTH_LONG).show();

                } else {

                    Toast.makeText(context, "Permission Denied, You cannot access location data.", Toast.LENGTH_LONG).show();

                }
                break;
        }
    }

    private class JSONtask extends AsyncTask<String, String, JSONObject> {

        //public JSONObject list = new JSONObject();
        JSONObject jsonObj;
        String tag_string_req = "req_login";
        //List list=new ArrayList();
        String em, mob;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog.setMessage("Logging in...");
            showDialog();
            em = email.getText().toString().trim();
            mob = mobile.getText().toString().trim();

            editor.putString("username", em);
            editor.putString("password", mob);
            editor.commit();
        }

        @Override
        protected JSONObject doInBackground(String... params) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            try {

                URL url = new URL(params[0] + em + "&mobile_no=" + mob);

                connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("GET");
                //connection.setRequestProperty("Accept", "application/json");
                //connection.setRequestProperty("Content-type", "application/x-www-form-urlencoded");

                connection.connect();

                int code = connection.getResponseCode();
                //list.add(String.valueOf(code));

                if (code == 200) {
                    hideDialog();
                } else {
                    hideDialog();
                    Log.e("response code", String.valueOf(code));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Something went wrong. Please try after sometime.", Toast.LENGTH_SHORT).show();
                        }
                    });

                    //Toast.makeText(getApplicationContext(), "Enter correct credentials", Toast.LENGTH_LONG).show();
                }

                InputStream stream = connection.getInputStream();

                reader = new BufferedReader(new InputStreamReader(stream));

                StringBuffer buffer = new StringBuffer();

                String line = "";
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }

                jsonObj = new JSONObject(buffer.toString());
                //String token = jsonObj.getString("data");

                //list.add(token);
                //list.add(jsonObj.getString("status"));

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            return jsonObj;
        }

        @Override
        protected void onPostExecute(JSONObject jsonObj) {
            super.onPostExecute(jsonObj);
            try {
                if (jsonObj.getString("status").matches("sucess")) {
                    JSONObject j = new JSONObject();
                    j = jsonObj.getJSONObject("data");
                    String user_id, first_name, last_name, imagepath, last_login;
                    user_id = j.getString("user_id");
                    first_name = j.getString("first_name");
                    last_name = j.getString("last_name");
                    imagepath = j.getString("imagepath");
                    last_login = j.getString("last_login");

                    Intent intent = new Intent(getApplicationContext(), Profile.class);

                    intent.putExtra("user_id", user_id);
                    intent.putExtra("first_name", first_name);
                    intent.putExtra("last_name", last_name);
                    intent.putExtra("imagepath", imagepath);
                    intent.putExtra("last_login", last_login);

                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(getApplicationContext(), jsonObj.getString("msg"), Toast.LENGTH_SHORT).show();

                }
            } catch (Exception e) {

            }
        }

        private void showDialog() {
            if (!pDialog.isShowing())
                pDialog.show();
        }

        private void hideDialog() {
            if (pDialog.isShowing())
                pDialog.dismiss();
        }
    }


}
