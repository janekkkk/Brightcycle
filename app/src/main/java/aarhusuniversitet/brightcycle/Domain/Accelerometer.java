package aarhusuniversitet.brightcycle.Domain;

import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import aarhusuniversitet.brightcycle.Controller.DrivingInformation;
import timber.log.Timber;

public class Accelerometer implements Sensor {

    private float mLastX;
    private float mLastY;
    private float mLastZ;
    private final float NOISE = (float) 2.0; //m/s^2
    private DrivingInformation drivingInformation;

    public Accelerometer(DrivingInformation drivingInformation){
        this.drivingInformation = drivingInformation;
    }

    @Override
    public int read() {
        return 0;
    }

    public void isOutOfTurn(){
        drivingInformation.stopBlinking();
    }

    public void isBraking(){
        drivingInformation.turnOnBrakeLights();
    }

    public void stoppedBraking(){
        drivingInformation.turnOffBrakeLights();
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

            //The user has to make a turn with an angle ca. 15° --> unit is m/s^2
            if ((deltaX > 2.5) || (deltaX < -2.5)){
                //drivingInformation.stopBlinking("b");
                Timber.d("blinker off");
            }

            //Assumption: the user is breaking with a deceleration around 3m/s^2 --> thesis
            if (deltaY > 2.3){ //phone mounted in an angle of ~40°
                Timber.d("stoplight on");
            }
        }

        @Override
        public void onAccuracyChanged(android.hardware.Sensor sensor, int accuracy) {
            Timber.d(sensor.toString() + " - " + accuracy);
        }
    };
}
