import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
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

    public void sendAuth(String authData, BoolCallback callback) throws IOException{

        //тест
        //String logpas = "login1 pass1 #";
        System.out.println("logpas length in bytes" + authData.getBytes("UTF-8").length);
        out.writeInt(authData.getBytes(StandardCharsets.UTF_8).length);
        if (in.readByte() == Command.commandOK) {
            out.write(authData.getBytes("UTF-8"));
            System.out.println("send logpas");
            callback.callback(in.readByte() == Command.commandOK);
        } else {
            System.err.println("ошибка ответа сервера");
        }
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
    public String askStorageInfo(){

        String storageInfo;
        byte[] byteInfo;
        try{
            out.writeInt(1);
            if (in.readByte() == Command.commandOK) {
                out.writeByte(Command.storage_INFO);
                int size = in.readInt();
                byteInfo = new byte[size];
                in.read(byteInfo);
                storageInfo = new String(byteInfo, StandardCharsets.UTF_8);
                System.out.println(storageInfo);
                return storageInfo;
            } else {
                System.err.println("ошибка ответа сервера");
                return new String(); //заглушка
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        return new String(); //заглушка
    }

    public void uploadFIle(String path, String selectedFilename, long selectedFileSize, BoolCallback boolCallback) {
        int commandSize = Byte.BYTES + Integer.BYTES + selectedFilename.getBytes(StandardCharsets.UTF_8).length + Long.BYTES;
        try {
            out.writeInt(commandSize);
            System.out.println(in.readByte()); //okbyte
            out.writeByte(Command.upload);
            out.writeInt(selectedFilename.getBytes(StandardCharsets.UTF_8).length);
            out.write(selectedFilename.getBytes(StandardCharsets.UTF_8));
            out.writeLong(selectedFileSize);
            System.out.println(in.readByte()); //okbyte
            FileInputStream fis = new FileInputStream(path + "/" + selectedFilename);
            byte[] buffer = new byte[1024 * 5 * 1024];
            while (fis.available() > 0) {
                int readBytes = fis.read(buffer);
                System.out.println(readBytes);
                out.write(buffer, 0, readBytes);
            }
            fis.close();
            boolCallback.callback(in.readByte() == Command.commandOK);
        }catch (IOException e){
            e.printStackTrace();
        }

    }
}
