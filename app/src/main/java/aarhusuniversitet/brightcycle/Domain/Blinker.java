package aarhusuniversitet.brightcycle.Domain;

public class Blinker extends Light {

    public boolean blinking = false;

    public Blinker(Sensor accelerometer, Sensor lightSensor) {
        super(accelerometer, lightSensor);
    }

    public void blink() {

    }

    public void stopBlink() {
        blinking = false;
    }
}
