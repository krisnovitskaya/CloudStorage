import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;


public class Controller implements Initializable {
    public ListView<String> lv;
    public TextField txt;
    public Button send;
    private Socket socket;
    private DataInputStream is;
    private DataOutputStream os;
    private final String clientFilesPath = "./common/src/main/resources/clientFiles";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            socket = new Socket("localhost", 8189);
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        File dir = new File(clientFilesPath);
        for (String file : dir.list()) {
            lv.getItems().add(file);
        }

    }
    // ./download fileName
    // ./upload fileName

    public void sendCommand(ActionEvent actionEvent) {
        String command = txt.getText();
        String [] op = command.split(" ");
        if (op[0].equals("./upload")) {
            upload(op);
        } else if (op[0].equals("./download")){
            download(op);
        }
    }

    private void upload(String[] op){
        File file = new File(clientFilesPath + "/" + op[1]);
        if (file.exists()) {
            System.out.println("upload start");
            try {
                os.write("#/put".getBytes()); //команда
                byte[] buf = new byte[128];
                int count;
                count = is.read(buf);
                System.out.println(count);

                if (checkOKcommand(buf)) {
                    os.write((byte) op[1].length());
                    os.write(op[1].getBytes());
                    is.read(buf);
                    if (checkOKcommand(buf)) {
                        os.writeLong(file.length());
                        is.read(buf);
                        if (checkOKcommand(buf)) {
                            FileInputStream fis = new FileInputStream(file);
                            byte[] buffer = new byte[1024];
                            while (fis.available() > 0) {
                                int readBytes = fis.read(buffer);
                                System.out.println(readBytes);
                                os.write(buffer, 0, readBytes);
                            }
                            fis.close();
                            System.out.println("wait server answer");
                            is.read(buf);
                            if (checkOKcommand(buf)) {
                                System.out.println("upload done");
                            } else {
                                System.out.println("error 4");
                            }
                        } else {
                            System.out.println("error 3");
                        }
                    } else {
                        System.out.println("error 2");
                    }
                } else {
                    System.out.println("error 1");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private boolean checkOKcommand(byte[] buf){
        StringBuilder sb = new StringBuilder();
        sb.append((char) buf[0]);
        sb.append((char) buf[1]);
        return sb.toString().equals("OK");
    }
    private void download(String[] op){
        try{
            os.write("#/get".getBytes()); //команда
            byte[] buf = new byte[128];
            int count;
            count = is.read(buf);
            System.out.println(count);

            if (checkOKcommand(buf)) { // отправляем длину имени и имя
                os.write((byte) op[1].length());
                os.write(op[1].getBytes());
                StringBuilder sb = new StringBuilder();
                sb.append((char) is.read());
                sb.append((char) is.read());
                if (sb.toString().equals("OK")) {
                    File file = new File(clientFilesPath + "/" + op[1]);
                    if(file.exists()){
                        file.delete();
                        file.createNewFile();
                    } else {
                        file.createNewFile();
                    }
                    byte[] bytes = new byte[8];
                    is.read(bytes);
                    long fileSize = byte2long(bytes);
                    os.write("#".getBytes());
                    FileOutputStream fos = new FileOutputStream(file);
                    byte[] buffer = new byte[1024];
                    while (file.length() != fileSize) {
                        int readBytes = is.read(buffer);
                        System.out.println(readBytes);
                        fos.write(buffer, 0, readBytes);
                    }
                    System.out.println("file download");
                    fos.close();
                } else {
                    System.out.println("no file on server");
                }
            } else {
                System.out.println("err1");
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }



    public long byte2long(byte[] b) throws IOException
    {
        ByteArrayInputStream baos = new ByteArrayInputStream(b);
        DataInputStream dos=new DataInputStream(baos);
        long result=dos.readLong();
        dos.close();
        return result;
    }


}



