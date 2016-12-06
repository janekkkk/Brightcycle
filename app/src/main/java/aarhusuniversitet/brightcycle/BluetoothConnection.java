package aarhusuniversitet.brightcycle;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import aarhusuniversitet.brightcycle.Controller.DrivingInformation;
import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import timber.log.Timber;

public class BluetoothConnection {

    public static final int REQUEST_ENABLE_BT = 1;
    private BluetoothSPP bluetoothSPP;
    private static BluetoothConnection instance = null;
    private DrivingInformation drivingInformation;
    private Activity activity;

    public static BluetoothConnection getInstance(Activity activity) {
        if (instance == null) {
            instance = new BluetoothConnection(activity);
        }
        return instance;
    }

    public BluetoothConnection(Activity activity) {
        this.activity = activity;
        bluetoothSPP = new BluetoothSPP(activity);
        drivingInformation = DrivingInformation.getInstance(activity, this);
    }

    public boolean isBluetoothAvailable() {
        return bluetoothSPP.isBluetoothAvailable();
    }

    public boolean isBluetoothEnabled() {
        return bluetoothSPP.isBluetoothEnabled();
    }

    public void connect(Intent data) {
        bluetoothSPP.connect(data);
    }

    public void disconnect() {
        bluetoothSPP.disconnect();
    }

    public void setupBluetoothConnection() {
        bluetoothSPP.setupService();
        bluetoothSPP.startService(BluetoothState.DEVICE_OTHER);
        setOndataReceivedListener();
        setConnectionListener();
    }

    private void setConnectionListener() {
        bluetoothSPP.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() {
            public void onDeviceConnected(String name, String address) {
                // Do something when successfully connected
                Timber.d("Connected! " + name + " " + address);
            }

            public void onDeviceDisconnected() {
                // Do something when connection was disconnected
                drivingInformation.saveLocationBike();
                Timber.d("Bluetooth device disconnected!");
            }

            public void onDeviceConnectionFailed() {
                // Do something when connection failed
                Timber.d("Bluetooth connection failed!");
            }
        });
    }

    private void setOndataReceivedListener() {

    }

    public void sendData(String device, int value) {
    }
}
