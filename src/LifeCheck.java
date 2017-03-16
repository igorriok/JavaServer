
public class LifeCheck extends Thread {
    
    private ConcurrentHashMap<String, Ship> shipList;
    
    public LifeCheck(ConcurrentHashMap<String, Ship> shipList) {
        this.shipList = shipList;
    }

    public void run() {
        while(true) {
            shipList.forEach((k,v) -> if(v.getLife().compareTo(LocalDateTime.now()) > 60000) {
                shipList.remove(k)
            });
            Thread.sleep(60000);
        }

    }
}
