import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.ConcurrentHashMap;

public class LifeCheck extends Thread {
    
    private ConcurrentHashMap<String, Ship> shipList;
    
    public LifeCheck(ConcurrentHashMap<String, Ship> shipList) {
        this.shipList = shipList;
    }

    public void run() {
        while(true) {
            shipList.forEach((k,v) -> {
                if(v.getLife().compareTo(LocalTime.now()) > 60000) {
                    shipList.remove(k);
                }
            });
            try {
                Thread.sleep(60000);
            } catch (Exception e) {
                System.out.print(e);
            }
        }

    }
}
