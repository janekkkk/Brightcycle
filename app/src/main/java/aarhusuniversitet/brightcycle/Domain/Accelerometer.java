package aarhusuniversitet.brightcycle.Domain;

public class Accelerometer implements Sensor {

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
}
