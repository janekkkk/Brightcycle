package aarhusuniversitet.brightcycle.Controller;

import android.app.Activity;

import java.util.ArrayList;

import aarhusuniversitet.brightcycle.BluetoothConnection;
import aarhusuniversitet.brightcycle.Domain.Accelerometer;
import aarhusuniversitet.brightcycle.Domain.BackLight;
import aarhusuniversitet.brightcycle.Domain.Blinker;
import aarhusuniversitet.brightcycle.Domain.GeoLocation;
import aarhusuniversitet.brightcycle.Domain.Light;
import aarhusuniversitet.brightcycle.Domain.Sensor;

public class DrivingInformation {

    public Light leftBlinker, rightBlinker;
    public Light backLight;
    public Sensor accelerometer;
    public GeoLocation currentLocation;
    public GeoLocation savedBikeLocation;
    public GeoLocation mockLocation;

    protected Activity activity;
    public ArrayList<Light> lights;
    protected BluetoothConnection bluetoothConnection;
    private static DrivingInformation instance;

    // List of device commands to send to the microcontroller.
    private final String LEFT_BLINKER = "l";
    private final String RIGHT_BLINKER = "r";
    private final String STOP_BLINKERS = "b";
    private final String BACK_LIGHT_AUTOMATIC = "a";
    private final String BACK_LIGHT_MANUAL = "m";
    private final String BACK_LIGHT_BRAKE_ON = "s";
    private final String BACK_LIGHT_BRAKE_OFF = "o";


    public DrivingInformation(Activity activity, BluetoothConnection bluetoothConnection) {
        this.activity = activity;
        this.bluetoothConnection = bluetoothConnection;
        initialize();
    }

    /**
     * Gets the only instance of the drivingInformation there is or when there is  creates a new one.
     * Also called singleton pattern.
     *
     * @param activity            the current activity
     * @param bluetoothConnection the bluetooth connection to the microcontroller
     * @return the only drivingInformation instance
     */
    public static DrivingInformation getInstance(Activity activity, BluetoothConnection bluetoothConnection) {
        if (instance == null) {
            instance = new DrivingInformation(activity, bluetoothConnection);
        }
        return instance;
    }

    /**
     * Saves the location of your bike to the database. Will occur when the phone disconnects from the microcontroller.
     */
    public void saveLocationBike() {
        savedBikeLocation = currentLocation;
        savedBikeLocation.save();
    }

    /**
     * Will turn off all the light(s) except the blinkers.
     */
    public void turnOffLights() {
        for (Light light : lights) {

            if (light instanceof BackLight) {
                light.turnOff();

                if (((BackLight) light).automaticLight) { // When the brightness of the light is handled by the light sensor
                    ((BackLight) light).automaticLight = false;
                    bluetoothConnection.sendData(BACK_LIGHT_AUTOMATIC);
                } else {
                    bluetoothConnection.sendData(BACK_LIGHT_MANUAL, 0);
                }
            }
        }
    }

    /**
     * Turns on the lights based on the surrounding light captured by the light sensor.
     */
    public void turnOnLightsAutomatically() {
        for (Light light : lights) {
            if (light instanceof BackLight) {
                light.turnOn();
                ((BackLight) light).automaticLight = true;
                bluetoothConnection.sendData(BACK_LIGHT_AUTOMATIC);

            }
        }
    }

    /**
     * Sets the brightness of the light(s).
     *
     * @param brightness brightness to be set
     */
    public void setBrightnessLights(int brightness) {
        for (Light light : lights) {
            if (light instanceof BackLight) {
                light.setBrightness(brightness);
            }
        }

        bluetoothConnection.sendData(brightness);
    }

    /**
     * Start blinking in the given direction.
     *
     * @param direction which blinker to start blinking
     */
    public void startBlinking(String direction) {
        if (direction.equals(RIGHT_BLINKER)) {
            ((Blinker) rightBlinker).blink(direction);
        } else ((Blinker) leftBlinker).blink(direction);

        bluetoothConnection.sendData(direction);
    }

    /**
     * Stops the blinking blinker.
     */
    public void stopBlinking() {
        for (Light light : lights) {

            if (light instanceof Blinker) {
                ((Blinker) light).stopBlink();
            }
        }

        bluetoothConnection.sendData(STOP_BLINKERS);
    }

    /**
     * Turns on the brake light(s).
     */
    public void turnOnBrakeLights() {
        for (Light light : lights) {
            if (light instanceof BackLight) {
                ((BackLight) light).turnOnBrakeLight();
            }
        }

        bluetoothConnection.sendData(BACK_LIGHT_BRAKE_ON);
    }

    /**
     * Turns off the brake light(s).
     */
    public void turnOffBrakeLights() {
        for (Light light : lights) {
            if (light instanceof BackLight) {
                ((BackLight) light).turnOffBrakeLight();
            }
        }

        bluetoothConnection.sendData(BACK_LIGHT_BRAKE_OFF);
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
        accelerometer = new Accelerometer(this);
    }

    private void initializeLights() {
        leftBlinker = new Blinker(accelerometer);
        rightBlinker = new Blinker(accelerometer);
        backLight = new BackLight(accelerometer);

        lights = new ArrayList<>();
        lights.add(leftBlinker);
        lights.add(rightBlinker);
        lights.add(backLight);
    }

    public void turnOnLightsManually() {
        bluetoothConnection.sendData(BACK_LIGHT_MANUAL);
    }
}
