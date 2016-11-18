package aarhusuniversitet.brightcycle;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.routing.RouteManager;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.holder.ImageHolder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;

import java.lang.ref.WeakReference;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class HereMapsActivity extends AppCompatActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    // map embedded in the map fragment
    private Map map = null;

    // map fragment embedded in this activity
    private MapFragment mapFragment = null;

    // TextView for displaying the current map scheme
    private TextView textViewResult = null;

    // MapRoute for this activity
    private MapRoute mapRoute = null;

    private PositioningManager posManager;
    private boolean appPaused;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_here_maps);

        ButterKnife.bind(this);
        Timber.plant(new Timber.DebugTree());

        createActionBar();
        createAppDrawer();


        posManager = PositioningManager.getInstance();

        initHereMaps();

        appPaused = false;
        Timber.d("TESTTTT");
        if (posManager != null) {
            Timber.d("Teeest");
            posManager.start(
                    PositioningManager.LocationMethod.GPS_NETWORK);

        }
    }

    @Override
    protected void onPause() {
        if (posManager != null) {
            posManager.stop();
        }
        super.onPause();
        appPaused = true;
    }

    @Override
    // To remove the positioning listener
    public void onDestroy() {
        if (posManager != null) {
            // Cleanup
            posManager.removeListener(
                    positionListener);
        }
        map = null;
        super.onDestroy();
    }

    // Define positioning listener
    private PositioningManager.OnPositionChangedListener positionListener = new
            PositioningManager.OnPositionChangedListener() {

                public void onPositionUpdated(PositioningManager.LocationMethod method,
                                              GeoPosition position, boolean isMapMatched) {
                    // set the center only when the app is in the foreground
                    // to reduce CPU consumption
                    if (!appPaused) {
                        map.setCenter(position.getCoordinate(),
                                Map.Animation.NONE);
                    }
                }

                public void onPositionFixChanged(PositioningManager.LocationMethod method,
                                                 PositioningManager.LocationStatus status) {
                }
            };


    private void initHereMaps() {
        // Search for the map fragment to finish setup by calling init().
        mapFragment = (MapFragment) getFragmentManager().findFragmentById(
                R.id.mapfragment);
        mapFragment.init(error -> {
            if (error == OnEngineInitListener.Error.NONE) {
                // retrieve a reference of the map from the map fragment
                map = mapFragment.getMap();

                // Set current location indicator
                map.getPositionIndicator().setVisible(true);

                // Set the map center to the Aarhus region
                map.setCenter(new GeoCoordinate(56.1629, 10.2039, 0.0),
                        Map.Animation.LINEAR);
                // Set the zoom level to the average between min and max
                map.setZoomLevel(
                        (map.getMaxZoomLevel() + map.getMinZoomLevel()) / 2);
                textViewResult = (TextView) findViewById(R.id.title);
                textViewResult.setText(R.string.textview_routecoordinates_2waypoints);

                posManager = PositioningManager.getInstance();
                posManager.start(
                        PositioningManager.LocationMethod.GPS_NETWORK);

                // Register positioning listener
                posManager.addListener(
                        new WeakReference<PositioningManager.OnPositionChangedListener>(positionListener));


                // Display position indicator
                map.getPositionIndicator().setVisible(true);
            } else {
                Timber.d("Initializing Here Maps Failed...");
            }
        });


    }

    private RouteManager.Listener routeManagerListener =
            new RouteManager.Listener() {
                public void onCalculateRouteFinished(RouteManager.Error errorCode,
                                                     List<RouteResult> result) {

                    if (errorCode == RouteManager.Error.NONE &&
                            result.get(0).getRoute() != null) {

                        // create a map route object and place it on the map
                        mapRoute = new MapRoute(result.get(0).getRoute());
                        map.addMapObject(mapRoute);

                        // Get the bounding box containing the route and zoom in
                        GeoBoundingBox gbb = result.get(0).getRoute().getBoundingBox();
                        map.zoomTo(gbb, Map.Animation.LINEAR,
                                Map.MOVE_PRESERVE_ORIENTATION);

                        textViewResult.setText(
                                String.format("Route calculated with %d maneuvers.",
                                        result.get(0).getRoute().getManeuvers().size()));
                    } else {
                        textViewResult.setText(
                                String.format("Route calculation failed: %s",
                                        errorCode.toString()));
                    }
                }

                public void onProgress(int percentage) {
                    textViewResult.setText(
                            String.format("... %d percent done ...", percentage));
                }
            };

    @OnClick(R.id.btnGetDirections)
    // Functionality for taps of the "Get Directions" button
    public void getDirections(View view) {
        // 1. clear previous results
        textViewResult.setText("");
        if (map != null && mapRoute != null) {
            map.removeMapObject(mapRoute);
            mapRoute = null;
        }

        // 2. Initialize RouteManager
        RouteManager routeManager = new RouteManager();

        // 3. Select routing options via RoutingMode
        RoutePlan routePlan = new RoutePlan();

        RouteOptions routeOptions = new RouteOptions();
        routeOptions.setTransportMode(RouteOptions.TransportMode.PEDESTRIAN);
        routeOptions.setRouteType(RouteOptions.Type.FASTEST);
        routePlan.setRouteOptions(routeOptions);

        // 4. Select Waypoints for your routes
        // START
        routePlan.addWaypoint(new GeoCoordinate(56.1629, 10.2039));

        // END
        routePlan.addWaypoint(new GeoCoordinate(56.1503, 10.2047));

        // 5. Retrieve Routing information via RouteManagerListener
        RouteManager.Error error =
                routeManager.calculateRoute(routePlan, routeManagerListener);
        if (error != RouteManager.Error.NONE) {
            Toast.makeText(getApplicationContext(),
                    "Route calculation failed with: " + error.toString(),
                    Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private void createActionBar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setHomeButtonEnabled(false);
    }

    public void createAppDrawer() {
        Drawer result = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .addDrawerItems(
                        new PrimaryDrawerItem().withIdentifier(1).withName("Emergency call"),
                        new PrimaryDrawerItem().withIdentifier(2).withName("Emergency SMS"),
                        new DividerDrawerItem(),
                        new PrimaryDrawerItem().withIdentifier(3).withName("Settings"),
                        new DividerDrawerItem(),
                        new PrimaryDrawerItem().withIdentifier(4).withName("Show parked bicycle"),
                        new DividerDrawerItem(),
                        new PrimaryDrawerItem().withIdentifier(5).withName("Last searched locations")
                )
                .withOnDrawerItemClickListener((view, i, iDrawerItem) -> {
                    Timber.d("Item " + Integer.toString(i) + " Clicked");
                    switch (i) {
                        case 1:
                            Emergency.makeEmergencyCall(this);
                            break;
                        case 2:
                            // Emergency.makeEmergencySMS(this, currentLocation);
                            break;
                        case 3:

                            break;
                    }
                    return false;
                })
                .withSelectedItem(-1)
                .build();

        result.updateIcon(1, new ImageHolder(R.drawable.ic_dialer_sip));
        result.updateIcon(2, new ImageHolder(R.drawable.ic_message));
        result.updateIcon(3, new ImageHolder(R.drawable.ic_settings));
        result.updateIcon(4, new ImageHolder(R.drawable.ic_directions_bike));

        result.addItem(new SecondaryDrawerItem().withIdentifier(6).withName("Heibersgade 12, Aarhus"));
        result.addItem(new SecondaryDrawerItem().withIdentifier(7).withName("Norregade 8, Aarhus"));

        result.updateIcon(6, new ImageHolder(R.drawable.ic_directions));
        result.updateIcon(7, new ImageHolder(R.drawable.ic_directions));
    }


}
