import java.awt.*;
import java.awt.geom.Point2D;
import java.util.concurrent.ConcurrentHashMap;


public class Ships extends Thread {

    private ConcurrentHashMap<String, Point2D.Double> shipList;

    public Ships() {};

    protected void addShip(String ID, Point2D.Double location) {
        if(shipList.containsKey(ID)) {
            shipList.replace(ID, location);
        } else {
            shipList.put(ID, location);
        }
    }
    
    protected ConcurrentHashMap getShips() {
        return shipList;
    }

    public void run(){
        shipList = new ConcurrentHashMap<String, Point2D.Double>();
    }

}
