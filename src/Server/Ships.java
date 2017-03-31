package Server;

import java.time.LocalTime;
import java.util.concurrent.ConcurrentHashMap;


public class Ships extends Thread {

    private ConcurrentHashMap<String, Ship> shipList;


    protected void addShip(String ID, Ship location) {
        if(shipList.containsKey(ID)) {
            shipList.replace(ID, location);
        } else {
            shipList.put(ID, location);
        }
    }

    protected void removeShip(String ID) {
        if(shipList.containsKey(ID)) {
            shipList.remove(shipList.get(ID));
        }
    }
    
    protected ConcurrentHashMap getShips() {
        return shipList;
    }

    public void run(){
        shipList = new ConcurrentHashMap<String, Ship>();
        shipList.put("north", new Ship(47.5, 28.9, LocalTime.now()));
        shipList.put("south", new Ship(47.7, 29, LocalTime.now()));
        shipList.put("west", new Ship(46.98, 28.1, LocalTime.now()));
        shipList.put("est", new Ship(46.98, 29.6, LocalTime.now()));
    }

}
