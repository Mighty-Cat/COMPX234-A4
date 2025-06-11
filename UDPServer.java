import java.io.*;
import java.net.*;

public class UDPServer {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java UDPServer <port>");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("Error: Port must be a number");
            return;
        }

        try (DatagramSocket socket = new DatagramSocket(port)) {
            System.out.println("Server started on port " + port);
            byte[] buffer = new byte[1024];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength()).trim();
                System.out.println("Received: " + received);

                String[] parts = received.split(" ");
                if (parts.length >= 2 && parts[0].equals("DOWNLOAD")) {
                    String filename = parts[1];
                    File file = new File(filename);
                    String response;
                    if (file.exists()) {
                        // Temporary OK response, will add port later
                        response = "OK " + filename + " SIZE " + file.length();
                    } else {
                        response = "ERR " + filename + " NOT_FOUND";
                    }
                    byte[] sendData = response.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(
                        sendData, sendData.length, packet.getAddress(), packet.getPort()
                    );
                    socket.send(sendPacket);
                    System.out.println("Sent: " + response);
                }
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }
}