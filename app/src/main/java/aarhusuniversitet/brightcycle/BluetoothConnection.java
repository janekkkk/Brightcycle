package aarhusuniversitet.brightcycle;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import aarhusuniversitet.brightcycle.Controller.DrivingInformation;
import aarhusuniversitet.brightcycle.Domain.BlueteethDevice;
import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import timber.log.Timber;

public class BluetoothConnection {

    public static final int REQUEST_ENABLE_BT = 1;
    private BluetoothSPP bluetoothSPP;
    private static BluetoothConnection instance = null;
    private DrivingInformation drivingInformation;
    private Activity activity;
    public BlueteethDevice bluetoothDevice;

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

    public boolean isConnected() {
        return bluetoothDevice != null;
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
        setStateListener();
    }

    private void setConnectionListener() {
        bluetoothSPP.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() {
            public void onDeviceConnected(String name, String address) {
                Timber.d("Connected! " + name + " " + address);
                bluetoothDevice = new BlueteethDevice(name, address);
                bluetoothSPP.send("a", true);
                drivingInformation.turnOnLightsAutomatically();

                Intent intent = new Intent(activity, MapsActivity.class);
                activity.startActivity(intent);
            }

            public void onDeviceDisconnected() {
                Timber.d("Bluetooth device disconnected...");
                Toast.makeText(activity, "Bluetooth device disconnected...",
                        Toast.LENGTH_LONG).show();

                drivingInformation.saveLocationBike();
                bluetoothDevice = null;

                Intent intent = new Intent(activity, MainActivity.class);
                activity.startActivity(intent);
            }

            public void onDeviceConnectionFailed() {
                Timber.d("Bluetooth connection failed!");
                Toast.makeText(activity, "Bluetooth connection failed...",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setStateListener() {
        bluetoothSPP.setBluetoothStateListener((BluetoothSPP.BluetoothStateListener) state -> {
            if (state == BluetoothState.STATE_CONNECTED) {
                Timber.d("Bluetooth state: connected");
            } else if (state == BluetoothState.STATE_CONNECTING) {
                Timber.d("Bluetooth state: connecting...");
            } else if (state == BluetoothState.STATE_LISTEN) {
                Timber.d("Bluetooth state: listening");
                if (isConnected()) {
                    disconnect();
                }
            } else if (state == BluetoothState.STATE_NONE) {
                Timber.d("Bluetooth state: none");
                if (isConnected()) {
                    disconnect();
                }
            }
        });
    }

    private void setOndataReceivedListener() {
        bluetoothSPP.setOnDataReceivedListener((data, message) -> {
            Timber.d("Bluetooth data received: " + message);
        });
    }

    /**
     * Sends data to the microcontroller via Bluetooth
     *
     * @param device Device name
     * @param value  The value to send
     */
    public void sendData(String device, int value) {
        if (bluetoothDevice != null) {
            bluetoothSPP.send(device + Integer.toString(value), true);
            Timber.d("Bluetooth data send: " + device + Integer.toString(value));
        }
    }

    /**
     * Sends data to the microcontroller via Bluetooth
     *
     * @param device Device name
     */
    public void sendData(String device) {
        if (bluetoothDevice != null) {
            bluetoothSPP.send(device, true);
            Timber.d("Bluetooth data send: " + device);
        }
    }

    public void sendData(int value) {
        if (bluetoothDevice != null) {
            bluetoothSPP.send(Integer.toString(value), true);
            Timber.d("Bluetooth data send: " + Integer.toString(value));
        }
    }
}
