package aarhusuniversitet.brightcycle.Domain;

public class Light {

    protected int brightness = 0;
    public boolean isOn = false;
    protected Sensor accelerometer;

    public Light(Sensor accelerometer) {
        this.accelerometer = accelerometer;
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
        this.brightness = brightness;
    }
}
