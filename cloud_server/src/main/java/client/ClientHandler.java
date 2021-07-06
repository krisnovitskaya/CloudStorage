package client;


import server.NetworkServer;

import java.io.*;
import java.net.Socket;

public class ClientHandler {

    private final NetworkServer networkServer;
    private final Socket clientSocket;

    private DataInputStream in;
    private DataOutputStream out;

    private String nick;
    private File clientDir;




    public ClientHandler(NetworkServer networkServer, Socket clientSocket) {
        this.networkServer = networkServer;
        this.clientSocket = clientSocket;
    }


    public void go(){
        doHandle(clientSocket);
    }

    private void doHandle(Socket clientSocket) {
        try {
            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());

            //запуск авторизации с последующим чтением ввода в отдельном потоке
            new Thread(() -> {

                try {
                    authentication();
                    readingCommands();
                } catch (IOException e) {
                    System.out.println("Соединение с клиентом " + nick + " завершено.");
                } finally {
                    closeConnection();
                }

            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeConnection() {

        try{
            networkServer.unsubscribe(this); //отписаться
            clientSocket.close(); //закрыть сокет и ридеры ввода/вывода
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    private void readingCommands() throws IOException {
        while (true){
            String message = in.readUTF();
            if("/end".equals(message)){  //команда выхода от пользователя
                networkServer.unsubscribe(this);
                return;
            }
            System.out.println(nick + " want " + message);
            if(message.startsWith("/upload")){      //загрузка на сервер
                uploadFileServer();
            }else if(message.startsWith("/download")){
                String[] filename = message.split(" ", 2);
                File file = new File(clientDir + "/" + filename[1]);
                if(file.exists())  {
                    sendMessage("/download" + " " + nick + " "+ filename[1]);
                    downloadFileClient(file);
                } else {
                    sendMessage("wrong filename");
                }

            } else if (message.startsWith("/info")){
                sendInfo();
            }

        }
    }

    private void downloadFileClient(File file) throws IOException{

        InputStream is = new FileInputStream(file);
        byte [] buffer = new byte[8192];

        out.writeUTF(String.valueOf(file.length())); //длина файла
        while (true) {
            int readBytes = is.read(buffer);
            //System.out.println(readBytes);
            if(readBytes == -1) break;
            out.write(buffer, 0, readBytes);
        }
        System.out.println("done");
    }

    private void uploadFileServer() throws IOException{
        sendMessage("загрузка");
        String fileName = in.readUTF();
        File file = new File(clientDir.getPath() +"/" + fileName);
        file.createNewFile();
        long filelength = Long.parseLong(in.readUTF()); //передача длины для выхода из цикла
        System.out.println(filelength);
        try (FileOutputStream os = new FileOutputStream(file)) {
            byte[] buffer = new byte[8192];
            while (true) {
                if (file.length() == filelength) break;
                int r = in.read(buffer);
                //System.out.println(r);
                os.write(buffer, 0, r);
            }
        }
        System.out.println("File uploaded!");
    }


    private void sendInfo() throws IOException {
        File[] files = clientDir.listFiles();
        if(files.length == 0) {
            sendMessage("папка пуста");
        } else {
            for (File file1 : files) {
                sendMessage(file1.getName());
            }
        }
    }

    private void authentication() throws IOException {
 //       while(true) {
            sendMessage("Введите логин:");
            String login = in.readUTF();
            nick = networkServer.getAuthService().getUserName(login);
            chooseDir();
            sendMessage(" Авторизация прошла успешно. Ваш ник: "+ nick);
            networkServer.subscribe(this);

  //      }
    }

    private void chooseDir() {
        clientDir = new File("./cloud_server/src/main/java/dirs/" + nick);
        if(!clientDir.exists()) clientDir.mkdir();
    }

    public void sendMessage(String message) throws IOException {
        out.writeUTF(message);
    }

    public String getNick(){
        return nick;
    }
}
