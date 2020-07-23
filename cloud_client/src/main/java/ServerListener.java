import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class ServerListener implements Runnable {
    DataInputStream in;
    Socket socket;
    NetworkClient networkClient;

    public ServerListener(Socket clientSocket, NetworkClient networkClient) {
        this.networkClient = networkClient;
        try {
            socket = clientSocket;
            in = new DataInputStream(socket.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            String message;
            while (!networkClient.isCancel()) {
                message = in.readUTF();
                System.out.println("Server:" + message);
            }
        } catch (IOException e) {
            System.out.println("Connection has been closed.");
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
