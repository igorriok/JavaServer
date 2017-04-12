package Server;

import java.time.LocalTime;
import java.util.concurrent.ConcurrentHashMap;


public class Ships extends Thread {

    private ConcurrentHashMap<String, Ship> shipList;


    protected void addShip(String ID, Ship ship) {
        if(shipList.containsKey(ID)) {
            shipList.replace(ID, ship);
        } else {
            shipList.put(ID, ship);
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
        shipList.put("north", new Ship("north", 47.5, 29, LocalTime.now()));
        shipList.put("south", new Ship("south", 46.5, 29, LocalTime.now()));
        shipList.put("west", new Ship("west", 47, 28.1, LocalTime.now()));
        shipList.put("est", new Ship("est", 47, 29.6, LocalTime.now()));
    }

}
