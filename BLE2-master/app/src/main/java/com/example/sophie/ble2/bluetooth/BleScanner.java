package com.example.sophie.ble2.bluetooth;

/**
 * Created by Sophie on 3/2/2018.
 */
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.example.sophie.ble2.logging.SingletonLog;
import com.example.sophie.ble2.ui.Constants;
import com.example.sophie.ble2.ui.MainActivity;
import com.example.sophie.ble2.ui.SetAreaToMonitor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;
import static android.content.Context.MODE_PRIVATE;
import static com.example.sophie.ble2.ui.Constants.monitored_device;
import static com.example.sophie.ble2.ui.SetAreaToMonitor.final_setting;

public class BleScanner extends Activity {
    public static UUID uuid;
    SingletonLog singletonLog = new SingletonLog();
    private BluetoothLeScanner scanner = null;
    private BluetoothAdapter bluetooth_adapter = null;
    private Handler handler = new Handler();
    private ScanResultsConsumer scan_results_consumer;
    private Context context;
    private boolean scanning=false;
    private String device_name_start="";
    //values for average
    public static double distance;
    int moving_average_length = 0;
    boolean enough_data = false;
    double[] moving_average = new double[6];
    //get app context


    //end of avearage values
    public BleScanner(Context context) {this.context = context;
        //I use bluetooth manager to get BT adapter
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetooth_adapter = bluetoothManager.getAdapter();

// check bluetooth is available and on
        if (bluetooth_adapter == null || !bluetooth_adapter.isEnabled()) {
            Log.d(Constants.TAG, "Bluetooth is NOT switched on");
            Intent enableBtIntent = new Intent( BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBtIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(enableBtIntent);
        }

        Log.d(Constants.TAG, "Bluetooth is switched on");
    }
//start scanning for devices
    public void startScanning(final ScanResultsConsumer scan_results_consumer, long stop_after_ms) {
        //check if I'm not already scanning
        if (scanning) {
            Log.d(Constants.TAG, "Already scanning so ignoring startScanning request");
            return;
        }
        if (scanner == null) {
            //create a scanner if I don't have one(??)
            scanner = bluetooth_adapter.getBluetoothLeScanner();
            Log.d(Constants.TAG, "Created BluetoothScanner object");
        }
        //start a countdown to stop the scan
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {if (scanning) {
                Log.d(Constants.TAG, "Stopping scanning");
                scanner.stopScan(scan_callback);
                setScanning(false);
            } }

            //input - number of milisecond before scanning stops
        }, stop_after_ms);
        this.scan_results_consumer = scan_results_consumer;
        Log.d(Constants.TAG,"Scanning");
        //list of filters
        List<ScanFilter> filters;
        filters = new ArrayList<ScanFilter>();
        //filter uuid
// Filters and Settings
        //ScanFilter filter = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(monitored_device)).build();
        //filters.add(filter);
       // Scanning Started
        //low latency = get packages quickly !! not battery optimized
        ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        setScanning(true);
        //start scanning
        scanner.startScan(filters, settings, scan_callback);
    }
    //stop scanning
    public void stopScanning() {
        setScanning(false);
        Log.d(Constants.TAG, "Stopping scanning");
        scanner.stopScan(scan_callback);
    }

    public ScanCallback scan_callback = new ScanCallback() {
        public void onScanResult(int callbackType, final ScanResult result) {
            if (!scanning) {
                return;
            }
            BluetoothDevice device = ble_devices.get(i)
            //String b = result.toString();
            uuid = UUID.nameUUIDFromBytes(result.getScanRecord().getBytes());
            scan_results_consumer.candidateBleDevice(result.getDevice(), result.getScanRecord().getBytes(), result.getRssi());
            //count distance
            double rssi = result.getRssi();
            //show RSSI for CALIBRATION
            //Toast.makeText(context, String.valueOf(rssi), Toast.LENGTH_LONG).show();
            //TO-DO make this a user input//TX = white box: -64 ; estimote: -74 phone -38
            int txPower = -38;

            double ratio = rssi * 1.0 / txPower;
            double estimate = (0.89976) * Math.pow(ratio, 7.7095) + 0.111;
            //finished estimating distance


            if (moving_average_length < 5) {
                moving_average_length++;
            } else {
                moving_average_length = 0;
                enough_data = true;
            }

            moving_average[moving_average_length] = estimate;
//count average

            if (enough_data) {
                //increment all last values
                double average = 0;
                for (int i = 5; i >= 0; i--) {
                    average = average + moving_average[i];
                }
                //count average distance
                distance = average / 6;
                final_setting = loaded_final();
                singletonLog.logDown(String.valueOf(distance) + " - distance");
                singletonLog.logDown(String.valueOf(final_setting) + " - final setting");

                //@Override{
                if (distance < final_setting) {
                    //show toast
                    Toast toast = Toast.makeText(context, "Please, return the device before you leave.", Toast.LENGTH_SHORT);
                    toast.show();
                    // beep
                    ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME);
                    toneGen1.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 100);
                    toneGen1.release();
                }
            }
        }
    };
//scanning or not?
    public boolean isScanning() {
        return scanning;
    }
    void setScanning(boolean scanning) {
        this.scanning = scanning;
        if (!scanning) {
            scan_results_consumer.scanningStopped();
        } else {
            scan_results_consumer.scanningStarted(); }
    }
    private double loaded_final(){
        SharedPreferences sp = context.getApplicationContext().getSharedPreferences(String.valueOf(uuid), MODE_PRIVATE);
        return Double.longBitsToDouble(sp.getLong("final_result", Double.doubleToLongBits(0.01)));
    }
// end of class
}
