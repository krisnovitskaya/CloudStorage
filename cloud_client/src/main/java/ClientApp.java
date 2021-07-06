import java.io.*;
import java.net.Socket;

public class ClientApp {
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 4040;

    public static void main(String[] args) {
        new NetworkClient(SERVER_IP, SERVER_PORT).go();
    }

}
