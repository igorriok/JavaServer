package Server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;

public class ClientWorker implements Runnable {

    private Socket client;
    private final static String id = "id";
    private final static String shipMsg = "ship";
    private final static String missile = "missile";
    private final static String missileArray = "missileArray";
    private final static String shield = "shield";
    final static String points = "points";
    private ArrayList<String> line;
    private ArrayList<String> response;
    private Fishies db;
    private ConcurrentHashMap<String, Ship> shipsHashMap;
    private ConcurrentLinkedQueue<Missile> missileList;
    private ConcurrentHashMap<String, ClientWorker> clients;
    private ConcurrentLinkedQueue<Explosion> expList;
    private ObjectOutputStream out;
    private int ID = 0;
    private HttpTransport transport;
    private JsonFactory jsonFactory;
    private Double botLat, botLong;


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

    synchronized void sendMessage(ArrayList message) {
        try {
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Cant send message, " + e);
        }
    }
    
    public void setGoogleCon(HttpTransport transport, JsonFactory jsonFactory) {
        this.transport = transport;
        this.jsonFactory = jsonFactory;
    }

    public void run() {

        ObjectInputStream in = null;
        out = null;

        System.out.println("Client running");

        Random rnd = new Random();
        int botNr = rnd.nextInt(1000000);
        
        ScheduledExecutorService botScheduler = Executors.newScheduledThreadPool(1);
        botScheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                shipsHashMap.put(botNr + "", new Ship("Bot-" + botNr, botLat + rnd.nextInt(1),
                        botLong + rnd.nextInt(1), LocalTime.now(), 0));
            }
        }, 30, 5 * 60, TimeUnit.SECONDS);

        try {
            in = new ObjectInputStream(client.getInputStream());
            out = new ObjectOutputStream(client.getOutputStream());
            System.out.println("in and out created");

            System.out.println("Wait for messages");

            while((line = (ArrayList) in.readObject()) != null) {

                if(line.size() != 0) {
                    String head = line.get(0);
                    //System.out.println("Received: " + line.toString() + "\nTime: " + LocalDateTime.now());
                    switch (head) {
                        case shipMsg:
                            if (shipsHashMap.containsKey(line.get(1))) {
                                shipsHashMap.replace(line.get(1), new Ship(line.get(2), Double.parseDouble(line.get(3)),
                                        Double.parseDouble(line.get(4)), LocalTime.now(), Double.parseDouble(line.get(5))));
                                //System.out.println("Client Worker: updated ship:" + line.get(1));
                            } else {
                                shipsHashMap.put(line.get(1), new Ship(line.get(2), Double.parseDouble(line.get(3)),
                                        Double.parseDouble(line.get(4)), LocalTime.now(), Double.parseDouble(line.get(5))));
                                //System.out.println("Client Worker: Added ship:" + line.get(1));
                                botLat = Double.parseDouble(line.get(3));
                                botLong = Double.parseDouble(line.get(4));
                            }

                            response = new ArrayList<>();
                            response.add(shipMsg);
                            //Creating arrayList with all ships
                            //used java 7 loop
                            ArrayList<String> keyList = new ArrayList<>(shipsHashMap.keySet());
                            for (String aKeyList : keyList) {
                                response.add((shipsHashMap.get(aKeyList)).getName());
                                response.add(Double.toString((shipsHashMap.get(aKeyList)).getLat()));
                                response.add(Double.toString((shipsHashMap.get(aKeyList)).getLon()));
                                response.add(Double.toString((shipsHashMap.get(aKeyList)).getBearing()));
                            }
                            sendMessage(response);
                            //System.out.println("Sent: " + response.toString());
                            break;
                        case id:
                            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                                .setAudience(Collections.singletonList("653188213597-jll3ngrpn5b91b1ghn1vdnqhs3bi45s0.apps.googleusercontent.com"))
                                // Or, if multiple clients access the backend:
                                //.setAudience(Arrays.asList(CLIENT_ID_1, CLIENT_ID_2, CLIENT_ID_3))
                                .build();

                            // (Receive idTokenString from app)
                            GoogleIdToken idToken = verifier.verify(line.get(1));

                            if (idToken != null) {
                                Payload payload = idToken.getPayload();

                                // Print user identifier
                                String userId = payload.getSubject();
                                System.out.println("User ID: " + userId);
                                //make response
                                response = new ArrayList<>();
                                //add id head
                                response.add(id);
                                ID = db.getID(userId);
                                //add db ID
                                response.add(Integer.toString(ID));
                                sendMessage(response);
                                //Add this ClientWorker to list of clients
                                clients.put(Integer.toString(ID), this);
                                System.out.println("Sent: " + response.toString());
                                //make points response
                                response = new ArrayList<>();
                                //add points head
                                response.add(points);
                                //add points
                                response.add(Integer.toString(db.getPointsByID(ID)));
                                sendMessage(response);
                                System.out.println("Sent: " + response.toString());
                            } else {
                                System.out.println("Invalid ID token.");
                            }
                            break;
                        case missile:
                            //Missile(int ID, double bearing, double lat, double lon, LocalTime life)
                            missileList.add(new Missile(Integer.parseInt(line.get(1)), Double.parseDouble(line.get(2)), Double.parseDouble(line.get(3)),
                                    Double.parseDouble(line.get(4)), LocalTime.now()));
                            System.out.println("Received missile " + line.get(1));
                            break;
                        case missileArray:
                            response = new ArrayList<>();
                            response.add("missileArray");
                            for (Missile missile : missileList) {
                                response.add(Double.toString(missile.getBearing()));
                                response.add(Double.toString(missile.getLat()));
                                response.add(Double.toString(missile.getLon()));
                            }
                            sendMessage(response);
                            //System.out.println("Sent: " + response.toString());
                            response = new ArrayList<>();
                            response.add("exp");
                            for (Explosion exp : expList) {
                                response.add(Double.toString(exp.getLat()));
                                response.add(Double.toString(exp.getLon()));
                            }
                            sendMessage(response);
                            //System.out.println("Sent: " + response.toString());
                            break;
                        case shield:
                            (shipsHashMap.get(ID)).setShield(true);
                            Timer shieldState = new Timer();
                            shieldState.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    (shipsHashMap.get(ID)).setShield(false);
                                }
                            }, 3*1000);
                            break;
                        default:
                            break;
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("disconnected " + e);
            botScheduler.shutdownNow();
        }
        finally {
            try {
                in.close();
                out.close();
                client.close();
                botScheduler.shutdownNow();
                clients.remove(Integer.toString(ID));
                System.out.println("Client closed");
            } catch (IOException ioe) {
                System.out.println("cant stoop client");
            }
        }
    }
}