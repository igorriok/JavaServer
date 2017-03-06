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
                System.out.println("Accept failed");
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
                    try {
                        //line = (ArrayList) in.readObject();
                        String head = line.get(0);
                        System.out.println("Received: " + line + "\n Time: " + LocalDateTime.now());
                        switch (head) {
                            case ship:
                                ships.addShip(line.get(1), new Point(Integer.parseInt(line.get(2)), Integer.parseInt(line.get(3))));
                                response = new ArrayList<>();
                                response.add(ship);
                                response.add("ships");
                                out.writeObject(response);
                                break;
                            case id:
                                response = new ArrayList<>();
                                response.add(id);
                                response.add("points");
                                out.writeObject(response);
                                break;
                            }

                    } catch (IOException e) {
                        System.out.println("cant read object");
                    } /**catch (ClassNotFoundException e) {
                        System.out.println("cant read thi kind of object");
                    }*/
                }

            } catch (Exception e) {
                System.out.println("cant connect");

            } finally {
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
