package Server;

import java.time.LocalTime;

public class Ship {

    private String ShipName;
    private double lat, lon;
    private LocalTime life;

    public Ship (String ShipName, double lat, double lon, LocalTime life) {
        this.ShipName = ShipName;
        this.lat = lat;
        this.lon = lon;
        this.life = life;
    }
    
    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public LocalTime getLife(){
        return life;
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
}
