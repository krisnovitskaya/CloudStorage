package auth;

public interface AuthService {

    String getUserName(String login);
    void start();
    void stop();
}
