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

public class Server {


    public static void main(String[] args) throws IOException {

        //Scanner scanner = new Scanner(System.in);
        //String insert = scanner.nextLine();

        Ships ships = new Ships();
        ships.start();

        Timer shipLifeChecker = new Timer();
        shipLifeChecker.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                System.out.println("checking lifes");
                ConcurrentHashMap<String, Ship> shipList = ships.getShips();
                if (shipList != null) {
                    shipList.forEach((k, v) -> {
                        System.out.println(ChronoUnit.MINUTES.between(v.getLife(), LocalTime.now()));
                        if (ChronoUnit.MINUTES.between(v.getLife(), LocalTime.now()) >= 1) {
                            shipList.remove(k);
                            System.out.println("removed:" + k);
                        }
                    });
                }
            }
        }, 2*60000, 2*60000);
        
        Missles missles = new Missles();
        missles.start();
        
        Timer misslePosCalc = new Timer();
        misslePosCalc.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                ConcurrentHashMap<Ship> missleList = missles.getMissles();
                if (missleList != null) {
                    for(Missle missle : missleList) {
                        
                    }
                }
            }
        }, 1000, 1000);

        Fishies db = new Fishies();
        db.run();

        db.insert("test1", 123);

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
                w = new ClientWorker(server.accept(), ships, db);
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
        private Ships ships;
        private ArrayList<String> line;
        private ArrayList<String> response;
        private String head;
        private Fishies db;

        //Constructor
        ClientWorker(Socket client, Ships ships, Fishies db) {
            this.client = client;
            this.ships = ships;
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
                    System.out.println("Receiving: " + line.toString());
                    //line = (ArrayList) in.readObject();
                    if(line.size() != 0) {
                        head = line.get(0);
                        System.out.println("Received: " + line.toString() + "\nTime: " + LocalDateTime.now());
                        switch (head) {
                            case shipMsg:
                                ships.addShip(line.get(1), new Ship(line.get(2), Double.parseDouble(line.get(3)),
                                        Double.parseDouble(line.get(4)), LocalTime.now()));
                                System.out.println("Added:" + line.get(1));

                                ConcurrentHashMap<String, Ship> shipsHashMap = ships.getShips();

                                response = new ArrayList<>();
                                response.add(shipMsg);

                                ArrayList<String> keyList = new ArrayList<>(shipsHashMap.keySet());
                                for (String aKeyList : keyList) {
                                    response.add(aKeyList);
                                    response.add(Double.toString((shipsHashMap.get(aKeyList)).getLat()));
                                    response.add(Double.toString((shipsHashMap.get(aKeyList)).getLon()));
                                }
                                out.writeObject(response);
                                System.out.println("Sent: " + response.toString());
                                break;
                            case id:
                                response = new ArrayList<>();
                                response.add(id);
                                ArrayList<> points = db.getPoints(line.get(1));
                                response.add(Integer.toString(points.get(0)));
                                response.add(Integer.toString(points.get(1)));
                                out.writeObject(response);
                                System.out.println("Sent: " + response.toString());
                                break;
                            case missle:
                                missles.addMissle(new Missle(int.parseInt(line.get(1)), Double.parseDouble(line.get(2)),
                                        Double.parseDouble(line.get(3)), LocalTime.now());
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
