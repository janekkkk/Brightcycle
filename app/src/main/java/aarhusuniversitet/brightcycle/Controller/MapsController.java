package aarhusuniversitet.brightcycle.Controller;

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

import java.lang.ref.WeakReference;
import java.util.EnumSet;
import java.util.List;

import aarhusuniversitet.brightcycle.BluetoothConnection;
import aarhusuniversitet.brightcycle.MapsActivity;
import aarhusuniversitet.brightcycle.R;
import timber.log.Timber;

public class MapsController {

    private MapsActivity mapsActivity;
    public PositioningManager positioningManager;
    public NavigationManager navigationManager;

    private CoreRouter coreRouter;
    public Map map = null;
    private Route route;
    private MapRoute mapRoute = null;

    private DrivingInformation drivingInformation;
    private int routeColor;

    public MapsController(MapsActivity activity) {
        this.mapsActivity = activity;
        routeColor = mapsActivity.getApplicationContext().getColor(R.color.fab_material_amber_500);
    }

    public void initHereMaps() {
        final MapFragment mapFragment = (MapFragment)
                mapsActivity.getFragmentManager().findFragmentById(R.id.mapfragment);

        mapFragment.init(error -> {
            if (error == OnEngineInitListener.Error.NONE) {
                drivingInformation = DrivingInformation.getInstance(mapsActivity, BluetoothConnection.getInstance(mapsActivity));

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

                Timber.d("Initialized Here maps");
                mapsActivity.initMapCallback();
            } else {
                Timber.d("Initializing Here Maps Failed... " + error);
            }
        });
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

        // Select Waypoints for route
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
                    if (!mapsActivity.appPaused) {
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

    public void startNavigation(View fabButton) {
        if (map != null && route != null) {
            navigationManager = NavigationManager.getInstance();
            navigationManager.setMap(map);
            navigationManager.setMapUpdateMode(NavigationManager.MapUpdateMode.ROADVIEW);

            NavigationManager.Error error = navigationManager.startNavigation(route);

            if (error == NavigationManager.Error.NONE) {
                fabButton.setVisibility(View.INVISIBLE);
                Timber.d("Navigation started!");
                Toast.makeText(mapsActivity.getApplicationContext(), "Navigation started!",
                        Toast.LENGTH_SHORT).show();
                map.setCenter(drivingInformation.currentLocation.getCoordinate(), Map.Animation.BOW);
                map.setZoomLevel(map.getMaxZoomLevel());
                navigationManager
                        .setNaturalGuidanceMode(EnumSet.of(NavigationManager.NaturalGuidanceMode.JUNCTION));
            } else {
                Timber.d("Navigation starting error: " + error);
                navigationManager.setMap(null);
            }
        } else {
            Timber.d("Navigation starting error...");
        }
    }

    private class RouteListener implements CoreRouter.Listener {

        @Override
        public void onProgress(int percentage) {

        }

        @Override
        public void onCalculateRouteFinished(List<RouteResult> routeResult, RoutingError error) {

            // If the route was calculated successfully
            if (error == RoutingError.NONE && routeResult.get(0).getRoute() != null) {
                Toast.makeText(mapsActivity.getApplicationContext(), "Calculating route finished!",
                        Toast.LENGTH_SHORT).show();

                // create a map route object and place it on the map
                route = routeResult.get(0).getRoute();
                mapRoute = new MapRoute(route);
                mapRoute.setColor(routeColor);

                map.addMapObject(mapRoute);

                // Get the bounding box containing the route and zoom in
                GeoBoundingBox boundingBox = route.getBoundingBox();
                map.zoomTo(boundingBox, Map.Animation.BOW,
                        Map.MOVE_PRESERVE_ORIENTATION);

                mapsActivity.routeCalculatedCallback();
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
            Toast.makeText(mapsActivity.getApplicationContext(), "Destination reached!", Toast.LENGTH_LONG)
                    .show();

            // Revert to default behavior
            navigationManager.setMapUpdateMode(NavigationManager.MapUpdateMode.NONE);
            navigationManager.setTrafficAvoidanceMode(NavigationManager.TrafficAvoidanceMode.DISABLE);
            navigationManager.setMap(null);
        }

        @Override
        public void onRouteUpdated(final Route updatedRoute) {
            // This does not happen on re-route
            Toast.makeText(mapsActivity.getApplicationContext(), "Your route was udated!", Toast.LENGTH_LONG)
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
            Toast.makeText(mapsActivity.getApplicationContext(), "Your route was updated!", Toast.LENGTH_LONG)
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
    public final NavigationManager.NewInstructionEventListener navigationNewInstructionListener = new NavigationManager.NewInstructionEventListener() {
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
    public void attachNavigationListeners() {
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
    public void detachNavigationListeners() {
        if (navigationManager != null) {
            navigationManager.removeRerouteListener(navigationRerouteListener);
            navigationManager.removeNavigationManagerEventListener(navigationListener);
            navigationManager
                    .removeNewInstructionEventListener(navigationNewInstructionListener);
            navigationManager.removePositionListener(navigationPositionListener);
        }
    }
}
