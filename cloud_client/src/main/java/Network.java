import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

public class Network {

    private static Network ourInstance = new Network();

    public static Network getInstance() {
        return ourInstance;
    }

    private Network() {
    }

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;



    public void start(CountDownLatch countDownLatch) {

        try {
            socket = new Socket("localhost", 8189);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            countDownLatch.countDown();

        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    public void sendAuth() throws IOException, InterruptedException {

        //тест
        String logpas = "login1 pass1 #";
        System.out.println("logpas length in bytes" + logpas.getBytes("UTF-8").length);
        out.writeInt(logpas.getBytes(StandardCharsets.UTF_8).length);
        Thread.sleep(500);
        out.write(logpas.getBytes("UTF-8"));
        System.out.println("send logpas");
        Thread.sleep(500);
    }

    public void closeConnection(){
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
