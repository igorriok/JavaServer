import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.time.LocalDateTime;

public class Server {


    public static void main(String[] args) throws IOException {

        //Scanner scanner = new Scanner(System.in);
        //String insert = scanner.nextLine();

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

            try{
                //server.accept returns a client connection
                w = new ClientWorker(server.accept());
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

        //Constructor
        ClientWorker(Socket client) {
            this.client = client;
        }

        public void run() {

            String line;
            BufferedReader in = null;
            PrintWriter out = null;

            System.out.println("Client running");

            try {
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out = new PrintWriter(client.getOutputStream(), true);
                System.out.println("in and out created");

                System.out.println("Wait for messages");

                while ((line = in.readLine()) != null) {

                    //line = in.readLine();
                    //Send data back to client
                    out.println(line);
                    System.out.println("Received: " + line + "\n Time: " + LocalDateTime.now());
                }

            } catch (Exception e) {
                System.out.println("cant connect");

            } finally {
                try {
                    in.close();
                    out.close();
                    client.close();
                    System.out.println("...Stopped");
                } catch (IOException ioe) {
                    System.out.println("cant stoop client");
                }
            }
        }
    }
}