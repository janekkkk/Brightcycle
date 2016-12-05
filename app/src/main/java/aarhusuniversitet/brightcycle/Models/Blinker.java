package aarhusuniversitet.brightcycle.Models;

public class Blinker extends Light {

    public boolean blinking = false;

    public Blinker(Sensor accelerometer, Sensor lightSensor) {
        super(accelerometer, lightSensor);
    }

    public void blink() {

    }
}
