package com.example.sophie.ble2.bluetooth;

/**
 * Created by Sophie on 3/2/2018.
 */import android.bluetooth.BluetoothDevice;

public interface ScanResultsConsumer {
    void candidateBleDevice(BluetoothDevice device, byte[] scan_record, int rssi);
    void scanningStarted();
    void scanningStopped();

}
