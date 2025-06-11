import java.io.*;
import java.net.*;
import java.util.*;

public class UDPClient {
    private static final int INITIAL_TIMEOUT = 1000; // 1 second
    private static final int MAX_RETRIES = 5;

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java UDPClient <hostname> <port> <file_list>");
            return;
        }

        String hostname = args[0];
        int port;
        String fileList = args[2];

        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Error: Port must be a number");
            return;
        }

        List<String> files = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(fileList))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    files.add(line);
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading file list: " + e.getMessage());
            return;
        }

        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress serverAddress = InetAddress.getByName(hostname);

            for (String filename : files) {
                String message = "DOWNLOAD " + filename;
                String response = sendAndReceive(socket, serverAddress, port, message);
                if (response != null) {
                    System.out.println("Received: " + response);
                    // TODO: Process response
                } else {
                    System.out.println("Failed to download " + filename);
                }
            }
        } catch (IOException e) {
            System.out.println("Socket error: " + e.getMessage());
        }
    }

    private static String sendAndReceive(DatagramSocket socket, InetAddress address, int port, String message) {
        int currentTimeout = INITIAL_TIMEOUT;
        int retries = 0;
        byte[] buffer = new byte[1024];

        while (retries < MAX_RETRIES) {
            try {
                // Send packet
                byte[] sendData = message.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
                socket.send(sendPacket);
                System.out.println("Sent: " + message);

                // Set timeout and wait for response
                socket.setSoTimeout(currentTimeout);
                DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(receivePacket);
                return new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();

            } catch (SocketTimeoutException e) {
                retries++;
                System.out.println("Timeout, retrying (" + retries + "/" + MAX_RETRIES + ")");
                currentTimeout *= 2; // Double timeout
            } catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
                return null;
            }
        }
        System.out.println("Max retries reached");
        return null;
    }
}