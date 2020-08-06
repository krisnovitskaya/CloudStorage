public class ClientStatus {
//    private boolean authOK = false;
    private String login;
    //private int currentAction; //0 wait, 1 upload, 2 download
    private CurrentAction currentAction;
    private String currentFileName;
    private long currentFileSize;





    public ClientStatus(){
        this.login = null;
        this.currentAction = CurrentAction.COMMAND_SIZE;
        this.currentFileName = null;
        this.currentFileSize = -1;
    }

//    public boolean isAuthOK() {
//        return authOK;
//    }
//
//    public void setAuthOK(boolean authOK) {
//        this.authOK = authOK;
//    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public CurrentAction getCurrentAction() {
        return currentAction;
    }

    public void setCurrentAction(CurrentAction currentAction) {
        this.currentAction = currentAction;
    }

    public String getCurrentFileName() {
        return currentFileName;
    }

    public void setCurrentFileName(String currentFileName) {
        this.currentFileName = currentFileName;
    }

    public long getCurrentFileSize() {
        return currentFileSize;
    }

    public void setCurrentFileSize(long currentFileSize) {
        this.currentFileSize = currentFileSize;
    }
}