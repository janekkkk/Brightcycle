package aarhusuniversitet.brightcycle.Models;

public class Light {

    protected int brightness = 0;
    protected final int MIN_LIGHT_VALUE = 0;
    protected final int MAX_LIGHT_VALUE = 100;
    public boolean isOn = false;
    protected Sensor accelerometer;
    protected Sensor lightSensor;

    public Light(Sensor accelerometer, Sensor lightSensor) {
        this.accelerometer = accelerometer;
        this.lightSensor = lightSensor;
    }

    public void toggle() {
        if (isOn) {
            turnOff();
        } else {
            turnOn();
        }
    }

    public void turnOn() {
        isOn = true;
    }

    public void turnOff() {
        isOn = false;
    }

    public void setBrightness(int brightness) {
        if (brightness <= MIN_LIGHT_VALUE) {
            isOn = false;
        } else isOn = true;
        this.brightness = brightness;
    }
}
