package Server;

import java.time.LocalTime;

public class Missile {

    private int ID;
    private double lat, lon, bearing;
    private LocalTime life;

    Missile(int ID, double bearing, double lat, double lon, LocalTime life) {
        this.ID = ID;
        this.bearing = bearing;
        this.lat = lat;
        this.lon = lon;
        this.life = life;
    }
    
    int getID() {
        return ID;
    }
    
    double getBearing() {
        return bearing;
    }

    double getLat() {
        return lat;
    }

    double getLon() {
        return lon;
    }

    LocalTime getLife(){
        return life;
    }

    void setLat(double lat) {
        this.lat = lat;
    }

    void setLon(double lon) {
        this.lon = lon;
    }

    void setLife(LocalTime life) {
        this.life = life;
    }
}
