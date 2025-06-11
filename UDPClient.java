import java.io.*;
import java.net.*;
import java.util.*;

public class UDPClient {
    private static final int INITIAL_TIMEOUT = 1000; // 1 second
    private static final int MAX_RETRIES = 5;
    private static final int BLOCK_SIZE = 1000; // Bytes per block

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
                System.out.println("Downloading " + filename);
                String message = "DOWNLOAD " + filename;
                String response = sendAndReceive(socket, serverAddress, port, message);
                if (response == null || response.startsWith("ERR")) {
                    System.out.println("Cannot download " + filename + ": " + (response != null ? response : "No response"));
                    continue;
                }

                // Parse OK response
                String[] parts = response.split(" ");
                if (parts.length < 6 || !parts[0].equals("OK")) {
                    System.out.println("Invalid response: " + response);
                    continue;
                }
                long fileSize = Long.parseLong(parts[3]);
                int dataPort = Integer.parseInt(parts[5]);
                System.out.println(filename + " size: " + fileSize + " bytes, data port: " + dataPort);

                // Create file
                try (RandomAccessFile raf = new RandomAccessFile(filename, "rw")) {
                    raf.setLength(fileSize); // Pre-allocate file
                    long currentPos = 0;

                    // Download file in blocks
                    while (currentPos < fileSize) {
                        long endPos = Math.min(currentPos + BLOCK_SIZE - 1, fileSize - 1);
                        String getMessage = "FILE " + filename + " GET START " + currentPos + " END " + endPos;
                        String dataResponse = sendAndReceive(socket, serverAddress, dataPort, getMessage);
                        if (dataResponse == null || !dataResponse.startsWith("FILE " + filename + " OK")) {
                            System.out.println("Error receiving data for " + filename);
                            break;
                        }

                        // Parse data response
                        String[] dataParts = dataResponse.split(" ", 7);
                        if (dataParts.length < 7 || !dataParts[2].equals("OK")) {
                            System.out.println("Invalid data response: " + dataResponse);
                            break;
                        }
                        String base64Data = dataParts[6].trim();
                        byte[] fileData = Base64.getDecoder().decode(base64Data);
                        raf.seek(currentPos);
                        raf.write(fileData);
                        System.out.print("*");
                        currentPos += fileData.length;
                    }
                    System.out.println("\nDownload completed: " + filename);

                    // Send close message
                    String closeMessage = "FILE " + filename + " CLOSE";
                    String closeResponse = sendAndReceive(socket, serverAddress, dataPort, closeMessage);
                    if (closeResponse != null && closeResponse.equals("FILE " + filename + " CLOSE_OK")) {
                        System.out.println("File closed: " + filename);
                    } else {
                        System.out.println("Close failed for " + filename);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Socket error: " + e.getMessage());
        }
    }

    private static String sendAndReceive(DatagramSocket socket, InetAddress address, int port, String message) {
        int currentTimeout = INITIAL_TIMEOUT;
        int retries = 0;
        byte[] buffer = new byte[2048]; // Increased for Base64 data

        while (retries < MAX_RETRIES) {
            try {
                byte[] sendData = message.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
                socket.send(sendPacket);
                System.out.println("Sent: " + message);

                socket.setSoTimeout(currentTimeout);
                DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(receivePacket);
                return new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();

            } catch (SocketTimeoutException e) {
                retries++;
                System.out.println("Timeout, retrying (" + retries + "/" + MAX_RETRIES + ")");
                currentTimeout *= 2;
            } catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
                return null;
            }
        }
        System.out.println("Max retries reached");
        return null;
    }
}