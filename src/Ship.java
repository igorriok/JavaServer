import java.time.LocalTime;

public class Ship {

    private double lat, lon;
    private LocalTime life;

    public Ship(double lat, double lon, LocalTime life){
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
