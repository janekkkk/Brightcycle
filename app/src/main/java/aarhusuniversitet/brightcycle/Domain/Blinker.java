package aarhusuniversitet.brightcycle.Domain;

public class Blinker extends Light {

    public boolean blinking = false;
    public String direction;

    public Blinker(Sensor accelerometer, Sensor lightSensor) {
        super(accelerometer, lightSensor);
    }

    public void blink(String direction) {
        this.direction = direction;
        blinking = true;
    }

    public void stopBlink() {
        blinking = false;
    }
}
