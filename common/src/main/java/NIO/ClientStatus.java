package NIO;

public class ClientStatus {
    private String login;
    private int currentAction; //0 wait, 1 upload, 2 download
    private String currentFileName;
    private long currentFileSize;


    public ClientStatus(String login){
        this.login = login;
        this.currentAction = 0;
        this.currentFileName = null;
        this.currentFileSize = -1;
    }

    public String getLogin() {
        return login;
    }

    public int getCurrentAction() {
        return currentAction;
    }

    public String getCurrentFileName() {
        return currentFileName;
    }

    public long getCurrentFileSize() {
        return currentFileSize;
    }

    public void setCurrentAction(int currentAction) {
        this.currentAction = currentAction;
    }

    public void setCurrentFileName(String currentFileName) {
        this.currentFileName = currentFileName;
    }

    public void setCurrentFileSize(long currentFileSize) {
        this.currentFileSize = currentFileSize;
    }
}
