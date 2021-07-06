import java.io.*;
import java.net.Socket;

public class ClientListener implements Runnable{
    BufferedReader reader;
    DataOutputStream out;
    Socket socket;
    NetworkClient networkClient;
    public static final String END_COMMAND = "/end";

    public ClientListener(Socket clientSocket, NetworkClient networkClient){
        try{
            socket = clientSocket;
            this.networkClient = networkClient;
            out =  new DataOutputStream(socket.getOutputStream());
            reader = new BufferedReader(new InputStreamReader(System.in));
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        String messageServer;
        try {
            while(!networkClient.isCancel()) {
                    messageServer = reader.readLine();
                    if(!messageServer.startsWith("/")) out.writeUTF(messageServer); //login
                    //загрузка на сервер
                    if(messageServer.startsWith("/upload")){
                        String[] parts = messageServer.split(" ", 2);
                        if(new File(parts[1]).exists()){
                            out.writeUTF(messageServer);
                            sendFile(new File(parts[1]));
                        } else {
                            System.out.println("wrong path");
                        }
                    }
                    //скачивание с сервера
                    if(messageServer.startsWith("/download")){
                        out.writeUTF(messageServer);
                        System.out.println("запрос на скачивание");
                    }
                if(messageServer.startsWith("/info")){
                    out.writeUTF(messageServer);
                    System.out.println("запрос info");
                }
                    if(messageServer.equals(END_COMMAND)){
                        networkClient.setIsCancel();
                    }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        byte [] buffer = new byte[8192];
        out.writeUTF(file.getName());
        out.writeUTF(String.valueOf(file.length())); //длина файла
        while (true) {
                int readBytes = is.read(buffer);
            //System.out.println(readBytes);
                if(readBytes == -1) break;
                out.write(buffer, 0, readBytes);
        }
        System.out.println("done");

        }



    }

