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

import static java.util.concurrent.TimeUnit.SECONDS;

public class ClientWorker implements Runnable {

    private Socket client;
    private final static String ID = "ID";
    private final static String SHIP_MSG = "SHIP";
    private final static String MISSILE = "MISSILE";
    private final static String MISSILE_ARRAY = "MISSILE_ARRAY";
    private final static String SHIELD = "SHIELD";
    final static String POINTS = "POINTS";
    private ArrayList<String> line;
    private ArrayList<String> response;
    private Fishies db;
    private ConcurrentHashMap<String, Ship> shipsHashMap;
    private ConcurrentLinkedQueue<Missile> missileList;
    private ConcurrentHashMap<String, ClientWorker> clients;
    private ConcurrentLinkedQueue<Explosion> expList;
    private ObjectOutputStream out;
    private int internalID;
    private HttpTransport transport;
    private JsonFactory jsonFactory;
    private String threadName;
    private ScheduledExecutorService botScheduler = Executors.newScheduledThreadPool(1);;


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

    private void addBot(double lat, double lang) {
        Random rnd = new Random();
        int botNr = rnd.nextInt(1000000);
        botScheduler.scheduleAtFixedRate(() -> shipsHashMap.put(botNr + "", new Ship("Bot-" + botNr,
                        lat + ThreadLocalRandom.current().nextDouble(-0.3, 0.3),
                        lang + ThreadLocalRandom.current().nextDouble(-0.3, 0.3), LocalTime.now(), 0)),
                0, 301, SECONDS);
    }

    @Override
    public void run() {

        ObjectInputStream in;
        out = null;

        System.out.println("Client running");

        //Add this ClientWorker to list
        //threadName = Thread.currentThread().getName();
        
        //botScheduler = Executors.newScheduledThreadPool(1);

        try {
            in = new ObjectInputStream(client.getInputStream());
            out = new ObjectOutputStream(client.getOutputStream());
            System.out.println("in and out created");

            System.out.println("Waiting for messages");

            while((line = (ArrayList) in.readObject()) != null) {

                if(line.size() != 0) {
                    String head = line.get(0);
                    //System.out.println("Received: " + line.toString() + "\nTime: " + LocalDateTime.now());
                    switch (head) {
                        case SHIP_MSG:
                            if (shipsHashMap.containsKey(line.get(1))) {
                                //replace this ship (ID, Ship)
                                shipsHashMap.replace(line.get(1), new Ship(line.get(2), Double.parseDouble(line.get(3)),
                                        Double.parseDouble(line.get(4)), LocalTime.now(), Double.parseDouble(line.get(5))));
                                //System.out.println("Client Worker: updated ship:" + line.get(1));
                            } else {
                                //add this ship (ID, Ship)
                                shipsHashMap.put(line.get(1), new Ship(line.get(2), Double.parseDouble(line.get(3)),
                                        Double.parseDouble(line.get(4)), LocalTime.now(), Double.parseDouble(line.get(5))));
                                //System.out.println("Client Worker: Added ship:" + line.get(1));
                                //set lat, long for bot
                                addBot(Double.parseDouble(line.get(3)), Double.parseDouble(line.get(4)));
                            }

                            response = new ArrayList<>();
                            response.add(SHIP_MSG);
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
                        case ID:
                            System.out.println("internalID");
                            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                                .setAudience(Collections.singletonList("653188213597-vtktdjdlgo929m83vvhesq7rvtpgkngt.apps.googleusercontent.com"))
                                // Or, if multiple clients access the backend:
                                //.setAudience(Arrays.asList(CLIENT_ID_1, CLIENT_ID_2, CLIENT_ID_3))
                                .build();

                            // (Receive idTokenString from app)
                            GoogleIdToken idToken = verifier.verify(line.get(1));

                            if (idToken != null) {
                                Payload payload = idToken.getPayload();

                                // Print user identifier
                                String userId = payload.getSubject();
                                System.out.println("User internalID: " + userId);
                                //make response
                                response = new ArrayList<>();
                                //add ID head
                                response.add(ID);
                                //get internalID from database
                                internalID = db.getID(userId);
                                //add this thread to clients list
                                clients.put(Integer.toString(internalID), this);
                                //add db internalID
                                response.add(Integer.toString(internalID));
                                sendMessage(response);
                                System.out.println("Sent: " + response.toString());
                                //make POINTS response
                                response = new ArrayList<>();
                                //add POINTS head
                                response.add(POINTS);
                                //add POINTS
                                response.add(Integer.toString(db.getPointsByID(internalID)));
                                sendMessage(response);
                                System.out.println("Sent: " + response.toString());
                            } else {
                                System.out.println("Invalid internalID token.");
                            }
                            break;
                        case MISSILE:
                            //Missile(int internalID, double bearing, double lat, double lon, LocalTime life)
                            missileList.add(new Missile(Integer.parseInt(line.get(1)), Double.parseDouble(line.get(2)), Double.parseDouble(line.get(3)),
                                    Double.parseDouble(line.get(4)), LocalTime.now()));
                            System.out.println("Received MISSILE " + line.get(1));
                            break;
                        case MISSILE_ARRAY:
                            response = new ArrayList<>();
                            response.add("MISSILE_ARRAY");
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
                        case SHIELD:
                            (shipsHashMap.get(Integer.toString(internalID))).setShield(true);
                            Timer shieldState = new Timer();
                            shieldState.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    (shipsHashMap.get(Integer.toString(internalID))).setShield(false);
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
        }
        finally {
            try {
                out.close();
                client.close();
                botScheduler.shutdownNow();
                clients.remove(Integer.toString(internalID));
                System.out.println("Client closed");
            } catch (IOException ioe) {
                System.out.println("cant stoop client");
            } catch (NullPointerException e) {
                System.out.println("in cant be closed:" + e);
            }
        }
    }
}
