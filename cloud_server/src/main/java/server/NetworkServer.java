package server;

import auth.AuthService;
import auth.BaseAuthService;
import client.ClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class NetworkServer {
    private final int port;
    private final List<ClientHandler> clients = new ArrayList<>();
    private final AuthService authService;


    public NetworkServer(int port) {
        this.port = port;
        this.authService = new BaseAuthService();
    }

    //запуск
    public void go() {
        try (ServerSocket serverSocket = new ServerSocket(port)){
            System.out.println("Сервер был успешно создан. Порт: " + port);
            authService.start();
            while(true){
                System.out.println("Ожидание клиентского подключения...");
                Socket clientSocket = serverSocket.accept();
                System.out.println("Клиент подключился...");
                createClientHandler(clientSocket);
            }
        } catch (IOException e) {
            System.out.println("Ошибка при работе сервера.");
            e.printStackTrace();
        }
    }

    //создание обработчика клиентского подключения
    private void createClientHandler(Socket clientSocket) {
        ClientHandler clientHandler = new ClientHandler(this, clientSocket);
        clientHandler.go();
    }

    public AuthService getAuthService() {
        return authService;
    }

    //для подключения после авторизации
    public synchronized void subscribe(ClientHandler clientHandler){
        System.out.println(clientHandler.getNick() + " подключился");
        clients.add(clientHandler);
    }

    //для исключения после выхода или разрыва соединения
    public synchronized void unsubscribe(ClientHandler clientHandler){
        System.out.println(clientHandler.getNick() + " отключился");
        clients.remove(clientHandler);
    }
}
