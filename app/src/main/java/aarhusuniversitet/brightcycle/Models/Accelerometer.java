package aarhusuniversitet.brightcycle.Models;

public class Accelerometer implements Sensor {

    private final int MAX_VALUE = 100;
    private final int MIN_VALUE = 0;

    @Override
    public int read() {
        return 0;
    }

    public boolean isOutOfTurn(){
        if(read() < MAX_VALUE){
            return true;
        }
        else return false;
    }

    public boolean isBreaking(){
        return false;
    }
}
