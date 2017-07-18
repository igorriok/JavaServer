package Server;

import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.Math.*;

public class Server {


    public static void main(String[] args) throws IOException {

        //Scanner scanner = new Scanner(System.in);
        //String insert = scanner.nextLine();
        
        double R = 6378.1; //Radius of the Earth km
        double d = 0.05; //2000km/h
        ConcurrentLinkedQueue<Missile> missileList  = new ConcurrentLinkedQueue<Missile>();
        ConcurrentHashMap<String, Ship> shipList = new ConcurrentHashMap<String, Ship>();
        ConcurrentHashMap<String, ClientWorker> clients = new ConcurrentHashMap<>();
        ConcurrentLinkedQueue<Explosion> expList  = new ConcurrentLinkedQueue<Explosion>();
        double hitRadius = 0.05;
        ExecutorService executor = Executors.newCachedThreadPool();
        ExecutorService clientPool = Executors.newCachedThreadPool();

        //fot testing
        shipList.put("north", new Ship("north", 47.5, 28.8827, LocalTime.now(), 0));
        shipList.put("south", new Ship("south", 46.5, 28.8827, LocalTime.now(), 0));
        shipList.put("west", new Ship("west", 47, 28.1, LocalTime.now(), 0));
        shipList.put("est", new Ship("est", 47, 29.6, LocalTime.now(), 0));
        shipList.put("colonita", new Ship("colonita", 47.040885, 28.947728, LocalTime.now(), 0));

        //initialize database thread
        Fishies db = new Fishies();
        db.run();

        Timer shipLifeChecker = new Timer();
        shipLifeChecker.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                System.out.println("checking lifes" + shipList.toString());

                shipList.forEach((k, v) -> {
                    System.out.println(k + " last update " + ChronoUnit.MINUTES.between(v.getLife(), LocalTime.now()) + " minutes ago");
                    if (ChronoUnit.MINUTES.between(v.getLife(), LocalTime.now()) >= 3) {
                        shipList.remove(k);
                        System.out.println("removed:" + k);
                    }
                });
            }
        }, 60000, 60000);

        Timer expLifeChecker = new Timer();
        expLifeChecker.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                for (Explosion exp : expList) {
                    if (ChronoUnit.SECONDS.between(exp.getLife(), LocalTime.now()) >= 2) {
                        expList.remove(exp);
                        System.out.println("removed:" + exp);
                    }
                }
            }
        }, 500, 500);

        Timer missilePosCalc = new Timer();
        missilePosCalc.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                for(Missile missile : missileList) {

                    double bearing = Math.toRadians(missile.getBearing()); //Bearing is 90 degrees converted to radians.

                    double lat1 = Math.toRadians(missile.getLat()); //Current lat point converted to radians
                    double lon1 = Math.toRadians(missile.getLon()); //Current lon point converted to radians

                    double lat2 = Math.asin( sin(lat1) * cos(d/R) + cos(lat1) * sin(d/R) * cos(bearing));

                    double lon2 = lon1 + Math.atan2(sin(bearing) * sin(d/R) * cos(lat1), cos(d/R) - sin(lat1) * sin(lat2));

                    lat2 = Math.toDegrees(lat2);
                    lon2 = Math.toDegrees(lon2);

                    missile.setLat(lat2);
                    missile.setLon(lon2);
                }
            }
        }, 100, 100);
        
        Timer missileCheck = new Timer();
        missileCheck.scheduleAtFixedRate(new TimerTask() {
            ClientWorker fireID;
            ClientWorker hitedID;
            @Override
            public void run() {
                for (Missile missile : missileList) {
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            int missileID = missile.getID();
                            shipList.forEach((k, v) -> {

                                if (!k.equals(Integer.toString(missileID))) {

                                    double lat1 = Math.toRadians(missile.getLat());
                                    double lon1 = Math.toRadians(missile.getLon());

                                    double lat2 = Math.toRadians(v.getLat());
                                    double lon2 = Math.toRadians(v.getLon());
                                    // P
                                    double rho1 = R * cos(lat1);
                                    double z1 = R * sin(lat1);
                                    double x1 = rho1 * cos(lon1);
                                    double y1 = rho1 * sin(lon1);
                                    // Q
                                    double rho2 = R * cos(lat2);
                                    double z2 = R * sin(lat2);
                                    double x2 = rho2 * cos(lon2);
                                    double y2 = rho2 * sin(lon2);
                                    // Dot product
                                    double dot = (x1 * x2 + y1 * y2 + z1 * z2);
                                    double cos_theta = dot / (R * R);

                                    double theta = acos(cos_theta);
                                    // Distance in Metres
                                    double dist = R * theta;
                                    //System.out.println("checked");
                                    //check if missile is in hit radius and notify all participants
                                    if (dist <= hitRadius) {
                                        missileList.remove(missile);
                                        if (!v.getShield()) {
                                            //add explosion to list
                                            expList.add(new Explosion(missileID, missile.getLat(), missile.getLon(), LocalTime.now()));
                                            //update points
                                            db.updatePoints(missileID, 1);
                                            //notify participants of hit
                                            hitedID = clients.get(k);
                                            fireID = clients.get(Integer.toString(missileID));
                                            //create points update message to be sent to fired client
                                            if (fireID != null) {
                                                ArrayList<String> message = new ArrayList<>();
                                                message.add(ClientWorker.points);
                                                message.add(Integer.toString(db.getPointsByID(missileID)));
                                                message.add(v.getName());
                                                //send points through clientWorker by ID
                                                fireID.sendMessage(message);

                                                //create notification message to be sent to stricken player
                                                if (hitedID != null) {
                                                    ArrayList<String> hit = new ArrayList<>();
                                                    hit.add("hit");
                                                    hit.add((shipList.get(fireID)).getName());
                                                    hitedID.sendMessage(hit);
                                                }
                                            }
                                        }
                                    }
                                }
                            });
                            if (ChronoUnit.MINUTES.between(missile.getLife(), LocalTime.now()) >= 5 * 60) {
                                missileList.remove(missile);
                                System.out.println("removed:" + missile);
                            }
                        }
                    });
                }
            }
        }, 100, 100);
        
        Timer clientThreads = new Timer();
        clientThreads.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
              
                System.out.println("Online clients:" + clients.size());
            }
        }, 10*1000, 10*1000);

        System.out.println("Starting Sever Socket");

        ServerSocket server = null;

        try{
            server = new ServerSocket(57349, 1000);
            System.out.println("Waiting for a client...");

        } catch (IOException e) {
            System.out.println("Could not listen on port");
            System.exit(-1);
        }

        while(true) {

            ClientWorker w;
            try {
                //server.accept returns a client connection
                w = new ClientWorker(server.accept(), shipList, db, missileList, clients, expList);
                clientPool.execute(w);
                System.out.println("Client connected");
            } catch (IOException e) {
                System.out.println("Cant connect");
                //System.exit(-1);
            }
        }
    }
}
