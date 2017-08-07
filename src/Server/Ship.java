package Server;

import java.time.LocalTime;

public class Ship {

    private String ShipName;
    private double lat, lon, bearing;
    private LocalTime life;
    private boolean shield;

    Ship(String ShipName, double lat, double lon, LocalTime life, double bearing) {
        this.ShipName = ShipName;
        this.lat = lat;
        this.lon = lon;
        this.life = life;
        this.bearing = bearing;
        this.shield = false;
    }

    String getName() {
        return ShipName;
    }
    
    double getLat() {
        return lat;
    }

    double getLon() {
        return lon;
    }

    LocalTime getLife() {
        return life;
    }
  
    double getBearing() {
        return bearing;
    }
  
    boolean getShield() {
        return shield;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public void setLife(LocalTime life) {
        this.life = life;
    }
  
    public void setShield(boolean shield) {
        this.shield = shield;
    }
}
