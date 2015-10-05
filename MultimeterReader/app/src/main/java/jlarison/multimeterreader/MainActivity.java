package jlarison.multimeterreader;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.location.LocationManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    LocationManager locationManager;
    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);

        handler = new Handler() {
            @Override
            public void handleMessage(android.os.Message msg) {
                String message = msg.obj.toString();
                TextView text = (TextView)findViewById(R.id.textView);
                text.setText(message);
            }
        };

        /*BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        List<String> deviceList = new ArrayList<String>();
        for(BluetoothDevice bt : pairedDevices)
            deviceList.add(bt.getName());


        ListView bluetoothListView = (ListView)findViewById(R.id.bluetoothList);
        BaseAdapter adapter = new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1,deviceList);
        bluetoothListView.setAdapter(adapter);*/
    }

    public void onConnectButtonClick(View v) {
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


        TextView text = (TextView)findViewById(R.id.textView);
        text.setText("Connected: " + Boolean.toString(multimeterSocket.isConnected()));

        Reader reader = new Reader(multimeterSocket,getApplicationContext(),this);
        reader.start();
    }

    private void displayConnectionErrorToast() {
        Toast toast = Toast.makeText(this.getApplicationContext(), "Could not connect to Bluetooth Device", Toast.LENGTH_LONG);
        toast.show();
    }

    public void onExitButtonClick(View v) {
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
