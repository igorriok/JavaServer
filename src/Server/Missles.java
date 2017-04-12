package Server;

import java.time.LocalTime;
import java.util.concurrent.ConcurrentHashMap;


public class Missles extends Thread {

    private ConcurrentHashMap<String, Ship> missleList;


    protected void addMissle(int ID, Ship location) {
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
        shipList.put("north", new Ship(47.5, 29, LocalTime.now()));
        shipList.put("south", new Ship(46.5, 29, LocalTime.now()));
        shipList.put("west", new Ship(47, 28.1, LocalTime.now()));
        shipList.put("est", new Ship(47, 29.6, LocalTime.now()));
    }

}
