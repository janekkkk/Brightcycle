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
import android.widget.Toast;

import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.guidance.NavigationManager;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.routing.CoreRouter;
import com.here.android.mpa.routing.Maneuver;
import com.here.android.mpa.routing.Route;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;
import com.here.android.mpa.routing.RouteWaypoint;
import com.here.android.mpa.routing.RoutingError;
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
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.scalified.fab.ActionButton;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import aarhusuniversitet.brightcycle.Models.Emergency;
import br.com.mauker.materialsearchview.MaterialSearchView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class HereMapsActivity extends AppCompatActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.search_view)
    MaterialSearchView searchView;
    @BindView(R.id.fabButton)
    ActionButton fabButton;

    private Map map = null;
    private MapRoute mapRoute = null;
    private PositioningManager positioningManager;
    private NavigationManager navigationManager;
    private CoreRouter coreRouter;
    private boolean appPaused;
    private Route route;
    private static int routeColor;

    private DrivingInformation drivingInformation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_here_maps);

        ButterKnife.bind(this);
        Timber.plant(new Timber.DebugTree());
        appPaused = false;

        createActionBar();
        createAppDrawer();
        initHereMaps();
        createSearchSuggestionsOnTextChange();
        initFabButton();

        routeColor = getApplicationContext().getColor(R.color.fab_material_amber_500);
    }

    public void onResume() {
        super.onResume();
        appPaused = false;
        if (positioningManager != null) {
            positioningManager.start(PositioningManager.LocationMethod.GPS_NETWORK);
        }
        if (navigationManager != null && navigationManager
                .getRunningState() == NavigationManager.NavigationState.PAUSED) {
            attachNavigationListeners();

            NavigationManager.Error error = navigationManager.resume();
            if (error != NavigationManager.Error.NONE) {
                Toast.makeText(getApplicationContext(),
                        "NavigationManager resume failed: " + error.toString(), Toast.LENGTH_LONG)
                        .show();
                return;
            }

            // Notify navigation listener to update the maneuver billboard and show waypointer
            navigationNewInstructionListener.onNewInstructionEvent();
        }
    }

    @Override
    protected void onPause() {
        if (positioningManager != null) {
            positioningManager.stop();
        }
        detachNavigationListeners();
        super.onPause();
        appPaused = true;
    }

    @Override
    public void onDestroy() {
        if (positioningManager != null) {
            positioningManager.removeListener(positionListener);
        }
        map = null;
        super.onDestroy();
    }

    // ------------- Navigation and routing -------------

    private void initHereMaps() {
        final MapFragment mapFragment = (MapFragment)
                getFragmentManager().findFragmentById(R.id.mapfragment);

        mapFragment.init(error -> {
            if (error == OnEngineInitListener.Error.NONE) {
                // retrieve a reference of the map from the map fragment
                map = mapFragment.getMap();
                coreRouter = new CoreRouter();

                // Set current location indicator
                map.getPositionIndicator().setVisible(true);

                // Set the map center to the Aarhus region
                map.setCenter(drivingInformation.mockLocation,
                        Map.Animation.NONE);

                // Set the zoom level to the average between min and max
                map.setZoomLevel((map.getMaxZoomLevel() + map.getMinZoomLevel()) / 2);

                // Set position manager to get current location.
                positioningManager = PositioningManager.getInstance();

                positioningManager.addListener(
                        new WeakReference<PositioningManager.OnPositionChangedListener>(positionListener));

                if (positioningManager.start(PositioningManager.LocationMethod.GPS_NETWORK)) {
                    // Position updates started successfully.
                    Timber.d("Current location getting started.");
                }

                drivingInformation = DrivingInformation.getInstance(this, BluetoothConnection.getInstance(this));

                Timber.d("Initialized Here maps");
            } else {
                Timber.d("Initializing Here Maps Failed... " + error);
            }
        });
    }

    @OnClick(R.id.fabButton)
    public void startNavigation(View view) {
        if (map != null && route != null) {
            navigationManager = NavigationManager.getInstance();
            navigationManager.setMap(map);
            navigationManager.setMapUpdateMode(NavigationManager.MapUpdateMode.ROADVIEW);

            NavigationManager.Error error = navigationManager.startNavigation(route);

            if (error == NavigationManager.Error.NONE) {
                fabButton.setVisibility(View.INVISIBLE);
                Timber.d("Navigation started!");
            } else {
                Timber.d("Navigation starting error: " + error);
                navigationManager.setMap(null);
            }
        } else {
            Timber.d("Navigation starting error...");
        }
        navigationManager
                .setNaturalGuidanceMode(EnumSet.of(NavigationManager.NaturalGuidanceMode.JUNCTION));
    }

    public void getDirections(GeoCoordinate endPoint) {
        // Clear previous results
        if (map != null && mapRoute != null) {
            map.removeMapObject(mapRoute);
            mapRoute = null;
        }

        // Select routing options via RoutingMode
        RoutePlan routePlan = new RoutePlan();
        RouteOptions routeOptions = new RouteOptions();
        routeOptions.setTransportMode(RouteOptions.TransportMode.CAR);
        routeOptions.setRouteType(RouteOptions.Type.FASTEST);
        routePlan.setRouteOptions(routeOptions);

        // Select Waypoints for your routes
        // Start
        routePlan.addWaypoint(new RouteWaypoint(drivingInformation.currentLocation.getCoordinate()));

        // Destination
        routePlan.addWaypoint(new RouteWaypoint(endPoint));

        // Start calculating the route from your current location to the destination.
        coreRouter.calculateRoute(routePlan, new RouteListener());
    }

    // Current location listener
    public PositioningManager.OnPositionChangedListener positionListener = new
            PositioningManager.OnPositionChangedListener() {

                public void onPositionUpdated(PositioningManager.LocationMethod method,
                                              GeoPosition position, boolean isMapMatched) {
                    if (!appPaused) {
                        drivingInformation.currentLocation.setCoordinate(position.getCoordinate());
                    }
                }

                public void onPositionFixChanged(PositioningManager.LocationMethod method,
                                                 PositioningManager.LocationStatus status) {
                    if (status == PositioningManager.LocationStatus.AVAILABLE) {
                        Timber.d("Current location available!");
                        drivingInformation.currentLocation.setCoordinate(positioningManager.getPosition().getCoordinate());
                        map.setCenter(drivingInformation.currentLocation.getCoordinate(), Map.Animation.BOW);
                    }
                }
            };

    private class RouteListener implements CoreRouter.Listener {

        @Override
        public void onProgress(int percentage) {
        }

        @Override
        public void onCalculateRouteFinished(List<RouteResult> routeResult, RoutingError error) {

            // If the route was calculated successfully
            if (error == RoutingError.NONE && routeResult.get(0).getRoute() != null) {
                // create a map route object and place it on the map
                route = routeResult.get(0).getRoute();
                mapRoute = new MapRoute(route);
                mapRoute.setColor(routeColor);

                map.addMapObject(mapRoute);

                // Get the bounding box containing the route and zoom in
                GeoBoundingBox boundingBox = route.getBoundingBox();
                map.zoomTo(boundingBox, Map.Animation.BOW,
                        Map.MOVE_PRESERVE_ORIENTATION);
                fabButton.setVisibility(View.VISIBLE);
            } else {
                Timber.d("Route calculation failed... " + error);
            }
        }
    }

    private final NavigationManager.NavigationManagerEventListener navigationListener = new NavigationManager.NavigationManagerEventListener() {
        @Override
        public void onEnded(final NavigationManager.NavigationMode mode) {
            // NOTE: this method is called in both cases when destination is reached and when
            // NavigationManager is stopped.
            Toast.makeText(getApplicationContext(), "Destination reached!", Toast.LENGTH_LONG)
                    .show();

            // Revert to default behavior
            navigationManager.setMapUpdateMode(NavigationManager.MapUpdateMode.NONE);
            navigationManager.setTrafficAvoidanceMode(NavigationManager.TrafficAvoidanceMode.DISABLE);
            navigationManager.setMap(null);
        }

        @Override
        public void onRouteUpdated(final Route updatedRoute) {
            // This does not happen on re-route
            Toast.makeText(getApplicationContext(), "Your route was udated!", Toast.LENGTH_LONG)
                    .show();

            map.removeMapObject(mapRoute);
            mapRoute = new MapRoute(updatedRoute);
            showRoute();
        }
    };

    // Route updated
    private final NavigationManager.RerouteListener navigationRerouteListener = new NavigationManager.RerouteListener() {
        @Override
        public void onRerouteEnd(Route route) {
            Toast.makeText(getApplicationContext(), "Your route was updated!", Toast.LENGTH_LONG)
                    .show();

            map.removeMapObject(mapRoute);
            mapRoute = new MapRoute(route);
            showRoute();
        }
    };

    private final NavigationManager.PositionListener navigationPositionListener = new NavigationManager.PositionListener() {
        @Override
        public void onPositionUpdated(final GeoPosition position) {
            if (navigationManager == null) {
                return;
            }

            GeoCoordinate coordinate = position.getCoordinate();
            if (coordinate == null || !coordinate.isValid()) {
                return;
            }

            // Due to false Altitude from map data
            coordinate.setAltitude(0);
        }
    };

    // New maneuver
    private final NavigationManager.NewInstructionEventListener navigationNewInstructionListener = new NavigationManager.NewInstructionEventListener() {
        @Override
        public void onNewInstructionEvent() {
            Timber.d("onNewInstructionEvent");

            final Maneuver maneuver = navigationManager.getNextManeuver();

            if (maneuver == null) {
                Timber.d("onNewInstructionEvent - invalid maneuver");
            }

            // Show next maneuver
            // TODO updateNextManeuverBillboard(maneuver);
        }
    };

    /**
     * Displays calculated route on the map and zooms-in to route's bounding box.
     */
    private void showRoute() {
        if (mapRoute == null) {
            Timber.d("Failed to show route.");
            return;
        }

        mapRoute.setColor(routeColor);
        map.addMapObject(mapRoute);

        // Zoom in
        Route mainRoute = mapRoute.getRoute();
        map.zoomTo(mainRoute.getBoundingBox(), Map.Animation.BOW, Map.MOVE_PRESERVE_ORIENTATION);
    }

    /**
     * Attaches listeners to navigation manager.
     */
    private void attachNavigationListeners() {
        if (navigationManager != null) {
            navigationManager
                    .addPositionListener(new WeakReference<NavigationManager.PositionListener>(
                            navigationPositionListener));
            navigationManager.addNewInstructionEventListener(
                    new WeakReference<NavigationManager.NewInstructionEventListener>(
                            navigationNewInstructionListener));
            navigationManager.addNavigationManagerEventListener(
                    new WeakReference<NavigationManager.NavigationManagerEventListener>(
                            navigationListener));
            navigationManager
                    .addRerouteListener(new WeakReference<NavigationManager.RerouteListener>(
                            navigationRerouteListener));
        }
    }

    /**
     * Detaches listeners from navigation manager.
     */
    private void detachNavigationListeners() {
        if (navigationManager != null) {
            navigationManager.removeRerouteListener(navigationRerouteListener);
            navigationManager.removeNavigationManagerEventListener(navigationListener);
            navigationManager
                    .removeNewInstructionEventListener(navigationNewInstructionListener);
            navigationManager.removePositionListener(navigationPositionListener);
        }
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
                    getDirections(data.get(0).getCoordinate());
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
            if (error != ErrorCode.NONE) {
                // TODO Handle error
            } else {
                searchView.addSuggestions(data);
            }
        }
    }

    private void getSearchSuggestions(String term) {
        try {
            TextSuggestionRequest request = null;
            request = new TextSuggestionRequest(term).setSearchCenter(map.getCenter());

            if (request.execute(new SuggestionQueryListener()) !=
                    ErrorCode.NONE) {
                // TODO Handle request error
            }
        } catch (IllegalArgumentException ex) {
            // TODO Handle invalid create search request parameters
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
                getCoordinates(suggestion);
                searchView.closeSearch();
                Timber.d("Submitted " + suggestion);
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
            searchView.setQuery(suggestion, false);
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
                            Emergency.makeEmergencySMS(this, drivingInformation.currentLocation.getCoordinate());
                            break;
                        case 2:
                            Intent intent = new Intent(this, SettingsActivity.class);
                            startActivity(intent);
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

        drawer.addItem(new SecondaryDrawerItem().withIdentifier(6).withName("Heibersgade 12, Aarhus"));
        drawer.addItem(new SecondaryDrawerItem().withIdentifier(7).withName("Norregade 8, Aarhus"));

        drawer.updateIcon(6, new ImageHolder(R.drawable.ic_directions));
        drawer.updateIcon(7, new ImageHolder(R.drawable.ic_directions));
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
    public void onBackPressed() {
        if (searchView.isOpen()) {
            searchView.closeSearch();
        } else {
            super.onBackPressed();
        }
    }

}
