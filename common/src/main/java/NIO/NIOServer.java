package NIO;



import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.DoubleToIntFunction;

public class NIOServer implements Runnable {

    private ServerSocketChannel server;
    private Selector selector;
    private int count = 0;
    private String serverFilePath = "./common/src/main/resources/serverFiles";
    private ByteBuffer buffer = ByteBuffer.allocate(254);


    public NIOServer() throws IOException {
        server = ServerSocketChannel.open();
        server.socket().bind(new InetSocketAddress(8189));
        server.configureBlocking(false);
        selector = Selector.open();
        server.register(selector, SelectionKey.OP_ACCEPT);
    }

    @Override
    public void run() {
        try {
            System.out.println("Сервер запущен (Порт: 8189)");
            Iterator<SelectionKey> iter;
            SelectionKey key;
            while (server.isOpen()) {
                selector.select();
                iter = this.selector.selectedKeys().iterator();
                while (iter.hasNext()) {
                    key = iter.next();
                    iter.remove();
                    if (key.isAcceptable()) this.handleAccept(key);
                    if (key.isReadable()) this.handleRead(key);
                    if (key.isValid() && key.isWritable()) this.handleWrite(key);
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void handleWrite(SelectionKey key) throws IOException {
        ClientStatus clientStatus = (ClientStatus) key.attachment();
        RandomAccessFile raf = new RandomAccessFile(serverFilePath + "/" + clientStatus.getLogin() + "/" + clientStatus.getCurrentFileName(), "r");
        FileChannel rafChannel = raf.getChannel();
        buffer.clear();
        raf.seek(clientStatus.getCurrentFileSize());
        int readBytes = rafChannel.read(buffer);
        buffer.flip();
        SocketChannel socketChannel = (SocketChannel) key.channel();
        socketChannel.write(buffer);
        clientStatus.setCurrentFileSize(clientStatus.getCurrentFileSize() + (long)readBytes);
        if(clientStatus.getCurrentFileSize() == raf.length()){
            clientStatus.setCurrentFileName(null);
            clientStatus.setCurrentFileSize(-1);
            clientStatus.setCurrentAction(0);
            key.interestOps(SelectionKey.OP_READ);
        }
        buffer.clear();
    }

    private void handleAccept(SelectionKey key) throws IOException {
        SocketChannel socketChannel = ((ServerSocketChannel) key.channel()).accept();
        count++;
        socketChannel.configureBlocking(false);
        String userName = "user" + count;
        String clientDirName = serverFilePath + "/" + userName;
        Path clientDir = Paths.get(clientDirName);
        if (!Files.exists(clientDir)) {
            Files.createDirectory(clientDir);
        }
        socketChannel.register(selector, SelectionKey.OP_READ, new ClientStatus(userName));
        System.out.println("Подключился новый клиент " + userName);
    }


    private void handleRead(SelectionKey key) throws IOException, InterruptedException {
        Thread.sleep(100);
        SocketChannel socketChannel = (SocketChannel) key.channel();

        int read;
        try {
            read = socketChannel.read(buffer);
        } catch (IOException e) {
            key.cancel();
            socketChannel.close();
            System.out.println("Forceful shutdown");
            return;
        }

        System.out.println("read " + read);
        buffer.flip();

        ClientStatus clientStatus = (ClientStatus) key.attachment();
        int status = clientStatus.getCurrentAction();
        System.out.println("status " + status);
        switch (status){
            case 0:
                checkCommand(socketChannel, clientStatus, buffer);
                break;
            case 1:
                upload(socketChannel, clientStatus, buffer, key);
                break;
            case 2:
                download(socketChannel, clientStatus, buffer, key);
                break;
        }
        buffer.clear();

    }

    public byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(x);
        return buffer.array();
    }
    private void download(SocketChannel socketChannel, ClientStatus clientStatus, ByteBuffer buffer, SelectionKey key) throws IOException {
        if(clientStatus.getCurrentFileName() == null){
            byte fileNameSize = buffer.get();
            StringBuilder sb = new StringBuilder();
            for(byte i = 0; i < fileNameSize; i++){
                sb.append((char)buffer.get());
            }
            clientStatus.setCurrentFileName(sb.toString());
            if(Files.exists(Paths.get(serverFilePath + "/" + clientStatus.getLogin() + "/" + sb.toString()))){
                clientStatus.setCurrentFileName(sb.toString());
                socketChannel.write(ByteBuffer.wrap("OK".getBytes()));
                RandomAccessFile raf = new RandomAccessFile(serverFilePath + "/" + clientStatus.getLogin() + "/" + clientStatus.getCurrentFileName(), "r");
                socketChannel.write(ByteBuffer.wrap(longToBytes(raf.length())));
                raf.close();
            } else {
                clientStatus.setCurrentAction(0);
                socketChannel.write(ByteBuffer.wrap("NO".getBytes()));
            }
        } else if(clientStatus.getCurrentFileName() != null && clientStatus.getCurrentFileSize() == -1){
            if((char)buffer.get() == '#'){
                clientStatus.setCurrentFileSize(0);
                key.interestOps(SelectionKey.OP_WRITE);
            }
        }
    }

    private void upload(SocketChannel socketChannel, ClientStatus clientStatus, ByteBuffer buffer, SelectionKey key) throws IOException {
        if(clientStatus.getCurrentFileName() == null){
            byte fileNameSize = buffer.get();
            StringBuilder sb = new StringBuilder();
            for(byte i = 0; i < fileNameSize; i++){
                sb.append((char)buffer.get());
            }
            clientStatus.setCurrentFileName(sb.toString());
            socketChannel.write(ByteBuffer.wrap("OK".getBytes()));
        } else if(clientStatus.getCurrentFileSize() == -1){
            byte[] byteLong = new byte[8];
            for(int i = 0; i < 8; i++){
                byteLong[i] = buffer.get();
            }
            clientStatus.setCurrentFileSize(ByteBuffer.wrap(byteLong).getLong());
            Path path = Paths.get(serverFilePath + "/" + clientStatus.getLogin() + "/" + clientStatus.getCurrentFileName());
            Files.deleteIfExists(path);
            socketChannel.write(ByteBuffer.wrap("OK".getBytes()));
        }else if(clientStatus.getCurrentFileName() != null && clientStatus.getCurrentFileSize() != -1){
            RandomAccessFile raf = new RandomAccessFile(serverFilePath + "/" + clientStatus.getLogin() + "/" + clientStatus.getCurrentFileName(), "rw");
            FileChannel rafChannel = raf.getChannel();
            raf.seek(raf.length());
            rafChannel.write(buffer);
            //System.out.println(" current file length = " + raf.length() + "/ need file length = " + clientStatus.getCurrentFileSize());

            if(raf.length() == clientStatus.getCurrentFileSize()){
                System.out.println("file upload done " + clientStatus.getCurrentFileName());
                clientStatus.setCurrentAction(0);
                clientStatus.setCurrentFileSize(-1);
                clientStatus.setCurrentFileName(null);
                rafChannel.close();
                raf.close();
                socketChannel.write(ByteBuffer.wrap("OK".getBytes()));
            }
        }
    }

    private void checkCommand(SocketChannel socketChannel, ClientStatus clientStatus, ByteBuffer buffer) throws IOException {
        if((char)buffer.get() == '#'){
            StringBuilder command = new StringBuilder();
            for(int i = 0; i < 4; i++){
                command.append((char)buffer.get());
            }
            if(command.toString().equals("/put")){
                clientStatus.setCurrentAction(1);
                socketChannel.write(ByteBuffer.wrap("OK".getBytes()));
            } else if(command.toString().equals("/get")){
                clientStatus.setCurrentAction(2);
                socketChannel.write(ByteBuffer.wrap("OK".getBytes()));
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new Thread(new NIOServer()).start();
    }
}




