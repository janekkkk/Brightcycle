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
import android.widget.AdapterView;
import android.widget.Button;

import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.Image;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.guidance.NavigationManager;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.routing.CoreRouter;
import com.here.android.mpa.routing.Route;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;
import com.here.android.mpa.routing.RouteWaypoint;
import com.here.android.mpa.routing.RoutingError;
import com.here.android.mpa.search.ErrorCode;
import com.here.android.mpa.search.ResultListener;
import com.here.android.mpa.search.TextSuggestionRequest;
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
    private NavigationManager navigationManager;
    CoreRouter coreRouter;
    private boolean appPaused;
    private Route route;

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
    }

    public void onResume() {
        super.onResume();
        appPaused = false;
        if (posManager != null) {
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
    public void onDestroy() {
        if (posManager != null) {
            // Cleanup
            posManager.removeListener(
                    positionListener);
        }
        map = null;
        super.onDestroy();
    }

    // Current location listener
    private PositioningManager.OnPositionChangedListener positionListener = new
            PositioningManager.OnPositionChangedListener() {

                public void onPositionUpdated(PositioningManager.LocationMethod method,
                                              GeoPosition position, boolean isMapMatched) {
                    if (!appPaused) {
                        map.setCenter(position.getCoordinate(),
                                Map.Animation.BOW);
                    }
                }

                public void onPositionFixChanged(PositioningManager.LocationMethod method,
                                                 PositioningManager.LocationStatus status) {
                    Timber.d("Position changed: " + status.name());
                }
            };

    private void initHereMaps() {

        final MapFragment mapFragment = (MapFragment)
                getFragmentManager().findFragmentById(R.id.mapfragment);

        mapFragment.init(new OnEngineInitListener() {
            @Override
            public void onEngineInitializationCompleted(
                    OnEngineInitListener.Error error) {
                if (error == OnEngineInitListener.Error.NONE) {
                    // retrieve a reference of the map from the map fragment
                    map = mapFragment.getMap();
                    coreRouter = new CoreRouter();
                    // Set current location indicator
                    map.getPositionIndicator().setVisible(true);

                    // Set the map center to the Aarhus region
                    map.setCenter(new GeoCoordinate(56.14703396, 10.20783076),
                            Map.Animation.NONE);

                    // Set the zoom level to the average between min and max
                    map.setZoomLevel(
                            (map.getMaxZoomLevel() + map.getMinZoomLevel()) / 2);

                    // Set position manager to get current location.
                    posManager = PositioningManager.getInstance();
                    if (posManager != null) {
                        posManager.start(
                                PositioningManager.LocationMethod.GPS_NETWORK);
                    }

                    posManager.addListener(
                            new WeakReference<PositioningManager.OnPositionChangedListener>(positionListener));

                } else {
                    Timber.d("Initializing Here Maps Failed...");
                }

            }
        });

    }

    @OnClick(R.id.btnStartNavigation)
    public void startNavigation(View view) {
        NavigationManager navigationManager = NavigationManager.getInstance();
        if(map != null && route != null){
            navigationManager.setMap(map);
            NavigationManager.Error error = navigationManager.startNavigation(route);

            if(error != null){
                Timber.d("Navigation starting error: " + error);
            }
        }
        else {
            Timber.d("Navigation starting error...");
        }

    }

    @OnClick(R.id.btnGetDirections)
    public void getDirections(View view) {
        // 1. clear previous results
        if (map != null && mapRoute != null) {
            map.removeMapObject(mapRoute);
            mapRoute = null;
        }

        // 3. Select routing options via RoutingMode
        RoutePlan routePlan = new RoutePlan();

        RouteOptions routeOptions = new RouteOptions();
        routeOptions.setTransportMode(RouteOptions.TransportMode.PEDESTRIAN);
        routeOptions.setRouteType(RouteOptions.Type.FASTEST);
        routePlan.setRouteOptions(routeOptions);

        // 4. Select Waypoints for your routes
        // START
        if (posManager != null) {
            GeoCoordinate startPoint = posManager.getPosition().getCoordinate();
            routePlan.addWaypoint(new RouteWaypoint(startPoint));
            Timber.d("Startpoint set! LAT:" + posManager.getPosition().getCoordinate().getLatitude() + " LONG:" + posManager.getPosition().getCoordinate().getLongitude());
        }
        // END
        GeoCoordinate endPoint = new GeoCoordinate(56.156491, 10.211105);
        routePlan.addWaypoint(new RouteWaypoint(endPoint));

        Image destinationMarkerImage = new Image();
        try {
            destinationMarkerImage.setImageResource(R.drawable.ic_action_location);
            map.addMapObject(new MapMarker(endPoint, destinationMarkerImage));
        } catch (IOException e) {
            e.printStackTrace();
        }

        map.setZoomLevel(
                (map.getMaxZoomLevel() + map.getMinZoomLevel()) / 4);
        coreRouter.calculateRoute(routePlan, new RouteListener());
    }

    private class RouteListener implements CoreRouter.Listener {

        public void onProgress(int percentage) {
        }

        // Method defined in Listener
        public void onCalculateRouteFinished(List<RouteResult> routeResult, RoutingError error) {
            // If the route was calculated successfully
            if (error == RoutingError.NONE && routeResult.get(0) != null) {
                // create a map route object and place it on the map
                route = routeResult.get(0).getRoute();
                mapRoute = new MapRoute(route);
                map.addMapObject(mapRoute);

                // Get the bounding box containing the route and zoom in
                GeoBoundingBox gbb = route.getBoundingBox();
                map.zoomTo(gbb, Map.Animation.LINEAR,
                        Map.MOVE_PRESERVE_ORIENTATION);
                //routeResult.getRoute().getManeuvers().size()
            } else {
                Timber.d("Route calculation failed... " + error.toString());
            }

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
    public void onBackPressed() {
        if (searchView.isOpen()) {
            searchView.closeSearch();
        } else {
            super.onBackPressed();
        }
    }

    // Example Search request listener
    class SuggestionQueryListener implements ResultListener<List<String>> {
        @Override
        public void onCompleted(List<String> data, ErrorCode error) {
            if (error != ErrorCode.NONE) {
                // Handle error
                // ...
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
                //Handle request error
                //...
            }
        } catch (IllegalArgumentException ex) {
            //Handle invalid create search request parameters
        }
    }

    private void createSearchSuggestionsOnTextChange() {
        searchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                getSearchSuggestions(query);
                searchView.setQuery(searchView.getSuggestionAtPosition(0), false);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                getSearchSuggestions(newText);
                return false;
            }
        });

        // Do something when the suggestion list is clicked.
        searchView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String suggestion = searchView.getSuggestionAtPosition(position);
                searchView.setQuery(suggestion, true);
            }
        });
    }

    // Voice search
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MaterialSearchView.REQUEST_VOICE && resultCode == RESULT_OK) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (matches != null && matches.size() > 0) {
                String searchWord = matches.get(0);
                if (!TextUtils.isEmpty(searchWord)) {
                    searchView.setQuery(searchWord, false);
                    getSearchSuggestions(searchWord);
                }
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}
