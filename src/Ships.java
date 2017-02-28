import java.awt.*;
import java.util.concurrent.ConcurrentHashMap;


public class Ships extends Thread {

    private ConcurrentHashMap shipList;

    public Ships (){};

    protected void addShip(String ID, Point location) {
        if(shipList.containsKey(ID)) {
            shipList.replace(ID, location);
        } else {
            shipList.put(ID, location);
        }
    }

    public void run(){
        shipList = new ConcurrentHashMap<String, Point>();
    }

}
