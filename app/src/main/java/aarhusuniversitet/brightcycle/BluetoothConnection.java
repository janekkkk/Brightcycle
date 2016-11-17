package aarhusuniversitet.brightcycle;

import android.content.Context;
import android.content.Intent;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;

public class BluetoothConnection {

    public static final int REQUEST_ENABLE_BT = 1;
    private BluetoothSPP bluetoothSPP;
    private static BluetoothConnection instance = null;

    public static BluetoothConnection getInstance(Context context) {
        if (instance == null) {
            instance = new BluetoothConnection(context);
        }
        return instance;
    }

    public BluetoothConnection(Context context) {
        bluetoothSPP = new BluetoothSPP(context);
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

    public void setupBluetoothConnection() {
        bluetoothSPP.setupService();
        bluetoothSPP.startService(BluetoothState.DEVICE_OTHER);
        setOndataReceivedListener();
    }

    public void setOndataReceivedListener() {
        bluetoothSPP.setOnDataReceivedListener((data, message) -> {
            // Do something when data incoming
        });
    }
}
