package aarhusuniversitet.brightcycle.Domain;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.here.android.mpa.common.GeoCoordinate;

@Table(name = "SavedLocations")
public class GeoLocation extends Model {

    @Column(name = "Address")
    private String address;
    @Column(name = "Latitude")
    public double latitude;
    @Column(name = "Longitude")
    public double longitude;

    public GeoLocation(double latitude, double longitude) {
        super();
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public GeoLocation() {
        super();
    }

    // Database call
    public static GeoLocation getLastLocation() {
        return new Select().from(GeoLocation.class).executeSingle();
    }

    public GeoLocation(GeoCoordinate geoCoordinate) {
        this.latitude = geoCoordinate.getLatitude();
        this.longitude = geoCoordinate.getLongitude();
    }

    public GeoCoordinate getCoordinate() {
        return new GeoCoordinate(latitude, longitude);
    }

    public void setCoordinate(GeoCoordinate geoCoordinate) {
        this.latitude = geoCoordinate.getLatitude();
        this.longitude = geoCoordinate.getLongitude();
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }
}
