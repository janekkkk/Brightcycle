package aarhusuniversitet.brightcycle;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.Image;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;
import com.here.android.mpa.mapping.MapMarker;
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

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import br.com.mauker.materialsearchview.MaterialSearchView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class HereMapsActivity extends AppCompatActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.btnGetDirections)
    Button btnGetDirections;
    @BindView(R.id.search_view)
    MaterialSearchView searchView;

    private Map map = null;
    private MapFragment mapFragment = null;
    private MapRoute mapRoute = null;
    private PositioningManager posManager;

    private boolean appPaused;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_here_maps);

        ButterKnife.bind(this);
        Timber.plant(new Timber.DebugTree());
        appPaused = false;

        createActionBar();
        createAppDrawer();

        posManager = PositioningManager.getInstance();
        initHereMaps();
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
                                Map.Animation.LINEAR);
                    }
                }

                public void onPositionFixChanged(PositioningManager.LocationMethod method,
                                                 PositioningManager.LocationStatus status) {
                    Timber.d("Position changed: " + status.name());
                }
            };

    private void initHereMaps() {
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

                    RouteResult routeResult = result.get(0);
                    if (errorCode == RouteManager.Error.NONE &&
                            routeResult.getRoute() != null) {

                        // create a map route object and place it on the map
                        mapRoute = new MapRoute(routeResult.getRoute());
                        map.addMapObject(mapRoute);

                        // Get the bounding box containing the route and zoom in
                        GeoBoundingBox gbb = routeResult.getRoute().getBoundingBox();
                        map.zoomTo(gbb, Map.Animation.LINEAR,
                                Map.MOVE_PRESERVE_ORIENTATION);

                        //routeResult.getRoute().getManeuvers().size()
                    } else {
                        Timber.d("Route calculation failed: %s",
                                errorCode.toString());
                    }
                }

                public void onProgress(int percentage) {
                    Timber.d("... %d percent done ...", percentage);
                }
            };

    @OnClick(R.id.btnGetDirections)
    public void getDirections(View view) {
        // 1. clear previous results
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
        if (posManager != null) {
            routePlan.addWaypoint(posManager.getPosition().getCoordinate());
        }

        // END
        GeoCoordinate destination = new GeoCoordinate(56.156491, 10.211105);
        routePlan.addWaypoint(destination);

        Image destinationMarkerImage = new Image();

        try {
            destinationMarkerImage.setImageResource(R.drawable.ic_action_location);
        } catch (IOException e) {
            e.printStackTrace();
        }

        map.addMapObject(new MapMarker(destination, destinationMarkerImage));

        map.setZoomLevel(35);

        // 5. Retrieve Routing information via RouteManagerListener
        RouteManager.Error error =
                routeManager.calculateRoute(routePlan, routeManagerListener);
        if (error != RouteManager.Error.NONE) {
            Toast.makeText(getApplicationContext(), "Route calculation failed with: " + error.toString(), Toast.LENGTH_SHORT)
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
                        case 0:
                            Emergency.makeEmergencyCall(this);
                            break;
                        case 1:
                            // Emergency.makeEmergencySMS(this, currentLocation);
                            break;
                        case 2:

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
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MaterialSearchView.REQUEST_VOICE && resultCode == RESULT_OK) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (matches != null && matches.size() > 0) {
                String searchWrd = matches.get(0);
                if (!TextUtils.isEmpty(searchWrd)) {
                    searchView.setQuery(searchWrd, false);
                }
            }

            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
