package aarhusuniversitet.brightcycle.Domain;

import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import timber.log.Timber;

public class Accelerometer implements Sensor {

    private float mLastX;
    private float mLastY;
    private float mLastZ;
    private final float NOISE = (float) 2.0;

    @Override
    public int read() {
        return 0;
    }

    public boolean isOutOfTurn(){
        return false;
    }

    public boolean isBreaking(){
        return false;
    }

    public SensorEventListener accelerometerSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            float deltaX = Math.abs(mLastX - x);
            float deltaY = Math.abs(mLastY - y);
            float deltaZ = Math.abs(mLastZ - z);
            if (deltaX < NOISE) deltaX = (float) 0.0;
            if (deltaY < NOISE) deltaY = (float) 0.0;
            if (deltaZ < NOISE) deltaZ = (float) 0.0;
            mLastX = x;
            mLastY = y;
            mLastZ = z;

            if (deltaX > deltaY) {
                Timber.d("horizontal movement");
            } else if (deltaY > deltaX) {
                Timber.d("vertical movement");
            } else {
            }

        }

        @Override
        public void onAccuracyChanged(android.hardware.Sensor sensor, int accuracy) {
            Timber.d(sensor.toString() + " - " + accuracy);
        }
    };
}