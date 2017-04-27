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

import static java.lang.Math.*;

public class Server {


    public static void main(String[] args) throws IOException {

        //Scanner scanner = new Scanner(System.in);
        //String insert = scanner.nextLine();
        
        double R = 6378.1; //Radius of the Earth km
        double d = 0.07; //500km/h in 0.5 sec
        ConcurrentLinkedQueue<Missile> missileList  = new ConcurrentLinkedQueue<Missile>();
        ConcurrentHashMap<String, Ship> shipList = new ConcurrentHashMap<String, Ship>();
        ConcurrentHashMap<String, ClientWorker> clients = new ConcurrentHashMap<>();
        ConcurrentLinkedQueue<Explosion> expList  = new ConcurrentLinkedQueue<Explosion>();


        shipList.put("north", new Ship("north", 47.5, 29, LocalTime.now()));
        shipList.put("south", new Ship("south", 46.5, 29, LocalTime.now()));
        shipList.put("west", new Ship("west", 47, 28.1, LocalTime.now()));
        shipList.put("est", new Ship("est", 47, 29.6, LocalTime.now()));
        shipList.put("colonita", new Ship("colonita", 47.040885, 28.947728, LocalTime.now()));

        Fishies db = new Fishies();
        db.run();

        Timer shipLifeChecker = new Timer();
        shipLifeChecker.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                System.out.println("checking lifes" + shipList.toString());

                shipList.forEach((k, v) -> {
                    System.out.println(ChronoUnit.MINUTES.between(v.getLife(), LocalTime.now()));
                    if (ChronoUnit.MINUTES.between(v.getLife(), LocalTime.now()) >= 20) {
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

                    double lat2 = Math.asin( sin(lat1)* cos(d/R) + cos(lat1)* sin(d/R)* cos(bearing));

                    double lon2 = lon1 + Math.atan2(sin(bearing)* sin(d/R)* cos(lat1), cos(d/R)- sin(lat1)* sin(lat2));

                    lat2 = Math.toDegrees(lat2);
                    lon2 = Math.toDegrees(lon2);

                    missile.setLat(lat2);
                    missile.setLon(lon2);
                }
            }
        }, 500, 500);
        
        Timer missileCheck = new Timer();
        missileCheck.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                for(Missile missile : missileList) {

                    int missleID = missile.getID();
                    shipList.forEach((k, v) -> {

                        if(!k.equals(Integer.toString(missleID))) {

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

                            if (dist <= 0.4) {
                                expList.add(new Explosion(missleID, missile.getLat(), missile.getLon(), LocalTime.now()));
                                missileList.remove(missile);
                                db.updatePoints(missleID, 1);
                                //create points update
                                ArrayList<String> message = new ArrayList<>();
                                message.add(ClientWorker.points);
                                message.add(Integer.toString(db.getPointsByID(missleID)));
                                //send points through clientWorker by ID
                                clients.get(Integer.toString(missleID)).sendMessage(message);
                            }
                        }

                        if (ChronoUnit.MINUTES.between(missile.getLife(), LocalTime.now()) >= 20) {
                            missileList.remove(missile);
                            System.out.println("removed:" + missile);
                        }
                    });
                }
            }
        }, 500, 500);

        System.out.println("Starting Sever Socket");

        ServerSocket server = null;

        try{
            server = new ServerSocket(57349);
            System.out.println("Waiting for a client...");

        } catch (IOException e) {
            System.out.println("Could not listen on port");
            System.exit(-1);
        }

        while(true){

            ClientWorker w;

            try {
                //server.accept returns a client connection
                w = new ClientWorker(server.accept(), shipList, db, missileList, clients, expList);
                Thread t = new Thread(w);
                t.start();
                System.out.println("Client connected");
            } catch (IOException e) {
                System.out.println("Cant connect");
                //System.exit(-1);
            }
        }

    }


    static class ClientWorker implements Runnable {

        private Socket client;
        private final static String id = "id";
        private final static String shipMsg = "ship";
        private final static String missile = "missile";
        private final static String missileArray = "missileArray";
        private final static String points = "points";
        private ArrayList<String> line;
        private ArrayList<String> response;
        private String head;
        private Fishies db;
        private ConcurrentHashMap<String, Ship> shipsHashMap;
        private ConcurrentLinkedQueue<Missile> missileList;
        private ConcurrentHashMap<String, ClientWorker> clients;
        private ConcurrentLinkedQueue<Explosion> expList;
        ObjectOutputStream out;
        int ID = 0;

        //Constructor
        ClientWorker(Socket client, ConcurrentHashMap<String, Ship> ships, Fishies db, ConcurrentLinkedQueue<Missile> missiles,
                     ConcurrentHashMap<String, ClientWorker> clients, ConcurrentLinkedQueue<Explosion> expList) {
            this.missileList = missiles;
            this.client = client;
            this.shipsHashMap = ships;
            this.db = db;
            this.clients = clients;
            this.expList = expList;
        }

        void sendMessage(ArrayList message) {
            try {
                out.writeObject(message);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Cant send message, " + e);
            }
        }

        public void run() {

            ObjectInputStream in = null;
            out = null;

            System.out.println("Client running");

            try {
                in = new ObjectInputStream(client.getInputStream());
                out = new ObjectOutputStream(client.getOutputStream());
                System.out.println("in and out created");

                System.out.println("Wait for messages");

                while((line = (ArrayList) in.readObject()) != null) {

                    if(line.size() != 0) {
                        head = line.get(0);
                        System.out.println("Received: " + line.toString() + "\nTime: " + LocalDateTime.now());
                        switch (head) {
                            case shipMsg:
                                shipsHashMap.put(line.get(1), new Ship(line.get(2), Double.parseDouble(line.get(3)),
                                        Double.parseDouble(line.get(4)), LocalTime.now()));
                                System.out.println("Client Worker: Added ID:" + line.get(1));

                                response = new ArrayList<>();
                                response.add(shipMsg);

                                ArrayList<String> keyList = new ArrayList<>(shipsHashMap.keySet());
                                for (String aKeyList : keyList) {
                                    response.add((shipsHashMap.get(aKeyList)).getName());
                                    response.add(Double.toString((shipsHashMap.get(aKeyList)).getLat()));
                                    response.add(Double.toString((shipsHashMap.get(aKeyList)).getLon()));
                                }
                                out.writeObject(response);
                                System.out.println("Sent: " + response.toString());
                                break;
                            case id:
                                response = new ArrayList<>();
                                response.add(id);
                                ID = db.getID(line.get(1));
                                //add ID
                                response.add(Integer.toString(db.getID(line.get(1))));
                                out.writeObject(response);
                                //Add this ClientWorker to list
                                clients.put(Integer.toString(ID), this);
                                System.out.println("Sent: " + response.toString());
                                response = new ArrayList<>();
                                response.add(points);
                                response.add(Integer.toString(db.getPointsByID(ID)));
                                out.writeObject(response);
                                System.out.println("Sent: " + response.toString());
                                break;
                            case missile:
                                missileList.add(new Missile(Integer.parseInt(line.get(1)), Double.parseDouble(line.get(2)), Double.parseDouble(line.get(3)),
                                        Double.parseDouble(line.get(4)), LocalTime.now()));
                                System.out.println("Received missile");
                                break;
                            case missileArray:
                                response = new ArrayList<>();
                                response.add("missileArray");
                                for (Missile missile : missileList) {
                                    response.add(Double.toString(missile.getBearing()));
                                    response.add(Double.toString(missile.getLat()));
                                    response.add(Double.toString(missile.getLon()));
                                }
                                out.writeObject(response);
                                System.out.println("Sent: " + response.toString());
                                response = new ArrayList<>();
                                response.add("exp");
                                for (Explosion exp : expList) {
                                    response.add(Double.toString(exp.getLat()));
                                    response.add(Double.toString(exp.getLon()));
                                }
                                out.writeObject(response);
                                System.out.println("Sent: " + response.toString());
                                break;
                            default:
                                break;
                        }
                    }
                }

            } catch (Exception e) {
                System.out.println("disconnected " + e);
            }
            finally {
                try {
                    in.close();
                    out.close();
                    client.close();
                    clients.remove(Integer.toString(ID));
                    System.out.println("Client closed");
                } catch (IOException ioe) {
                    System.out.println("cant stoop client");
                }
            }
        }
    }
}
