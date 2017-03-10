import sun.rmi.runtime.Log;

import java.awt.geom.Point2D;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

public class Server {


    public static void main(String[] args) throws IOException {

        //Scanner scanner = new Scanner(System.in);
        //String insert = scanner.nextLine();

        Ships ships = new Ships();
        ships.start();

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
                w = new ClientWorker(server.accept(), ships);
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
        private final String ship = "ship";
        private Ships ships;
        private ArrayList<String> line;
        private ArrayList<String> response;
        private String head;

        //Constructor
        ClientWorker(Socket client, Ships ships) {
            this.client = client;
            this.ships = ships;
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
                            case ship:
                                ships.addShip(line.get(1), new Point2D.Double(Double.parseDouble(line.get(2)), Double.parseDouble(line.get(3))));
                                System.out.println("Added:" + line.get(1));

                                ConcurrentHashMap<String, Point2D.Double> shipsHashMap = ships.getShips();

                                response = new ArrayList<>();
                                response.add(ship);
                                ArrayList<String> keyList = new ArrayList<>(shipsHashMap.keySet());
                                for (String aKeyList : keyList) {
                                    response.add(aKeyList);
                                    response.add(Double.toString((shipsHashMap.get(aKeyList)).getX()));
                                    response.add(Double.toString((shipsHashMap.get(aKeyList)).getY()));
                                }
                                out.writeObject(response);
                                System.out.println("Sent: " + response.toString());
                                break;
                            case id:
                                response = new ArrayList<>();
                                response.add(id);
                                response.add("points"); //to be changed in get points from SQLite table
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
