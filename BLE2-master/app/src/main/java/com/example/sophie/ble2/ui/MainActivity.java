package com.example.sophie.ble2.ui;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.sophie.ble2.R;
import com.example.sophie.ble2.bluetooth.BleScanner;
import com.example.sophie.ble2.bluetooth.ScanResultsConsumer;

import java.io.File;
import java.util.ArrayList;
import java.util.UUID;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;

import static com.example.sophie.ble2.bluetooth.BleScanner.distance;
import static com.example.sophie.ble2.bluetooth.BleScanner.uuid;

public class MainActivity extends AppCompatActivity implements ScanResultsConsumer {

    private boolean ble_scanning = false;
    private Handler handler = new Handler();
    private ListAdapter ble_device_list_adapter;
    private BleScanner ble_scanner;
    //scanning for this long!!
    private static final long SCAN_TIMEOUT = 500000;
    private static final int REQUEST_LOCATION = 0;
    private static String[] PERMISSIONS_LOCATION = {Manifest.permission.ACCESS_COARSE_LOCATION};
    private boolean permissions_granted = false;
    private int device_count = 0;
    private Toast toast;

    static class ViewHolder {
        public TextView text;
        public TextView bdaddr;
        public TextView distance;
    }


    @RequiresApi(api = Build.VERSION_CODES.M)

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // hockeyapp
        checkForUpdates();
        //get app context
        Context context = this;
        File dest = context.getExternalFilesDir(null);
        if (dest == null) {
            dest = context.getFilesDir();}
        //set button text
        setButtonText();
        //new adapter
        ble_device_list_adapter = new ListAdapter();
        //find listview
        ListView listView = this.findViewById(R.id.deviceList);
        //fill it
        listView.setAdapter(ble_device_list_adapter);
        //create BLE scanner
        ble_scanner = new BleScanner(this.getApplicationContext());
        //enable me clicking on items - opens area setting
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                //check if I'm scanning
                if (ble_scanning) {
                    //inform I'm not scanning
                    setScanState(false);
                    //stop scanning
                    ble_scanner.stopScanning();
                }
                //get list position of the device I clicked on
                BluetoothDevice device = ble_device_list_adapter.getDevice(position);
                //delete toasts
                if (toast != null) {
                    toast.cancel();
                }
                //open area setting activity
                Intent intent = new Intent(MainActivity.this,
                        SetAreaToMonitor.class);
                //??
                intent.putExtra(SetAreaToMonitor.EXTRA_NAME, device.getName());
                intent.putExtra(SetAreaToMonitor.EXTRA_ID, device.getAddress());
                startActivity(intent);
            }
        });
    }

    @Override
    public void candidateBleDevice(final BluetoothDevice device, byte[] scan_record, int rssi) {
        runOnUiThread(new Runnable() {
            @Override
            //add devices to my list adapter
            public void run() {
                //if new, add device I found
                ble_device_list_adapter.addDevice(device);
                //??
                ble_device_list_adapter.notifyDataSetChanged();
                //increment device count - use in future?
                device_count++;
            }
        });
    }

    @Override
    public void scanningStarted() {
        setScanState(true);
    }

    @Override
    //if the scanning has stopper
    public void scanningStopped() {
        if (toast != null) {
            toast.cancel();
        }
        setScanState(false);
    }
//change buttons text
    private void setButtonText() {
        final String button_text = Constants.FIND;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView) MainActivity.this.findViewById(R.id.scanButton)).setText(button_text);
            }
        });
    }
//void to keep track of scanning
    private void setScanState(boolean value) {
        ble_scanning = value;
        //change button text
        ((Button) this.findViewById(R.id.scanButton)).setText(value ? Constants.STOP_SCANNING : Constants.FIND);
    }

    private class ListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> ble_devices;

        public ListAdapter() {
            super();
            ble_devices = new ArrayList<BluetoothDevice>();
        }
//add device if it's not on the list
        public void addDevice(BluetoothDevice device) {
            if (!ble_devices.contains(device)) {
                ble_devices.add(device);
            }
        }

        /*public boolean contains(BluetoothDevice device) {
            return ble_devices.contains(device);
        }*/

        public BluetoothDevice getDevice(int position) {
            return ble_devices.get(position);
        }

        public void clear() {
            ble_devices.clear();
        }

        @Override
        public int getCount() {
            return ble_devices.size();
        }

        @Override
        public Object getItem(int i) {
            return ble_devices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            if (view == null) {
                view = MainActivity.this.getLayoutInflater().inflate(R.layout.list_row, null);
                viewHolder = new ViewHolder();
                viewHolder.text = view.findViewById(R.id.textView);
                viewHolder.bdaddr = view.findViewById(R.id.bdaddr);
                viewHolder.distance = view.findViewById(R.id.distance);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
            BluetoothDevice device = ble_devices.get(i);
            String deviceName = String.valueOf(uuid);

            if (deviceName != null && deviceName.length() > 0) {
                viewHolder.text.setText(deviceName);
            } else {
                viewHolder.text.setText(R.string.unknown_device);
            }
            viewHolder.bdaddr.setText(device.getAddress());
            viewHolder.distance.setText(String.valueOf(distance));
            return view;
        }
    }

    public void onScan(View view) {
        if (!ble_scanner.isScanning()) {
            device_count = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    permissions_granted = false;
                    requestLocationPermission();
                } else {
                    Log.i(Constants.TAG, "Location permission has already been granted. Starting scanning.");
                    permissions_granted = true;
                }
            } else {
// the ACCESS_COARSE_LOCATION permission did not exist before M so....
                permissions_granted = true;
            }
            startScanning();
        } else {
            ble_scanner.stopScanning();
        }
    }

    private void requestLocationPermission() {
        Log.i(Constants.TAG, "Location permission has NOT yet been granted. Requesting permission.");
        //for API 23+ request permission
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            Log.i(Constants.TAG, "Displaying location permission rationale to provide additional context.");
            //create a dialog for the user to grant permissions
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Permission Required");
            builder.setMessage("Please grant Location access so this application can perform Bluetooth scanning");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                public void onDismiss(DialogInterface dialog) {
                    Log.d(Constants.TAG, "Requesting permissions after explanation");
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
                }
            });
            builder.show();
        } else {
            //for API 22-
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_LOCATION) {
            Log.i(Constants.TAG, "Received response for location permission request.");
// Check if the only required permission has been granted
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
// Location permission has been granted
                Log.i(Constants.TAG, "Location permission has now been granted. Scanning.....");
                permissions_granted = true;
                if (ble_scanner.isScanning()) {
                    startScanning();
                }
            } else {
                Log.i(Constants.TAG, "Location permission was NOT granted.");
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
//toast maker
    private void simpleToast(String message, int duration) {
        toast = Toast.makeText(this, message, duration);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }
//SCAN!
    private void startScanning() {
        if (permissions_granted) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ble_device_list_adapter.clear();
                    ble_device_list_adapter.notifyDataSetChanged();
                }
            });
            //inform I started scanning
            simpleToast(Constants.SCANNING, 2000);
            ble_scanner.startScanning(this, SCAN_TIMEOUT);
        } else {
            Log.i(Constants.TAG, "Permission to perform Bluetooth scanning was not yet granted");
        }
    }
    //hockey app
    @Override
    public void onResume() {
        super.onResume();
        // ... your own onResume implementation
        checkForCrashes();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterManagers();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterManagers();
    }

    private void checkForCrashes() {
        CrashManager.register(this);
    }

    private void checkForUpdates() {
        // Remove this for store builds!
        UpdateManager.register(this);
    }

    private void unregisterManagers() {
        UpdateManager.unregister();
    }

}

