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


        // 1 + 4 + 5 + 8
        out.writeInt(18);
        Thread.sleep(500);
        Path file = Paths.get("./clientTestdir/3.txt");
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
            FileInputStream fis = new FileInputStream("./clientTestdir/3.txt");
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


        //download


        out.writeInt(10);       //длина команды    1 + 4 + 5
        Thread.sleep(500);
        Path file2 = Paths.get("./cloudserver_logins/login1/1.txt");
        out.write(66);                                      //1
        String fileName2 = file2.getFileName().toString();
        System.out.println(file2.toAbsolutePath().toString());
        int fileNameLength2 = fileName2.length();
        out.writeInt(fileNameLength2);                           //4

        long fileLength2 = Files.size(file2);
        System.out.println("long file na servere " + fileLength2);
        out.write(fileName2.getBytes("UTF-8"));       //5

        Thread.sleep(500);


        File filedownload= new File("./clientTestdir/_1.txt");
        if(filedownload.exists()){
            filedownload.delete();
        }else{
            filedownload.createNewFile();
        }
        FileOutputStream fos = new FileOutputStream(filedownload, true);
        byte[] buffer = new byte[1024*1024];
        while (filedownload.length() != fileLength2) {
            int readBytes = in.read(buffer);
            System.out.println(readBytes);
            fos.write(buffer, 0, readBytes);
            System.out.println(filedownload.length());
        }
        System.out.println("file download");
        fos.close();


        out.close();
        socket.close();
    }
}
