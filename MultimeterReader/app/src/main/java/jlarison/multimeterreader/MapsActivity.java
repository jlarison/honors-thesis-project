package jlarison.multimeterreader;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MapsActivity extends FragmentActivity implements ConnectionCallbacks, OnConnectionFailedListener, View.OnClickListener, LocationListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private Location mCurrentLocation;
    private String mLastUpdateTime;
    public Handler mapHandler;
    public Handler bluetoothHandler;
    private LocationRequest mLocationRequest;
    private String currentReading;
    private String userId;

    private static final int RC_SIGN_IN = 9001;


    @Override
    public void onConnected(Bundle bundle) {
        //mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        //startLocationUpdates();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        // Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        long pollRate = Long.parseLong(sharedPref.getString("poll_rate", "1000"));
        float minDisp = Float.parseFloat(sharedPref.getString("smallest_disp", "5"));
        int priorityCode = Integer.parseInt(sharedPref.getString("location_mode", "102"));
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(priorityCode);
        mLocationRequest.setInterval(pollRate);
        mLocationRequest.setSmallestDisplacement(minDisp);
        mLocationRequest.setFastestInterval(pollRate);
        mLocationRequest.setMaxWaitTime(pollRate);
        buildGoogleApiClient();
        currentReading = null;

        findViewById(R.id.sign_in_button).setOnClickListener(this);




        // Enable Local Datastore.
        Parse.enableLocalDatastore(this);
        Parse.initialize(this, "1y6Pbr9Xgwc1CMLZ6VaZWmiC4md6smub6GOOcbgg", "YF2wi0NJfq2siq6kx2Cqqvv4qro9rnFovWzvAFxV");
/*        ParseObject testObject = new ParseObject("TestObject");
        testObject.put("barble", "bapkins");
        testObject.saveInBackground();*/

        bluetoothHandler = new Handler() {
            @Override
            public void handleMessage(android.os.Message msg) {
                if(currentReading==null) {
                    currentReading = msg.obj.toString();
                    startLocationUpdates();
                }
                else currentReading = msg.obj.toString();
            }
        };


        /*mapHandler = new Handler(){
            @Override
            public void handleMessage(android.os.Message message){
                try {
                    android.location.Location loc = (android.location.Location) message.obj;
                    placeMarker(loc.getLatitude(), loc.getLongitude());
                }catch(Exception e){

                }
            }
        };

        Thread T = new loc(mGoogleApiClient, this);
        T.start();*/

    }

    protected synchronized void buildGoogleApiClient() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        updateUI();
    }

    private void updateUI() {
        //TODO: USE THESE TO PLACE MARKER
        Log.d("jorsh", "updating UI:" + mCurrentLocation.getLatitude() + " reading: " + currentReading);
        placeMarker(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
        //mLastUpdateTime;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (keyCode == KeyEvent.KEYCODE_MENU && event.getRepeatCount() == 0) {
            openConfig();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {

        if (key.equals("poll_rate")) {
            stopLocationUpdates();
            long pollRate = Long.parseLong(sharedPreferences.getString("poll_rate", "1000"));
            mLocationRequest.setInterval(pollRate);
            startLocationUpdates();
        }
        else if (key.equals("smallest_disp")) {
            stopLocationUpdates();
            float minDisp = Float.parseFloat(sharedPreferences.getString("smallest_disp", "5"));
            mLocationRequest.setSmallestDisplacement(minDisp);
            startLocationUpdates();
        }
        else if (key.equals("location_mode")){
            stopLocationUpdates();
            int priorityCode = Integer.parseInt(sharedPreferences.getString("location_mode", "102"));
            mLocationRequest.setPriority(priorityCode);
            startLocationUpdates();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }


    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        Marker m;
        mMap.setMyLocationEnabled(true);
        //mMap.setTrafficEnabled(true);
        //mMap.setLocationSource();
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(38.8833, -98.3500), 2.9f));
        m = mMap.addMarker(new MarkerOptions().position(new LatLng(40.552586, -105.077100)).title("Fort Collins").snippet("Intersection of Drake Rd and College Ave"));
        //m.showInfoWindow();
    }

    private void placeMarker(double lat, double longi) {
        //DateFormat df = new SimpleDateFormat("HH:mm:ss");
        //Date dateobj = new Date();
        if(currentReading != null) {
            Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            float batteryPct = level / (float)scale;

            String temperature = translateReading(currentReading);
            mMap.addMarker(new MarkerOptions().position(new LatLng(lat, longi)).title(temperature).snippet(new LatLng(lat, longi).toString() + "\n" + getLocalTimeDate()));

            if(userId != null) {
                ParseObject testObject = new ParseObject("PollutionReading");
                testObject.put("userId", userId);
                ParseGeoPoint point = new ParseGeoPoint(lat, longi);
                testObject.put("time", getLocalTimeDate());
                testObject.put("geo_point", point);
                testObject.put("temp", temperature);
                testObject.put("battery", batteryPct);
                testObject.put("polling", mLocationRequest.getInterval());
                testObject.put("displacement", mLocationRequest.getSmallestDisplacement());
                testObject.put("locMode", mLocationRequest.getPriority());
                testObject.saveInBackground();
            }
        }
    }

    public void retrieve(View view)  {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("PollutionReading");
        query.whereEqualTo("userId", userId);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, com.parse.ParseException e) {
                if (e == null) {
                    //bring up list of objects
                    clearMap();
                    for(ParseObject obj : objects) {
                        double lat = obj.getParseGeoPoint("geo_point").getLatitude();
                        double longi = obj.getParseGeoPoint("geo_point").getLongitude();
                        String timestamp = obj.getString("time");
                        String temp = obj.getString("temp");
                        mMap.addMarker(new MarkerOptions().position(new LatLng(lat, longi)).title(temp).snippet(new LatLng(lat, longi).toString() + "\n" + timestamp));
                    }
                } else {
                    //fail
                }
            }
        });
    }

    public void joshlarison(View view) {
        mMap.clear();
    }

    private void clearMap(){
        mMap.clear();
    }

    public void toggleTraffic(View view) {
        if(mMap.isTrafficEnabled()){
            mMap.setTrafficEnabled(false);
        }else{
            mMap.setTrafficEnabled(true);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                onSignInClick(v);
                break;
        }
    }

    public void onSignInClick(View view) {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        Log.d("signin", "handleSignInResult:" + result.isSuccess());
        if(result.isSuccess()) {
            GoogleSignInAccount acct = result.getSignInAccount();
            Toast toast = Toast.makeText(this.getApplicationContext(), "Successfully signed in to " + acct.getEmail(), Toast.LENGTH_LONG);
            toast.show();
            this.userId = acct.getId();
        }
        else {
            Toast toast = Toast.makeText(this.getApplicationContext(), "Login Failed", Toast.LENGTH_LONG);
            toast.show();
        }
    }



    public void createBluetoothConnection(View view) {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        BluetoothDevice multimeter = null;
        for(BluetoothDevice device : pairedDevices) {
            if(device.getName().equals("TP9605")) {
                multimeter = device;
            }
        }

        if(multimeter == null) {
            displayConnectionErrorToast();
            return;
        }

        mBluetoothAdapter.getRemoteDevice(multimeter.getAddress());
        BluetoothSocket multimeterSocket = null;
        try {
            multimeterSocket = multimeter.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));
            multimeterSocket.connect();
        } catch (IOException e) {
            displayConnectionErrorToast();
            return;
        }


        //TextView text = (TextView)findViewById(R.id.textView);
        //text.setText("Connected: " + Boolean.toString(multimeterSocket.isConnected()));
        displayConnectionSuccessToast();

        Reader reader = new Reader(multimeterSocket,getApplicationContext(),this);
        reader.start();

    }

    private void displayConnectionErrorToast() {
        Toast toast = Toast.makeText(this.getApplicationContext(), "Could not connect to Bluetooth Device", Toast.LENGTH_LONG);
        toast.show();
    }

    private void displayConnectionSuccessToast() {
        Toast toast = Toast.makeText(this.getApplicationContext(), "Connected to device TP9605!", Toast.LENGTH_LONG);
        toast.show();
    }

    private String translateReading(String reading) {
        String translated = reading.substring(1, 3);
        translated += ".";
        translated += reading.charAt(3);
        translated += " degress";
        return translated;
    }

    private String getLocalTimeDate(){
        DateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
        Date dateobj = new Date();
        return df.format(dateobj);
    }

    public void openConfig() {
        Intent intent = new Intent(this, ConfigActivity.class);
        startActivity(intent);
    }



}

class loc extends Thread{
    GoogleApiClient apiClient;
    Location mlLastLocation;
    MapsActivity MA;

    public loc(GoogleApiClient apiClient, MapsActivity MA){
        this.apiClient = apiClient;
        this.MA = MA;
        //buildGoogleApiClient();
    }

    @Override
    public void run(){
        //buildGoogleApiClient();
        //apiClient.connect();
        while(true) {
            mlLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    apiClient);
            //mlLastLocation.getLatitude();
            //mlLastLocation.getLongitude();
            Message m = new Message();
            m.obj = mlLastLocation;
            MA.mapHandler.sendMessage(m);
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
