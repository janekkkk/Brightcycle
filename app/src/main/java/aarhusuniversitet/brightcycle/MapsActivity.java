package aarhusuniversitet.brightcycle;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.guidance.NavigationManager;
import com.here.android.mpa.search.ErrorCode;
import com.here.android.mpa.search.GeocodeRequest;
import com.here.android.mpa.search.Location;
import com.here.android.mpa.search.ResultListener;
import com.here.android.mpa.search.TextSuggestionRequest;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.holder.ImageHolder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.scalified.fab.ActionButton;

import java.util.ArrayList;
import java.util.List;

import aarhusuniversitet.brightcycle.Controller.DrivingInformation;
import aarhusuniversitet.brightcycle.Controller.MapsController;
import aarhusuniversitet.brightcycle.Domain.Accelerometer;
import aarhusuniversitet.brightcycle.Domain.Emergency;
import aarhusuniversitet.brightcycle.Domain.GeoLocation;
import br.com.mauker.materialsearchview.MaterialSearchView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class MapsActivity extends AppCompatActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.search_view)
    MaterialSearchView searchView;
    @BindView(R.id.fabButton)
    ActionButton fabButton;
    @BindView(R.id.btnBlinkerLeft)
    Button btnBlinkerLeft;
    @BindView(R.id.btnBlinkerRight)
    Button btnBlinkerRight;

    public boolean appPaused;
    @BindView(R.id.buttonLayout)
    LinearLayout buttonLayout;

    private SensorManager mSensorManager;
    private Sensor accelerometerSensor;
    private MapsController mapsController;
    private DrivingInformation drivingInformation;
    private BluetoothConnection bluetoothConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_here_maps);

        ButterKnife.bind(this);
        Timber.plant(new Timber.DebugTree());

        bluetoothConnection = BluetoothConnection.getInstance(this);

        if (bluetoothConnection.isConnected()) {
            Timber.d("Still connected with bluetooth to: " + bluetoothConnection.bluetoothDevice.getName());
        }

        mapsController = new MapsController(this);
        mapsController.initHereMaps();

        appPaused = false;

        createActionBar();
        createAppDrawer();
        createSearchSuggestionsOnTextChange();
        initFabButton();
        hideBlinkers();
    }

    private void initializeAccelerometer() {
        // Get sensor manager
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        // Get the default sensor of specified type
        accelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(((Accelerometer) drivingInformation.accelerometer).accelerometerSensorListener, accelerometerSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void onResume() {
        super.onResume();
        appPaused = false;
        if (mapsController.positioningManager != null) {
            mapsController.positioningManager.start(PositioningManager.LocationMethod.GPS_NETWORK);
        }
        if (mapsController.navigationManager != null && mapsController.navigationManager
                .getRunningState() == NavigationManager.NavigationState.PAUSED) {
            mapsController.attachNavigationListeners();

            NavigationManager.Error error = mapsController.navigationManager.resume();
            if (error != NavigationManager.Error.NONE) {
                Toast.makeText(getApplicationContext(),
                        "NavigationManager resume failed: " + error.toString(), Toast.LENGTH_LONG)
                        .show();
                return;
            }

            // Notify navigation listener to update the maneuver billboard and show waypointer
            mapsController.navigationNewInstructionListener.onNewInstructionEvent();

            if (accelerometerSensor != null) {
                mSensorManager.registerListener(((Accelerometer) drivingInformation.accelerometer).accelerometerSensorListener, accelerometerSensor,
                        SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }

    @Override
    protected void onPause() {
        if (mapsController.positioningManager != null) {
            mapsController.positioningManager.stop();
        }
        mapsController.detachNavigationListeners();

        if (accelerometerSensor != null) {
            mSensorManager.unregisterListener(((Accelerometer) drivingInformation.accelerometer).accelerometerSensorListener);
        }

        super.onPause();
        appPaused = true;
    }

    @Override
    public void onDestroy() {
        if (mapsController.positioningManager != null) {
            mapsController.positioningManager.removeListener(mapsController.positionListener);
        }
        mapsController.map = null;
        super.onDestroy();
    }

    // ------------- Navigation and routing -------------

    public void initMapCallback() {
        drivingInformation = DrivingInformation.getInstance(this, BluetoothConnection.getInstance(this));
        initializeAccelerometer();
    }

    public void routeCalculatedCallback() {
        fabButton.setVisibility(View.VISIBLE);
        hideBlinkers();
    }

    public void startNavigationCallback() {
        showBlinkers();
    }

    // ------------- Searching, geocoding, reverse geocoding -------------

    /**
     * On searching for addresses (geocoding)
     */
    class GeocodeListener implements ResultListener<List<Location>> {
        @Override
        public void onCompleted(List<Location> data, ErrorCode error) {
            if (error != ErrorCode.NONE) {
                Timber.d("Error getting coordinates of address...");
            } else {
                if (data.size() > 0) {
                    Timber.d("Destination lat: " + String.valueOf(data.get(0).getCoordinate().getLatitude()) + " long: " + String.valueOf(data.get(0).getCoordinate().getLongitude()));
                    mapsController.getDirections(data.get(0).getCoordinate());
                } else {
                    Timber.d("No results for addresses...");
                    runOnUiThread(() -> {
                        searchView.openSearch();
                        Toast.makeText(getApplicationContext(), "No results, try again...", Toast.LENGTH_LONG)
                                .show();
                    });
                }
            }
        }
    }

    class SuggestionQueryListener implements ResultListener<List<String>> {
        @Override
        public void onCompleted(List<String> data, ErrorCode error) {
            if (error == ErrorCode.NONE) {
                searchView.addSuggestions(data);
            } else {
                Toast.makeText(getApplicationContext(),
                        "Error getting suggestions: " + error, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void getSearchSuggestions(String term) {
        try {
            TextSuggestionRequest request;
            request = new TextSuggestionRequest(term).setSearchCenter(mapsController.map.getCenter());
            request.execute(new SuggestionQueryListener());
        } catch (IllegalArgumentException ex) {
            Timber.d("Illegal argument: " + ex);
        }
    }

    private void createSearchSuggestionsOnTextChange() {
        searchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {

            // When the search form is submitted.
            @Override
            public boolean onQueryTextSubmit(String input) {
                getSearchSuggestions(input);
                String suggestion = searchView.getSuggestionAtPosition(0);
                searchView.setQuery(suggestion, false);

                if(suggestion.length() > 0){
                    getCoordinates(suggestion);
                    searchView.closeSearch();
                    Timber.d("Submitted " + suggestion);
                }

                return false;
            }

            // When the text in the search form is updated.
            @Override
            public boolean onQueryTextChange(String newText) {
                getSearchSuggestions(newText);
                return false;
            }
        });

        // Do something when the suggestion list is clicked.
        searchView.setOnItemClickListener((parent, view, position, id) -> {
            String suggestion = searchView.getSuggestionAtPosition(position);
            searchView.setQuery(suggestion, true);
            searchView.closeSearch();
            // Request the coordinates of the suggestion clicked on.
            getCoordinates(suggestion);

            Timber.d("Clicked " + suggestion);
        });
    }

    private void getCoordinates(String address) {
        ResultListener<List<Location>> listener = new GeocodeListener();
        GeocodeRequest request = new GeocodeRequest(address).setSearchArea(drivingInformation.currentLocation.getCoordinate(), 5000);

        if (request.execute(listener) != ErrorCode.NONE) {
            Timber.d("Error getting geocoordinates of destination...");
        }
    }

    // Result when using search by voice.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MaterialSearchView.REQUEST_VOICE && resultCode == RESULT_OK) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (matches != null && matches.size() > 0) {
                String input = matches.get(0);
                if (!TextUtils.isEmpty(input)) {
                    searchView.setQuery(input, false);
                    getSearchSuggestions(input);
                }
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // ------------- Initialization of user interface -------------
    private void initFabButton() {
        fabButton.setImageResource(R.drawable.ic_navigation);
        fabButton.setButtonColor(getApplicationContext().getColor(R.color.fab_material_amber_500));
        fabButton.setButtonColorPressed(getApplicationContext().getColor(R.color.fab_material_amber_900));
    }

    private void createActionBar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setHomeButtonEnabled(false);
    }

    public void createAppDrawer() {
        Drawer drawer = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .addDrawerItems(
                        new PrimaryDrawerItem().withIdentifier(1).withName("Emergency call"),
                        new PrimaryDrawerItem().withIdentifier(2).withName("Emergency SMS"),
                        new DividerDrawerItem(),
                        new PrimaryDrawerItem().withIdentifier(3).withName("Settings"),
                        new DividerDrawerItem(),
                        new PrimaryDrawerItem().withIdentifier(4).withName("Show parked bicycle")
                )
                .withOnDrawerItemClickListener((view, i, iDrawerItem) -> {
                    Timber.d("Item " + Integer.toString(i) + " Clicked");
                    switch (i) {
                        case 0:
                            Emergency.makeEmergencyCall(this);
                            break;
                        case 1:
                            Emergency.makeEmergencySMS(this, drivingInformation.currentLocation.getCoordinate());
                            break;
                        case 3:
                            Intent intent = new Intent(MapsActivity.this, SettingActivity.class);
                            startActivity(intent);
                            break;
                        case 5:
                            drivingInformation.savedBikeLocation = GeoLocation.getLastLocation();
                            mapsController.showBikeLocation();
                            break;
                    }
                    return false;
                })
                .withSelectedItem(-1)
                .build();

        drawer.updateIcon(1, new ImageHolder(R.drawable.ic_dialer_sip));
        drawer.updateIcon(2, new ImageHolder(R.drawable.ic_message));
        drawer.updateIcon(3, new ImageHolder(R.drawable.ic_settings));
        drawer.updateIcon(4, new ImageHolder(R.drawable.ic_directions_bike));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                searchView.openSearch();
                return true;
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (searchView.isOpen()) {
            searchView.closeSearch();
        } else {
            super.onBackPressed();
        }
    }

    private void hideBlinkers() {
        btnBlinkerLeft.setVisibility(View.INVISIBLE);
        btnBlinkerRight.setVisibility(View.INVISIBLE);
    }

    private void showBlinkers() {
        Snackbar.make(buttonLayout, "Use the left or right side of the screen to activate the blinkers", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();

        btnBlinkerLeft.setVisibility(View.VISIBLE);
        btnBlinkerRight.setVisibility(View.VISIBLE);
        btnBlinkerLeft.setBackgroundColor(getApplicationContext().getColor(R.color.fab_material_amber_500));
        btnBlinkerRight.setBackgroundColor(getApplicationContext().getColor(R.color.fab_material_amber_500));
        btnBlinkerLeft.getBackground().setAlpha(128);
        btnBlinkerRight.getBackground().setAlpha(128);

        Handler handler = new Handler();
        handler.postDelayed(() -> {
            btnBlinkerRight.setBackgroundColor(Color.TRANSPARENT);
            btnBlinkerRight.setText("");

            btnBlinkerLeft.setBackgroundColor(Color.TRANSPARENT);
            btnBlinkerLeft.setText("");
        }, 5000);

    }

    @OnClick(R.id.btnBlinkerLeft)
    public void buttonBlinkerLeftClicked(View button) {
        Timber.d("Left button pressed");

        drivingInformation.startBlinking("l");
    }

    @OnClick(R.id.btnBlinkerRight)
    public void buttonBlinkerRightClicked(View button) {
        Timber.d("Right button pressed");
        drivingInformation.startBlinking("r");
    }

    @OnClick(R.id.fabButton)
    public void fabButtonClicked(View fabButton) {
        mapsController.startNavigation(fabButton);
    }
}
