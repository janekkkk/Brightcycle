package aarhusuniversitet.brightcycle.Domain;

public class BackLight extends Light {

    public boolean automaticLight = true;

    public BackLight(Sensor accelerometer, Sensor lightSensor) {
        super(accelerometer, lightSensor);
    }

    public void turnOnBrakeLight() {
        brightness = 100;
    }
}

