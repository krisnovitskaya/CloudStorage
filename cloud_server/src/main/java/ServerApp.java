import server.NetworkServer;

public class ServerApp {
    private static final int DEFAULT_PORT = 4040;

    public static void main(String[] args) {
        new NetworkServer(DEFAULT_PORT).go();
    }
}
