package Server;

import java.util.concurrent.ConcurrentLinkedQueue;


public class Missles extends Thread {

    private ConcurrentLinkedQueue<Missle> missleList;

    public void run() {
        missleList = new ConcurrentLinkedQueue<Missle>();
    }

    protected void addMissle(Missle missle) {
        missleList.add(missle);
    }
    
    protected ConcurrentLinkedQueue getMissles() {
        return missleList;
    }
    
}
