package aarhusuniversitet.brightcycle.Domain;

import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

public class Accelerometer implements Sensor, SensorEventListener {

    private final int MAX_VALUE = 100;
    private final int MIN_VALUE = 0;

    @Override
    public int read() {
        return 0;
    }

    public boolean isOutOfTurn(){
        return read() <= MAX_VALUE;
    }

    public boolean isBreaking(){
        return false;
    }

    // TODO implement accelerometer
    @Override
    public void onSensorChanged(SensorEvent event) {

    }

    @Override
    public void onAccuracyChanged(android.hardware.Sensor sensor, int accuracy) {

    }
}
