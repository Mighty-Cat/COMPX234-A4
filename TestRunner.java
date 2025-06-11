import java.io.*;
import java.nio.file.*;

/**
 * TestRunner simplifies testing of UDPServer and UDPClient for COMPX234-A4.
 * It sets up a server and one client, downloads a test file, and checks MD5.
 */
public class TestRunner {
    private static final String SERVER_DIR = "test/server";
    private static final String CLIENT_DIR = "test/client";
    private static final String TEST_FILE = "test.txt";
    private static final int SERVER_PORT = 51234;
    private static final String HOSTNAME = "localhost";

    public static void main(String[] args) {
        try {
            System.out.println("Starting test...");
            setupTestEnvironment();
            compileClasses();
            runTest();
            verifyResults();
            System.out.println("Test completed!");
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    /**
     * Sets up server and client directories with a test file and files.txt.
     */
    private static void setupTestEnvironment() throws IOException {
        // Create directories
        new File(SERVER_DIR).mkdirs();
        new File(CLIENT_DIR).mkdirs();

        // Create test file in server directory
        Files.write(Paths.get(SERVER_DIR, TEST_FILE), "Hello, this is a test file.".getBytes());
        System.out.println("Created test file: " + TEST_FILE);

        // Create files.txt for client
        Files.write(Paths.get(CLIENT_DIR, "files.txt"), (TEST_FILE + "\n").getBytes());
        System.out.println("Created files.txt in client directory");
    }

    /**
     * Compiles UDPServer and UDPClient.
     */
    private static void compileClasses() throws IOException, InterruptedException {
        String[] commands = {
            "javac UDPServer.java -d " + SERVER_DIR,
            "javac UDPClient.java -d " + CLIENT_DIR
        };

        for (String cmd : commands) {
            Process process = Runtime.getRuntime().exec(cmd);
            if (process.waitFor() != 0) {
                throw new IOException("Failed to compile: " + cmd);
            }
        }
        System.out.println("Compiled server and client classes.");
    }

    /**
     * Runs server and client.
     */
    private static void runTest() throws IOException, InterruptedException {
        // Start server
        ProcessBuilder serverPb = new ProcessBuilder(
            "java", "-cp", SERVER_DIR, "UDPServer", String.valueOf(SERVER_PORT)
        );
        serverPb.redirectOutput(new File("test/server.log"));
        Process server = serverPb.start();
        System.out.println("Server started on port " + SERVER_PORT);

        // Wait for server to start
        Thread.sleep(1000);

        // Start client
        ProcessBuilder clientPb = new ProcessBuilder(
            "java", "-cp", CLIENT_DIR, "UDPClient", HOSTNAME, String.valueOf(SERVER_PORT), "files.txt"
        );
        clientPb.redirectOutput(new File(CLIENT_DIR + "/client.log"));
        Process client = clientPb.start();
        System.out.println("Client started");

        // Wait for client to finish (timeout 20 seconds)
        if (!client.waitFor(20, java.util.concurrent.TimeUnit.SECONDS)) {
            client.destroy();
            throw new IOException("Client timed out");
        }

        // Stop server
        server.destroy();
        System.out.println("Test finished, server stopped.");
    }

    /**
     * Checks MD5 of downloaded file against server file.
     */
    private static void verifyResults() throws IOException, InterruptedException {
        System.out.println("Checking MD5 checksums...");

        // Get MD5 for server and client files
        String serverHash = getMD5(SERVER_DIR + "/" + TEST_FILE);
        String clientHash = getMD5(CLIENT_DIR + "/" + TEST_FILE);

        System.out.println("Server " + TEST_FILE + ": " + serverHash);
        System.out.println("Client " + TEST_FILE + ": " + clientHash);

        if (serverHash.equals(clientHash)) {
            System.out.println("MD5 checksums match!");
        } else {
            throw new IOException("MD5 checksums do not match");
        }
    }

    /**
     * Gets MD5 checksum for a file using md5sum command.
     */
    private static String getMD5(String filePath) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec("md5sum -b " + filePath);
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = reader.readLine();
        process.waitFor();
        if (line != null && line.contains(" ")) {
            return line.split("\\s+")[0];
        }
        return "";
    }

    /**
     * Deletes test directory.
     */
    private static void cleanup() {
        try {
            File testDir = new File("test");
            if (testDir.exists()) {
                for (File file : testDir.listFiles()) {
                    if (file.isDirectory()) {
                        for (File subFile : file.listFiles()) {
                            subFile.delete();
                        }
                    }
                    file.delete();
                }
                testDir.delete();
            }
            System.out.println("Cleaned up test directory.");
        } catch (Exception e) {
            System.err.println("Cleanup failed: " + e.getMessage());
        }
    }
}