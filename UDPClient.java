
public class UDPClient {
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java UDPClient <hostname> <port> <file_list>");
            return;
        }
        System.out.println("Client started with args: " + String.join(" ", args));
        // TODO: Implement client logic
    }
}