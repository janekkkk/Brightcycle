package aarhusuniversitet.brightcycle.Models;

import com.here.android.mpa.common.GeoCoordinate;

public class GeoLocation {
    private String address;
    private double longitude, latitude;

    public GeoLocation(double longitude, double latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
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
