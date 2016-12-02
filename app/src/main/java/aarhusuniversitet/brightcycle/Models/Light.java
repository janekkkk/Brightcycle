package aarhusuniversitet.brightcycle.Models;

public class Light {

    private int brightness = 0;
    private boolean lightOn = false;
    protected Sensor accelerometer;
    protected Sensor lightSensor;

    public Light(Sensor accelerometer, Sensor lightSensor) {
        this.accelerometer = accelerometer;
        this.lightSensor = lightSensor;
    }

    public void toggle() {
        if (lightOn) {
            turnOff();
        } else {
            turnOff();
        }
    }

    public void turnOn() {
        lightOn = true;
    }

    public void turnOff() {
        lightOn = false;
    }

    public void setBrightness(int brightness) {
        this.brightness = brightness;
    }

    public int getBrightness() {
        return brightness;
    }
}
