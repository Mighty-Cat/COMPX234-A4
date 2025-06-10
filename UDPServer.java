
public class UDPServer {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java UDPServer <port>");
            return;
        }
        System.out.println("Server started on port: " + args[0]);
        // TODO: Implement server logic
    }
}