package aarhusuniversitet.brightcycle.Controller;

import android.app.Activity;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.ArrayList;

import aarhusuniversitet.brightcycle.BluetoothConnection;
import aarhusuniversitet.brightcycle.Domain.Accelerometer;
import aarhusuniversitet.brightcycle.Domain.BackLight;
import aarhusuniversitet.brightcycle.Domain.Blinker;
import aarhusuniversitet.brightcycle.Domain.GeoLocation;
import aarhusuniversitet.brightcycle.Domain.Light;
import aarhusuniversitet.brightcycle.Domain.LightSensor;
import aarhusuniversitet.brightcycle.Domain.Sensor;
import timber.log.Timber;

public class DrivingInformation {

    public Light leftBlinker, rightBlinker;
    public Light backLight;
    public Sensor lightSensor;
    public Sensor accelerometer;
    public GeoLocation currentLocation;
    public GeoLocation savedBikeLocation;
    public GeoLocation mockLocation;

    protected Activity activity;
    public ArrayList<Light> lights;
    protected BluetoothConnection bluetoothConnection;
    private static DrivingInformation instance;

    private final String LEFT_BLINKER = "l";
    private final String RIGHT_BLINKER = "r";
    private final String BACK_LIGHT_BRAKE = "s";
    private final String BACK_LIGHT_AUTOMATIC = "a";
    private final String BACK_LIGHT_MANUAL = "m";

    public DrivingInformation(Activity activity, BluetoothConnection bluetoothConnection) {
        this.activity = activity;
        this.bluetoothConnection = bluetoothConnection;
        initialize();
    }

    public static DrivingInformation getInstance(Activity activity, BluetoothConnection bluetoothConnection) {
        if (instance == null) {
            instance = new DrivingInformation(activity, bluetoothConnection);
        }
        return instance;
    }

    public void saveLocationBike() {
        savedBikeLocation = currentLocation;
        savedBikeLocation.save();
    }

    public void turnOffLights() {
        for (Light light :
                lights) {
            if (light instanceof BackLight) {
                light.turnOff();
            }
        }

        bluetoothConnection.sendData(BACK_LIGHT_MANUAL + Integer.toString(100));
    }

    public void turnOnLights() {
        for (Light light :
                lights) {
            if (light instanceof BackLight) {
                light.turnOn();
            }
        }

        bluetoothConnection.sendData(BACK_LIGHT_MANUAL + Integer.toString(0));
    }

    public void setBrightnessLights(int brightness) {
        for (Light light :
                lights) {
            if (light instanceof BackLight) {
                light.setBrightness(brightness);
            }
        }

        bluetoothConnection.sendData(BACK_LIGHT_MANUAL + Integer.toString(brightness));
    }

    public void startBlinking(String direction) {
        if (direction.equals(RIGHT_BLINKER)) {
            ((Blinker) rightBlinker).blink();
        } else ((Blinker) leftBlinker).blink();

        bluetoothConnection.sendData(direction);
    }

    public void stopBlinking(String direction) {
        if (((Accelerometer) accelerometer).isOutOfTurn()) {
            for (Light light :
                    lights) {
                if (light instanceof Blinker && ((Blinker) light).blinking) {
                    ((Blinker) light).stopBlink();
                }
            }
            bluetoothConnection.sendData(direction);
        }
    }

    private void initialize() {
        initializeLocations();
        initializeSensors();
        initializeLights();
    }

    private void initializeLocations() {
        mockLocation = new GeoLocation(56.14703396, 10.20783076);
        savedBikeLocation = new GeoLocation();
        currentLocation = mockLocation;
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
