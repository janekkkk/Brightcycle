package aarhusuniversitet.brightcycle;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.robotpajamas.blueteeth.BlueteethDevice;
import com.robotpajamas.blueteeth.BlueteethManager;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_BLUETOOTH_ENABLE = 1000;
    private static final int DEVICE_SCAN_MILLISECONDS = 10000;
    ArrayList<BlueteethDevice> bluetoothDevices = new ArrayList<>();

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);

        // If BLE support isn't there, quit the app
        checkBluetoothSupport();
    }

    @Override
    protected void onResume() {
        super.onResume();

        bluetoothDevices.clear();
        startScanning();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopScanning();
    }

    private void startScanning() {
        // Clear existing devices (assumes none are connected)
        Log.d("TEST", "Start scanning");
        bluetoothDevices.clear();
        BlueteethManager.with(this).scanForPeripherals(DEVICE_SCAN_MILLISECONDS, bleDevices -> {
            Log.d("TEST", "On Scan completed");
            //mSwipeRefresh.setRefreshing(false);
            for (BlueteethDevice device : bleDevices) {
                if (!TextUtils.isEmpty(device.getBluetoothDevice().getName())) {
                    Log.d("TEST", device.getName() + ", " + device.getMacAddress());
                    bluetoothDevices.add(device);
                }
            }
        });
    }

    private void stopScanning() {
        // Update the button, and shut off the progress bar
        BlueteethManager.with(this).stopScanForPeripherals();
    }

    private void checkBluetoothSupport() {
        // Check for BLE support - also checked from Android manifest.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            exitApp("No BLE Support...");
        }

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            exitApp("No BLE Support...");
        }

        //noinspection ConstantConditions
        if (!btAdapter.isEnabled()) {
            enableBluetooth();
        }
    }

    private void exitApp(String reason) {
        // Something failed, exit the app and send a toast as to why
        Toast.makeText(getApplicationContext(), reason, Toast.LENGTH_LONG).show();
        finish();
    }

    private void enableBluetooth() {
        // Ask user to enable bluetooth if it is currently disabled
        startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQ_BLUETOOTH_ENABLE);
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
