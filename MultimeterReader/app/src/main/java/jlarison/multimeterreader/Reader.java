package jlarison.multimeterreader;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
//import android.location.Location;
//import android.location.LocationManager;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Created by Josh on 10/1/2015.
 */
public class Reader extends Thread{

    BluetoothSocket socket;
    //LocationManager mLocationManager;
    Context context;
    InputStream inStream;
    MapsActivity main;

    final String bluetoothTag = "MultimeterBluetooth";

    public Reader(BluetoothSocket socket, Context context, MapsActivity main) {

        this.socket = socket;
        this.context = context;
        this.main = main;

        InputStream tempInputStream = null;
        try {
            tempInputStream = socket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        inStream = tempInputStream;
    }

    public void run() {
        InputStream stream = null;


        byte[] inBuffer = new byte[14];
        try {
            stream = socket.getInputStream();
            //String locationProvider = LocationManager.NETWORK_PROVIDER;

            String permission = "android.permission.ACCESS_FINE_LOCATION";
            int res = context.checkCallingOrSelfPermission(permission);
            //mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            //Location loc = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            stream.read(inBuffer);
            while(true) {
                stream.read(inBuffer);
                if (res == PackageManager.PERMISSION_GRANTED) {
                    //Location lastKnownLocation = mLocationManager.getLastKnownLocation(locationProvider);
                    //String reading = " Reading: " + Arrays.toString(inBuffer) + "\n" + "Location: " + lastKnownLocation.getLatitude() + lastKnownLocation.getLongitude();
                    //Log.i("data", reading );
                    String reading = new String(inBuffer);
                    if(reading.toString().charAt(0) == '0') {
                        Log.i("dataString", reading.toString());
                        Message msg = new Message();
                        msg.obj = reading;
                        main.bluetoothHandler.sendMessage(msg);
                    }
                }

                //Log.i("data",Integer.toString(stream.read()));
                Thread.sleep(100);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }



    /*
        public void run() {
            Log.i(bluetoothTag, "BEGIN mConnectedThread");
            String strOutput = "";
            byte[] byData = new byte[1024];
            byte[] byRead = new byte[256];
            byte[] bySend = new byte[16];
            int nSize = 0;
            int nSend = 14;


            while (true) {
                int nRead = 14;
                try {
                    inStream.read(byRead);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                int i = 0;
                while (i < nRead) {
                    byData[nSize + i] = byRead[i];
                    i += 1;
                }
                nSize += nRead;
                if (nSize >= nSend * 2) {
                    StringBuilder stringBuilder;
                    Object[] objArr;
                    int nPos = 0;
                    i = 0;
                    while (i < nSize - 8) {
                        if (byData[i] == 32 && byData[i + 7] == 13) {
                            if (byData[i + 8] == 10) {
                                nPos = i + 9;
                                nSend = 14;
                            } else {
                                nPos = i + 8;
                                nSend = 13;
                            }
                            if (nPos != 0 && nSize - nPos >= nSend) {
                                for (i = 0; i < nSend; i += 1) {
                                    bySend[i] = byData[nPos + i];
                                }
                                nPos = (nPos + nSend) - 2;
                                for (i = 0; i < nSize - nPos; i += 1) {
                                    byData[i] = byData[nPos + i];
                                }
                                nSize -= nPos;
                                if (bySend[4] == 83) {
                                    bySend[4] = (byte) 53;
                                }
                                for (i = 1; i < 5; i += 1) {
                                    if (bySend[i] == 59) {
                                        bySend[i] = (byte) 32;
                                    }
                                    if (bySend[i] == 58) {
                                        bySend[i] = (byte) 76;
                                    }
                                }
                                strOutput = "  ";
                                for (i = 0; i < nSend; i += 1) {
                                    stringBuilder = new StringBuilder(String.valueOf(strOutput));
                                    objArr = new Object[1];
                                    objArr[0] = Byte.valueOf(bySend[i]);
                                    strOutput = stringBuilder.append(String.format("%02X-", objArr)).toString();
                                }
                                Log.i(bluetoothTag, "Send  Data:" + strOutput + "   size = " + nSend);
                                if (bySend[5] == 32) {
                                    Log.w(bluetoothTag, "The space char (6) was not correct! (30 - 34)");
                                } else if (bySend[6] <= 52 || bySend[6] < 48) {
                                    Log.w(bluetoothTag, "The pointer num (7) was not correct! (30 - 34)");
                                } else {
                                    //send data
                                }
                            }
                        } else {
                            i += 1;
                        }
                    }
                    for (i = 0; i < nSend; i += 1) {
                        bySend[i] = byData[nPos + i];
                    }
                    nPos = (nPos + nSend) - 2;
                    for (i = 0; i < nSize - nPos; i += 1) {
                        byData[i] = byData[nPos + i];
                    }
                    nSize -= nPos;
                    if (bySend[4] == 83) {
                        bySend[4] = (byte) 53;
                    }
                    for (i = 1; i < 5; i += 1) {
                        if (bySend[i] == 59) {
                            bySend[i] = (byte) 32;
                        }
                        if (bySend[i] == 58) {
                            bySend[i] = (byte) 76;
                        }
                    }
                    strOutput = "  ";
                    for (i = 0; i < nSend; i += 1) {
                        stringBuilder = new StringBuilder(String.valueOf(strOutput));
                        objArr = new Object[1];
                        objArr[0] = Byte.valueOf(bySend[i]);
                        strOutput = stringBuilder.append(String.format("%02X-", objArr)).toString();
                    }
                    Log.i(bluetoothTag, "Send  Data:" + strOutput + "   size = " + nSend);
                    if (bySend[5] == 32) {
                        if (bySend[6] <= 52) {
                        }
                        Log.w(bluetoothTag, "The pointer num (7) was not correct! (30 - 34)");
                    } else {
                        Log.w(bluetoothTag, "The space char (6) was not correct! (30 - 34)");
                    }
                }
            }
        }*/


}
