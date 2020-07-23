package auth;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class BaseAuthService implements AuthService {

    private static int countID;
    private static List<String> userData;

    private static final File userCountSave = new File("./cloud_server/src/main/java/auth/userdata.txt"); //хранение существующих id


    public BaseAuthService(){
        userData = new ArrayList<>();
        //File userCountSave = new File("E:\\java\\cloudstorage\\cloud_server\\src\\main\\java\\auth\\userdata.txt");
        try {
            if(!userCountSave.exists()) userCountSave.createNewFile();
        } catch (IOException e){
            System.out.println("ошибка создания файла");
        }
        try{

            try (BufferedReader reader = new BufferedReader(new FileReader(userCountSave))){
                if(reader.ready()){
                countID = Integer.parseInt(reader.readLine());
            } else {
                countID = 0;
            } }
        } catch (IOException e) {
            e.printStackTrace();
        }

        for(int i = 0; i < countID; i++){
            userData.add("login" + i);
        }
    }

    @Override
    public String getUserName(String login) { //упрощенный логин без пароля
        for (String data : userData) {
            if(data.equals(login)) return login;
        }
        countID++;
        updateCount();
        userData.add("login" + countID);
        return "login" + countID;
    }


    //обновить файл с сохраненнными id
    private void updateCount() {
        try {
            FileWriter writer = new FileWriter(userCountSave);
            writer.write(String.valueOf(countID));
            writer.close();
        } catch ( IOException e ){
            System.out.println("Ошибка записи userCountSave");
        }
    }

    @Override
    public void start() {
        System.out.println("Сервис аутентификации запущен.");
    }

    @Override
    public void stop() {
        System.out.println("Сервис аутентификации остановлен.");
    }
}
