import java.io.IOException;
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
                // TODO: Process request
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }
}