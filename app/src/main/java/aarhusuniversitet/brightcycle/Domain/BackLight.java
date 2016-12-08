package aarhusuniversitet.brightcycle.Domain;

public class BackLight extends Light {

    public boolean automaticLight = true;
    public boolean brakeLightOn = false;

    public BackLight(Sensor accelerometer) {
        super(accelerometer);
    }

    public void turnOnBrakeLight() {
        brakeLightOn = true;
    }

    public void turnOffBrakeLight() {
        brakeLightOn = false;
    }
}

