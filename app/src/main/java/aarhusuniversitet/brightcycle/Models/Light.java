package aarhusuniversitet.brightcycle.Models;

public class Light {

    private int brightness;
    protected Sensor accelerometer;
    protected Sensor lightSensor;

    public Light(Sensor accelerometer, Sensor lightSensor) {
        this.accelerometer = accelerometer;
        this.lightSensor = lightSensor;
    }

    public void toggle() {

    }

    public void turnOn() {

    }

    public void turnOff() {

    }
}
