package aarhusuniversitet.brightcycle;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.holder.ImageHolder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public class HereMapsActivity extends AppCompatActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    // map embedded in the map fragment
    private Map map = null;

    // map fragment embedded in this activity
    private MapFragment mapFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_here_maps);

        ButterKnife.bind(this);
        Timber.plant(new Timber.DebugTree());

        createActionBar();
        createAppDrawer();
        initHereMaps();
    }

    private void initHereMaps() {
        // Search for the map fragment to finish setup by calling init().
        mapFragment = (MapFragment) getFragmentManager().findFragmentById(
                R.id.mapfragment);
        mapFragment.init(error -> {
            if (error == OnEngineInitListener.Error.NONE) {
                // retrieve a reference of the map from the map fragment
                map = mapFragment.getMap();
                // Set the map center to the Vancouver region (no animation)
                map.setCenter(new GeoCoordinate(49.196261, -123.004773, 0.0),
                        Map.Animation.NONE);
                // Set the zoom level to the average between min and max
                map.setZoomLevel(
                        (map.getMaxZoomLevel() + map.getMinZoomLevel()) / 2);
            } else {
                Timber.d("Initializing Here Maps Failed...")
            }
        });
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
