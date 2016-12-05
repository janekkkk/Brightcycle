package aarhusuniversitet.brightcycle;

import android.app.Activity;

import com.here.android.mpa.common.GeoCoordinate;

import java.util.ArrayList;

import aarhusuniversitet.brightcycle.Models.Accelerometer;
import aarhusuniversitet.brightcycle.Models.BackLight;
import aarhusuniversitet.brightcycle.Models.Blinker;
import aarhusuniversitet.brightcycle.Models.GeoLocation;
import aarhusuniversitet.brightcycle.Models.Light;
import aarhusuniversitet.brightcycle.Models.LightSensor;
import aarhusuniversitet.brightcycle.Models.Sensor;

public class DrivingInformation {

    public Light leftBlinker, rightBlinker;
    public Light backLight;
    public Sensor lightSensor;
    public Sensor accelerometer;
    public GeoLocation currentLocation;
    public GeoLocation savedBikeLocation;
    public ArrayList<GeoLocation> savedLocations;
    public GeoCoordinate mockLocation;

    protected Activity activity;
    public ArrayList<Light> lights;
    protected BluetoothConnection bluetoothConnection;
    private static DrivingInformation drivingInformation;

    public DrivingInformation(Activity activity, BluetoothConnection bluetoothConnection) {
        this.activity = activity;
        this.bluetoothConnection = bluetoothConnection;
        initialize();
    }

    public static DrivingInformation getInstance(Activity activity, BluetoothConnection bluetoothConnection) {
        if (drivingInformation == null) {
            drivingInformation = new DrivingInformation(activity, bluetoothConnection);
            return drivingInformation;
        } else {
            return drivingInformation;
        }
    }

    public void turnOffLights() {
        for (Light light :
                lights) {
            light.turnOff();
        }

        bluetoothConnection.sendData("allLights", 0);
    }

    public void turnOnLights() {
        for (Light light :
                lights) {
            light.turnOn();
        }

        bluetoothConnection.sendData("allLights", 100);
    }

    public void setBrightnessLights(int brightness) {
        for (Light light :
                lights) {
            light.setBrightness(brightness);
        }

        bluetoothConnection.sendData("allLights", brightness);
    }

    public void startBlinking(String direction) {
        if (direction.equals("right")) {
            ((Blinker) rightBlinker).blink();
        }
        else ((Blinker) leftBlinker).blink();

        bluetoothConnection.sendData(direction, 1);
    }

    public void stopBlinking() {
        if (((Accelerometer) accelerometer).isOutOfTurn()) {
            for (Light light :
                    lights) {
                if (light instanceof Blinker && ((Blinker) light).blinking) {
                    ((Blinker) light).stopBlink();
                    bluetoothConnection.sendData("rightBlinker", 1);
                }
            }
        }
    }

    private void initialize() {
        initializeLocations();
        initializeSensors();
        initializeLights();
    }

    private void initializeLocations() {
        savedLocations = new ArrayList<>();
        mockLocation = new GeoCoordinate(56.14703396, 10.20783076);
        savedBikeLocation = new GeoLocation();
        currentLocation = new GeoLocation(mockLocation);
    }

    private void initializeSensors() {
        lightSensor = new LightSensor();
        accelerometer = new Accelerometer();
    }

    private void initializeLights() {
        leftBlinker = new Blinker(accelerometer, lightSensor);
        rightBlinker = new Blinker(accelerometer, lightSensor);
        backLight = new BackLight(accelerometer, lightSensor);

        lights = new ArrayList<>();
        lights.add(leftBlinker);
        lights.add(rightBlinker);
        lights.add(backLight);
    }
}
