import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Это просто для теста сервера
 */
public class TestClient {
    public static void main(String[] args) throws Exception {
        Socket socket = new Socket("localhost", 8189);
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        DataInputStream in = new DataInputStream(socket.getInputStream());
        String logpas = "login1 pass1 #";
        System.out.println("logpas length in bytes" + logpas.getBytes("UTF-8").length);
        out.writeInt(logpas.getBytes("UTF-8").length);
        Thread.sleep(500);
        out.write(logpas.getBytes("UTF-8"));
        System.out.println("send logpas");
        Thread.sleep(500);
        if((char)in.readByte() == '%') {

            // 1 + 4 + 5 + 8
            out.writeInt(18);
            Thread.sleep(500);
            Path file = Paths.get("./clientTestdir/1.txt");
            out.write(65);                                      //1
            String fileName = file.getFileName().toString();
            System.out.println(fileName);
            int fileNameLength = fileName.length();
            out.writeInt(fileNameLength);                           //4

            long fileLength = Files.size(file);
            out.write(fileName.getBytes("UTF-8"));       //5
            out.writeLong(fileLength);                              //8
            Thread.sleep(500);


            if ((char) in.readByte() == '%') {
                System.out.println("answer server ok");
                FileInputStream fis = new FileInputStream("./clientTestdir/1.txt");
                byte[] buffer = new byte[1024 * 5];
                while (fis.available() > 0) {
                    int readBytes = fis.read(buffer);
                    System.out.println(readBytes);
                    out.write(buffer, 0, readBytes);
                }
                fis.close();
                System.out.println("wait server answer");
            }
            if ((char) in.readByte() == '%') {
                System.out.println("complete");

            }
        } else {
            System.out.println("wrong logpass");
        }


        out.close();
        socket.close();
    }
}
