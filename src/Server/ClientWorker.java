package Server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ClientWorker implements Runnable {

    private Socket client;
    private final static String id = "id";
    private final static String shipMsg = "ship";
    private final static String missile = "missile";
    private final static String missileArray = "missileArray";
    final static String points = "points";
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

    synchronized void sendMessage(ArrayList message) {
        try {
            out.writeObject(message);
            out.flush();
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
                    //System.out.println("Received: " + line.toString() + "\nTime: " + LocalDateTime.now());
                    switch (head) {
                        case shipMsg:
                            if (shipsHashMap.containsKey(line.get(1))) {
                                shipsHashMap.replace(line.get(1), new Ship(line.get(2), Double.parseDouble(line.get(3)),
                                        Double.parseDouble(line.get(4)), LocalTime.now()));
                                //System.out.println("Client Worker: updated ship:" + line.get(1));
                            } else {
                                shipsHashMap.put(line.get(1), new Ship(line.get(2), Double.parseDouble(line.get(3)),
                                        Double.parseDouble(line.get(4)), LocalTime.now()));
                                //System.out.println("Client Worker: Added ship:" + line.get(1));
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
                            }
                            sendMessage(response);
                            //System.out.println("Sent: " + response.toString());
                            break;
                        case id:
                            response = new ArrayList<>();
                            response.add(id);
                            ID = db.getID(line.get(1));
                            //add ID
                            response.add(Integer.toString(db.getID(line.get(1))));
                            sendMessage(response);
                            //Add this ClientWorker to list of clients
                            clients.put(Integer.toString(ID), this);
                            System.out.println("Sent: " + response.toString());
                            response = new ArrayList<>();
                            response.add(points);
                            response.add(Integer.toString(db.getPointsByID(ID)));
                            sendMessage(response);
                            System.out.println("Sent: " + response.toString());
                            break;
                        case missile:
                            missileList.add(new Missile(Integer.parseInt(line.get(1)), Double.parseDouble(line.get(2)), Double.parseDouble(line.get(3)),
                                    Double.parseDouble(line.get(4)), LocalTime.now()));
                            //System.out.println("Received missile");
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