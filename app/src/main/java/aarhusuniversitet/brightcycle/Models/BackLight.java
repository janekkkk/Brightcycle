package aarhusuniversitet.brightcycle.Models;

public class BackLight extends Light {

    public BackLight(Sensor accelerometer, Sensor lightSensor) {
        super(accelerometer, lightSensor);
    }

    @Override
    public void setBrightness(int brightness) {
        // TODO ask hardware guys if they are doing this on the microcontroller
        if(((LightSensor)lightSensor).darkOutside()){
            this.brightness = 75;
        }
        else this.brightness = 0;
    }

    public void turnOnBrakeLight() {
        if(((Accelerometer)accelerometer).isBreaking()){
            brightness = 100;
        }
    }
}

