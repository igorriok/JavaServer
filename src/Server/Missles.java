package Server;

import java.time.LocalTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;


public class Missles extends Thread {

    private ConcurrentLinkedQueue<Missle> missleList;


    protected void addMissle(Missle missle) {
        missleList.add(missle);
    }
    
    protected ConcurrentLinkedQueue getMissles() {
        return missleList;
    }
    
}
