package aarhusuniversitet.brightcycle;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.net.Uri;
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

import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.btn_connect_bluetooth)
    Button btnConnectBluetooth;
    @BindView(R.id.content_main)
    RelativeLayout contentMain;

    BluetoothConnection bluetoothConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        Timber.plant(new Timber.DebugTree());
        setSupportActionBar(toolbar);
        bluetoothConnection = BluetoothConnection.getInstance(this.getApplicationContext());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!bluetoothConnection.isBluetoothAvailable()) {
            Toast.makeText(this.getApplicationContext(), "Bluetooth is not available on your device.", Toast.LENGTH_LONG).show();
        }

        // TODO Add line when bluetooth has to be enabled again.
        //btnConnectBluetooth.setVisibility(View.VISIBLE);

        Intent intent = new Intent(this, HereMapsActivity.class);
        startActivity(intent);
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

    // Get fired when the user picks a bluetooth connection to connect with.
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
            if (resultCode == Activity.RESULT_OK)
                bluetoothConnection.connect(data);
        } else if (requestCode == BluetoothState.REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                btnConnectBluetooth.setVisibility(View.GONE);
                bluetoothConnection.setupBluetoothConnection();
            } else {
                // Do something if user doesn't choose any device (Pressed back)
                Toast.makeText(this.getApplicationContext(), "No device chosen.", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void requestBluetoothPermission() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, BluetoothConnection.REQUEST_ENABLE_BT);
    }

    private void askUserToChooseBluetoothConnection() {
        Intent intent = new Intent(getApplicationContext(), DeviceList.class);
        startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
    }

    @OnClick(R.id.btn_connect_bluetooth)
    public void connectBluetooth(View view) {
        Timber.d("Bluetooth connect button pressed");
        if (!bluetoothConnection.isBluetoothEnabled()) {
            requestBluetoothPermission();
        } else {
            askUserToChooseBluetoothConnection();
        }
    }

}
