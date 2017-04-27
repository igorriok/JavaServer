package Server;

import java.time.LocalTime;

public class Explosion {

    private int ID;
    private double lat, lon;
    private LocalTime life;

    Explosion(int ID, double lat, double lon, LocalTime life) {
        this.ID = ID;
        this.lat = lat;
        this.lon = lon;
        this.life = life;
    }
    
    int getID() {
        return ID;
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
