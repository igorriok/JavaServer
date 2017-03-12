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
    }

}
