package DataBase;

import java.io.FileNotFoundException;
import java.sql.*;

public class DBService {
    private static Connection connection;
    private static PreparedStatement pstmt;

    public static boolean checkLoginPass(String login, String password){
        String prep = "select id from users where login = ? and password = ?";
        try {
            connect();
            pstmt = connection.prepareStatement(prep);
            pstmt.setString(1, login);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();
            if(rs.next()){
                return true;
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }finally{
            disconnect();
        }
        return false;
    }

    public static boolean addUser(String login, String password){
        String prep = "insert into users (login, password) values ( ?, ?)";
        try {
            connect();
            pstmt = connection.prepareStatement(prep);
            pstmt.setString(1, login);
            pstmt.setString(2, password);

            int x = pstmt.executeUpdate();
            if(x == 1){
                return true;
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }finally {
            disconnect();
        }
        return false;
    }

    public static void connect() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:logins.db");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
