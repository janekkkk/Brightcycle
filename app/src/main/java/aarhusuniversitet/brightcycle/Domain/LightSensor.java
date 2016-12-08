package aarhusuniversitet.brightcycle.Domain;

public class LightSensor implements Sensor {

    @Override
    public int read() {
        return 0;
    }

    public boolean darkOutside(){
        return read() > 50;
    }

}
