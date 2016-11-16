package aarhusuniversitet.brightcycle;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    BluetoothSPP bluetoothConnection;

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.btn_connect_bluetooth)
    Button btnConnectBluetooth;
    @BindView(R.id.content_main)
    RelativeLayout contentMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        Timber.plant(new Timber.DebugTree());

        setSupportActionBar(toolbar);
        bluetoothConnection = new BluetoothSPP(this.getApplicationContext());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!bluetoothConnection.isBluetoothAvailable()) {
            Toast.makeText(this.getApplicationContext(), "Bluetooth is not available on your device.", Toast.LENGTH_LONG).show();
        }

        //if you want to update the items at a later time it is recommended to keep it in a variable
        PrimaryDrawerItem item1 = new PrimaryDrawerItem().withIdentifier(1).withName("BrightCycle");
        SecondaryDrawerItem item2 = new SecondaryDrawerItem().withIdentifier(2).withName("Emergency call");
        SecondaryDrawerItem item3 = new SecondaryDrawerItem().withIdentifier(3).withName("Show bicycle location");
        SecondaryDrawerItem item4 = new SecondaryDrawerItem().withIdentifier(4).withName("Settings");

        //create the drawer and remember the `Drawer` result object
        Drawer result = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .addDrawerItems(
                        item1,
                        new DividerDrawerItem(),
                        item2,
                        item3,
                        item4,
                        new DividerDrawerItem()
                )
                .withOnDrawerItemClickListener((view, i, iDrawerItem) -> {
                    return false;
                })
                .build();

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

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
            if (resultCode == Activity.RESULT_OK)
                bluetoothConnection.connect(data);
        } else if (requestCode == BluetoothState.REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                bluetoothConnection.setupService();
                bluetoothConnection.startService(BluetoothState.DEVICE_ANDROID);
                btnConnectBluetooth.setVisibility(View.GONE);
                setupBluetooth();
            } else {
                // Do something if user doesn't choose any device (Pressed back)
                Toast.makeText(this.getApplicationContext(), "No device chosen.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupBluetooth() {

        bluetoothConnection.setOnDataReceivedListener((data, message) -> {
            // Do something when data incoming
        });
    }

    private void requestBluetoothPermission() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    private void askConnection() {
        Intent intent = new Intent(getApplicationContext(), DeviceList.class);
        startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
    }

    @OnClick(R.id.btn_connect_bluetooth)
    public void connectBluetooth(View view) {
        Timber.d("Bluetooth connect button pressed");
        if (!bluetoothConnection.isBluetoothEnabled()) {
            requestBluetoothPermission();
        } else {
            askConnection();
        }
    }

}
