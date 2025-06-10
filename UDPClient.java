import java.io.*;
import java.util.*;

public class UDPClient {
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java UDPClient <hostname> <port> <file_list>");
            return;
        }

        String hostname = args[0];
        int port;
        String fileList = args[2];

        // Parse port number
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Error: Port must be a number");
            return;
        }

        // Read file list
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

        System.out.println("Files to download: " + files);
        // TODO: Implement socket communication
    }
}