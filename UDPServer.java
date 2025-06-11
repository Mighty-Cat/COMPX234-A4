import java.io.*;
import java.net.*;
import java.util.*;

public class UDPServer {
    private static final int MIN_PORT = 50000;
    private static final int MAX_PORT = 51000;

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
                        Random rand = new Random();
                        int dataPort = MIN_PORT + rand.nextInt(MAX_PORT - MIN_PORT + 1);
                        response = "OK " + filename + " SIZE " + file.length() + " PORT " + dataPort;
                        // Start thread to handle data requests
                        new Thread(() -> handleFileTransmission(dataPort, file, packet.getAddress(), packet.getPort())).start();
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

    private static void handleFileTransmission(int dataPort, File file, InetAddress clientAddress, int clientPort) {
        try (DatagramSocket socket = new DatagramSocket(dataPort)) {
            byte[] buffer = new byte[1024];
            RandomAccessFile raf = new RandomAccessFile(file, "r");

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String request = new String(packet.getData(), 0, packet.getLength()).trim();
                System.out.println("Thread on port " + dataPort + " received: " + request);

                String[] parts = request.split(" ");
                if (parts.length >= 2 && parts[0].equals("FILE")) {
                    String filename = parts[1];
                    if (parts[2].equals("CLOSE")) {
                        String response = "FILE " + filename + " CLOSE_OK";
                        byte[] sendData = response.getBytes();
                        DatagramPacket sendPacket = new DatagramPacket(
                            sendData, sendData.length, clientAddress, clientPort
                        );
                        socket.send(sendPacket);
                        System.out.println("Sent: " + response);
                        break;
                    } else if (parts[2].equals("GET") && parts.length >= 6) {
                        long start = Long.parseLong(parts[4]);
                        long end = Long.parseLong(parts[5]);
                        if (start >= 0 && end < file.length() && start <= end) {
                            raf.seek(start);
                            byte[] fileData = new byte[(int)(end - start + 1)];
                            raf.readFully(fileData);
                            String base64Data = Base64.getEncoder().encodeToString(fileData);
                            String response = "FILE " + filename + " OK START " + start + " END " + end + " DATA " + base64Data;
                            byte[] sendData = response.getBytes();
                            DatagramPacket sendPacket = new DatagramPacket(
                                sendData, sendData.length, clientAddress, clientPort
                            );
                            socket.send(sendPacket);
                            System.out.println("Sent: FILE " + filename + " OK START " + start + " END " + end);
                        }
                    }
                }
            }
            raf.close();
        } catch (IOException e) {
            System.out.println("Thread error on port " + dataPort + ": " + e.getMessage());
        }
    }
}