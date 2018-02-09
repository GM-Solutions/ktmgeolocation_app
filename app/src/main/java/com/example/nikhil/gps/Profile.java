package com.example.nikhil.gps;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
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
import android.widget.ImageView;
import android.widget.TextView;
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
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;

public class Profile extends AppCompatActivity implements OnMapReadyCallback {

    String user_id, first_name, last_name, imagepath, last_login, lat, longg;
    TextView name;
    ImageView pic;
    Button senLoc;
    ProgressDialog pDialog;
    Geocoder geocoder;
    List<Address> addresses;
    String city, country, state;
    private static final int ACCESS_FINE_LOCATION_INTENT_ID = 3;
    private GoogleMap map;
    SupportMapFragment mapFragment;
    double latitude, longitude;
    Button logout;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        context = getApplicationContext();
        activity = this;
        manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        gps = new GPSTracker(getApplicationContext());
        initGoogleAPIClient();

        //Init Google API Client
        //Check Permission

        geocoder = new Geocoder(this, Locale.getDefault());
        user_id = getIntent().getStringExtra("user_id");
        first_name = getIntent().getStringExtra("first_name");
        last_name = getIntent().getStringExtra("last_name");
        imagepath = getIntent().getStringExtra("imagepath");
        last_login = getIntent().getStringExtra("last_login");

        name = (TextView) findViewById(R.id.name);
        pic = (ImageView) findViewById(R.id.pic);
        senLoc = (Button) findViewById(R.id.sendLoc);

        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        name.setText("Name: " + first_name + " " + last_name);
        try {
            Picasso.with(this).load(imagepath).into(pic);
        } catch (Exception e) {

        }

        latitude = gps.getLatitude();
        longitude = gps.getLongitude();
        //address=  getAddress( latitude, longitude);
        mapFragment = ((SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map));
        mapFragment.getMapAsync(this);

        logout = (Button) findViewById(R.id.logout);
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), Login.class);
                startActivity(intent);
                Toast.makeText(getApplicationContext(), "Successfully Logout", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        senLoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConnectivityManager conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo netInfo = conMgr.getActiveNetworkInfo();
                if (netInfo != null) {
                    LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    boolean statusOfGPS = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                    if (statusOfGPS) {
                        double latitude = gps.getLatitude();
                        double longitude = gps.getLongitude();

                        try {
                            addresses = geocoder.getFromLocation(latitude, longitude, 1);
                            address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
                            city = addresses.get(0).getLocality();
                            state = addresses.get(0).getAdminArea();
                            country = addresses.get(0).getCountryName();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        lat = String.valueOf(latitude);
                        longg = String.valueOf(longitude);

                        Log.e("location", String.valueOf(longitude));
                        Log.e("location", String.valueOf(latitude));

                        new SendLocation().execute("https://bajajktmpbdss.gladminds.co/service/apis/dss/newservices/mobile_app/android/updatelocation.php?format=json&user_id=");

                    } else {
                        Toast.makeText(context, "GPS permission allows us to access location data. Please allow in .", Toast.LENGTH_LONG).show();

                    }
                } else {
                    Toast.makeText(getApplication(), "Connect to the internet", Toast.LENGTH_LONG).show();

                }
            }

        });


    }

    private void requestPermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {

            Toast.makeText(context, "GPS permission allows us to access location data. Please allow in App Settings for additional functionality.", Toast.LENGTH_LONG).show();

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
                            status.startResolutionForResult(Profile.this, REQUEST_CHECK_SETTINGS);
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
                    //   Toast.makeText(context,"Permission accessss, You cannot access location data.",Toast.LENGTH_LONG).show();

                } else {
                    //If GPS turned OFF show Location Dialog
                    new Handler().postDelayed(sendUpdatesToUI, 10);
                    // showSettingDialog();

                    //  Toast.makeText(context,"Permission Denied, You cannot access location data.",Toast.LENGTH_LONG).show();
                }

            }
        }
    };

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
        if (result == PackageManager.PERMISSION_GRANTED) {


            return true;

        } else {


            return false;

        }
    }

    private void initGoogleAPIClient() {
        //Without Google API Client Auto Location Dialog will not work
        mGoogleApiClient = new GoogleApiClient.Builder(Profile.this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(gpsLocationReceiver, new IntentFilter(BROADCAST_ACTION));//Register broadcast receiver to check the status of GPS

        showSettingDialog();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        LatLng latLng = new LatLng(latitude, longitude);
        map.addMarker(new MarkerOptions()
                .position(latLng)
                .draggable(false));
        // LatLng latLng = new LatLng(Double.valueOf(point._lat), Double.valueOf(point._long));
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15);
        map.animateCamera(cameraUpdate);

        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                return false;
            }
        });
        map.getUiSettings().setZoomGesturesEnabled(true);
    }

    private class SendLocation extends AsyncTask<String, String, JSONObject> {

        //public JSONObject list = new JSONObject();
        JSONObject jsonObj;
        String tag_string_req = "req_login";
        //List list=new ArrayList();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog.setMessage("Please wait...");
            showDialog();

        }

        @Override
        protected JSONObject doInBackground(String... params) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            try {
                String encodedAddress = URLEncoder.encode(address, "utf-8");
                //19625&latitude=4.6666&longitude=999885
                URL url = new URL(params[0] + user_id + "&latitude=" + lat + "&longitude=" + longg + "&address=" + encodedAddress + "&city=" + city + "&country=" + country + "&state=" + state);

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
                if (!(jsonObj.length() == 0)) {
                    if (jsonObj.getString("status").matches("sucess")) {
                        Toast.makeText(getApplicationContext(), jsonObj.getString("msg"), Toast.LENGTH_SHORT).show();
                    }else {
                        Toast.makeText(getApplicationContext(), "Something went wrong. Please try after sometime.", Toast.LENGTH_SHORT).show();

                    }
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

