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

import static java.lang.Math.acos;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

public class Server {


    public static void main(String[] args) throws IOException {

        //Scanner scanner = new Scanner(System.in);
        //String insert = scanner.nextLine();
        
        double R = 6378.1; //Radius of the Earth km
        double d = 0.07; //500km/h in 0.5 sec
        ConcurrentLinkedQueue<Missle> missleList  = new ConcurrentLinkedQueue<Missle>();
        ConcurrentHashMap<String, Ship> shipList = new ConcurrentHashMap<String, Ship>();

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

        Timer misslePosCalc = new Timer();
        misslePosCalc.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {


                    for(Missle missle : missleList) {
                        double bearing = Math.toRadians(missle.getBearing()); //Bearing is 90 degrees converted to radians.

                        double lat1 = Math.toRadians(missle.getLat()); //Current lat point converted to radians
                        double lon1 = Math.toRadians(missle.getLon()); //Current lon point converted to radians

                        double lat2 = Math.asin( sin(lat1)* cos(d/R) + cos(lat1)* sin(d/R)* cos(bearing));

                        double lon2 = lon1 + Math.atan2(sin(bearing)* sin(d/R)* cos(lat1), cos(d/R)- sin(lat1)* sin(lat2));

                        lat2 = Math.toDegrees(lat2);
                        lon2 = Math.toDegrees(lon2);
                        
                        missle.setLat(lat2);
                        missle.setLon(lon2);
                    }

            }
        }, 500, 500);
        
        Timer missleCheck = new Timer();
        missleCheck.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                    for(Missle missle : missleList) {

                        int missleID = missle.getID();
                            shipList.forEach((k, v) -> {

                                if(!k.equals(Integer.toString(missleID))) {

                                    double lat1 = Math.toRadians(missle.getLat());
                                    double lon1 = Math.toRadians(missle.getLon());

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
                                        missleList.remove(missle);
                                        //TODO: add points to shooted ID
                                        db.updatePoints(missleID, 1);
                                    }
                                }

                                if (ChronoUnit.MINUTES.between(missle.getLife(), LocalTime.now()) >= 20) {
                                    missleList.remove(missle);
                                    System.out.println("removed:" + missle);
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
                w = new ClientWorker(server.accept(), shipList, db, missleList);
                Thread t = new Thread(w);
                t.start();
                System.out.println("Client connected");
            } catch (IOException e) {
                System.out.println("Cant connect");
                System.exit(-1);
            }
        }

    }


    static class ClientWorker implements Runnable {

        private Socket client;
        private final String id = "id";
        private final String shipMsg = "ship";
        private final String missle = "missle";
        private final String missleArray = "missleArray";
        private ArrayList<String> line;
        private ArrayList<String> response;
        private String head;
        private Fishies db;
        private ConcurrentHashMap<String, Ship> shipsHashMap;
        private ConcurrentLinkedQueue<Missle> missleList;

        //Constructor
        ClientWorker(Socket client, ConcurrentHashMap ships, Fishies db, ConcurrentLinkedQueue missles) {
            this.missleList = missles;
            this.client = client;
            this.shipsHashMap = ships;
            this.db = db;
        }

        public void run() {
            ObjectInputStream in = null;
            ObjectOutputStream out = null;

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
                                System.out.println("Added:" + line.get(1));

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
                                int[] points = db.getPoints(line.get(1));
                                response.add(Integer.toString(points[0]));
                                response.add(Integer.toString(points[1]));
                                out.writeObject(response);
                                System.out.println("Sent: " + response.toString());
                                break;
                            case missle:
                                missleList.add(new Missle(Integer.parseInt(line.get(1)), Double.parseDouble(line.get(2)), Double.parseDouble(line.get(3)),
                                        Double.parseDouble(line.get(4)), LocalTime.now()));
                                System.out.println("Received missle");
                                break;
                            case missleArray:
                                response = new ArrayList<>();
                                response.add("missleArray");
                                for (Missle missle : missleList) {
                                    response.add(Double.toString(missle.getBearing()));
                                    response.add(Double.toString(missle.getLat()));
                                    response.add(Double.toString(missle.getLon()));
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
                    System.out.println("Client closed");
                } catch (IOException ioe) {
                    System.out.println("cant stoop client");
                }
            }
        }
    }
}
