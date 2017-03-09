import sun.rmi.runtime.Log;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.time.LocalDateTime;

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
                        System.out.println("Received: " + line.toString() + "\n Time: " + LocalDateTime.now());
                        switch (head) {
                            case ship:
                                ships.addShip(line.get(1), new Point(Integer.parseInt(line.get(2)), Integer.parseInt(line.get(3))));
                                ConcurrentHashMap shipsHashMap = ships.getShips();
                                response = new ArrayList<>();
                                response.add(ship);
                                List<String> keyList = new ArrayList<String>(shipsHashMap.keySet());
                                for(int i = 0; i < keyList.size(), i++) {
                                    String shipKey = ((String) keylist.get(i));
                                    response.add(shipKey);
                                    response.add(shipHashMap.get(shipKey).getX());
                                    response.add(shipHashMap.get(shipKey).getY());
                                }
                                out.writeObject(response);
                                System.out.println("Sent: " + response.toString());
                                break;
                            case id:
                                response = new ArrayList<>();
                                response.add(id);
                                response.add("points");
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
