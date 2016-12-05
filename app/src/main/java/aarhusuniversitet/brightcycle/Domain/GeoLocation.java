package aarhusuniversitet.brightcycle.Domain;

import com.here.android.mpa.common.GeoCoordinate;

public class GeoLocation {
    private String address;
    private double longitude, latitude;

    public GeoLocation(double latitude, double longitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public GeoLocation() {

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
