package com.example.sophie.beacontransmitter;

import android.app.Notification;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BeaconTransmitter;

import java.util.Arrays;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    Beacon beacon;
    BluetoothAdapter mBluetoothAdapter;
    private final static int REQUEST_ENABLE_BT = 1;
    private AdvertiseCallback mAdvertiseCallback;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    //device name
    String name = "067e6162-3b6f-4ae2-a171-2470b63dff00";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //check if BLE exists
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "ble_not_supported", Toast.LENGTH_SHORT).show();
            finish();
        }
        //
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        //beacon
        beacon = new Beacon.Builder()
                .setId1(name)
                .setId2("1")
                .setId3("2")
                .setManufacturer(0x0118)
                .setTxPower(-59)
                .setDataFields(Arrays.asList(new Long[] {0l}))
                .build();
    }

public void setUUID(View v){
    TextView textView = findViewById(R.id.textView);
    Button transmit = findViewById(R.id.button2);
    String uuid = textView.getText().toString();
    //save to file uuid.

    transmit.setVisibility(View.VISIBLE);
}

    public void transmit(View v){
    //bt initialize
        initialize();
            if (mAdvertiseCallback == null) {
                AdvertiseSettings settings = buildAdvertiseSettings();
                AdvertiseData data = buildAdvertiseData();
                mAdvertiseCallback = new SampleAdvertiseCallback();

                if (mBluetoothLeAdvertiser != null) {
                    mBluetoothLeAdvertiser.startAdvertising(settings, data,
                            mAdvertiseCallback);
                }
            }
        BeaconParser beaconParser = new BeaconParser()
                .setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25");
        BeaconTransmitter beaconTransmitter = new BeaconTransmitter(getApplicationContext(), beaconParser);
        beaconTransmitter.startAdvertising(beacon);
    }
    private AdvertiseData buildAdvertiseData() {

        /**
         * There is a strict limit of 31 Bytes on BLE Advertisements packets .
         *  Packets over this limit will result in a failure with error code
         *  AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE. Catch this error in the
         *  onStartFailure() method of an AdvertiseCallback implementation.
         */

        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.addServiceUuid(ParcelUuid.fromString(name));
        dataBuilder.setIncludeDeviceName(true);

        return dataBuilder.build();
    }

    /**
     * Returns an AdvertiseSettings object set to use low power (to help preserve battery life)
     * and disable the built-in timeout since this code uses its own timeout runnable.
     */
    private AdvertiseSettings buildAdvertiseSettings() {
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY);
        settingsBuilder.setTimeout(0);
        return settingsBuilder.build();
    }

    private class SampleAdvertiseCallback extends AdvertiseCallback {

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);

            Toast.makeText(getApplicationContext(), "Advertising failed",Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Toast.makeText(getApplicationContext(), "Advertising successfully started",Toast.LENGTH_SHORT).show();
        }
    }
    private void initialize() {
        if (mBluetoothLeAdvertiser == null) {
            BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager != null) {
                BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();
                if (mBluetoothAdapter != null) {
                    mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
                } else {
                    Toast.makeText(this, "bt null", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "bt null", Toast.LENGTH_SHORT).show();
            }
        }

    }



}
