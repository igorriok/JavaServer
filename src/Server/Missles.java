package Server;

import java.time.LocalTime;
import java.util.concurrent.ConcurrentHashMap;


public class Missles extends Thread {

    private ConcurrentLinkedQueue<Ship> missleList;


    protected void addMissle(Ship ship) {
        missleList.add(ship);
    }
    
    protected ConcurrentLinkedQueue getMissles() {
        return missleList;
    }
    
}
