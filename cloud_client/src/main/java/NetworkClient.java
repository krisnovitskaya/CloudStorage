import java.io.*;
import java.net.Socket;

public class NetworkClient {
    private String server_ip;
    private int server_port;
    Socket socket;
    private volatile boolean isCancel = false;
    Thread print;
    Thread send;

    public NetworkClient(String serverIp, int serverPort) {
        server_ip = serverIp;
        server_port = serverPort;
    }

    public void setIsCancel(){
        isCancel = true;
    }
    public boolean isCancel(){
        return isCancel;
    }


    public void go(){
        try {
            socket = new Socket(server_ip, server_port);
            System.out.println("...entering in CloudStorage.");

            send = new Thread(new ClientListener(socket, this));
            print = new Thread(new ServerListener(socket,this));

            send.start();
            print.start();
            send.join();
            print.join();
 //       } catch (IOException e) {
        } catch (IOException  | InterruptedException e) {
            e.printStackTrace();
        }
    }





}
