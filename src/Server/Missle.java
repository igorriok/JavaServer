package Server;

import java.time.LocalTime;

public class Missle {

    private int ID;
    private double lat, lon;
    private LocalTime life;

    public Missle(int ID, double lat, double lon, LocalTime life) {
        this.ID = ID;
        this.lat = lat;
        this.lon = lon;
        this.life = life;
    }
    
    public double getID() {
        return ID;
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
