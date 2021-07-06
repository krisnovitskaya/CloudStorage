import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
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
                if(message.startsWith("/download")) {
                    downloadFile(message);
                } else System.out.println("Server:" + message);
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

    private void downloadFile(String message) throws IOException {

        String[] strings = message.split(" ", 3);    // download nick filename

        //создается папка по логину
        File dir = new File("./common/loginDirs/" + strings[1]);
        if(!dir.exists()) dir.mkdir();
        File file = new File("./common/loginDirs/" + strings[1] + "/" + strings[2]);
        long fileLength = Long.parseLong(in.readUTF());
        try (FileOutputStream os = new FileOutputStream(file)) {
            byte[] buffer = new byte[8192];
            while (true) {
                if (file.length() == fileLength) break;
                int r = in.read(buffer);
                //System.out.println(r);
                os.write(buffer, 0, r);
            }
        }
        System.out.println("File uploaded!" + " path: " + file.getAbsolutePath());
    }
}
