import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

public class Controller implements Initializable {
    private Network network = Network.getInstance();
    private List<StorageFileInfo> listStorage = new ArrayList<>();


    @FXML
    Button uploadBtn;
    @FXML
    Button downloadBtn;
    @FXML
    Button deleteBtn;


    @FXML
    TableView<StorageFileInfo> storageFilesTable;
    @FXML
    TextField loginField;
    @FXML
    Button authBtn;
    @FXML
    CheckBox regField;
    @FXML
    PasswordField passField;
    @FXML
    HBox auth;


    @FXML
    TableView<FileInfo> filesTable;
    @FXML
    TextField pathField;
    @FXML
    ComboBox<String> disksBox;



    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            CountDownLatch networkStarter = new CountDownLatch(1);
            network.start(networkStarter);
            networkStarter.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        TableColumn<FileInfo, String> fileTypeColumn = new TableColumn<>();
        fileTypeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType().getName()));
        fileTypeColumn.setPrefWidth(24);

        TableColumn<FileInfo, String> filenameColumn = new TableColumn<>("Имя");
        filenameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFilename()));
        filenameColumn.setPrefWidth(240);

        TableColumn<FileInfo, Long> fileSizeColumn = new TableColumn<>("Размер");
        fileSizeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize()));
        fileSizeColumn.setCellFactory(column -> {
            return new TableCell<FileInfo, Long>() {
                @Override
                protected void updateItem(Long item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        String text = String.format("%,d bytes", item);
                        if (item == -1L) {
                            text = "[DIR]";
                        }
                        setText(text);
                    }
                }
            };
        });
        fileSizeColumn.setPrefWidth(120);


        filesTable.getColumns().addAll(fileTypeColumn, filenameColumn, fileSizeColumn);
        filesTable.getSortOrder().add(fileTypeColumn);

        disksBox.getItems().clear();
        for (Path p : FileSystems.getDefault().getRootDirectories()) {
            disksBox.getItems().add(p.toString());
        }
        disksBox.getSelectionModel().select(0);

        filesTable.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    Path path = Paths.get(pathField.getText()).resolve(filesTable.getSelectionModel().getSelectedItem().getFilename());
                    if (Files.isDirectory(path)) {
                        updateList(path);
                    }
                }
            }
        });

        updateList(Paths.get("."));

        //настройка серверной таблицы
        TableColumn<StorageFileInfo, String> storage_fileTypeColumn = new TableColumn<>();
        storage_fileTypeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType().getName()));
        storage_fileTypeColumn.setPrefWidth(24);

        TableColumn<StorageFileInfo, String> storage_filenameColumn = new TableColumn<>("Имя");
        storage_filenameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFilename()));
        storage_filenameColumn.setPrefWidth(240);

        TableColumn<StorageFileInfo, Long> storage_fileSizeColumn = new TableColumn<>("Размер");
        storage_fileSizeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize()));
        storage_fileSizeColumn.setCellFactory(column -> {
            return new TableCell<StorageFileInfo, Long>() {
                @Override
                protected void updateItem(Long item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        String text = String.format("%,d bytes", item);
                        if (item == -1L) {
                            text = "[DIR]";
                        }
                        setText(text);
                    }
                }
            };
        });
        storage_fileSizeColumn.setPrefWidth(120);


        storageFilesTable.getColumns().addAll(storage_fileTypeColumn, storage_filenameColumn, storage_fileSizeColumn);
        storageFilesTable.getSortOrder().add(storage_fileTypeColumn);
    }

    public void updateList(Path path) {
        try {
            pathField.setText(path.normalize().toAbsolutePath().toString());
            filesTable.getItems().clear();
            filesTable.getItems().addAll(Files.list(path).map(FileInfo::new).collect(Collectors.toList()));
            filesTable.sort();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "По какой-то причине не удалось обновить список файлов", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void btnPathUpAction(ActionEvent actionEvent) {
        Path upperPath = Paths.get(pathField.getText()).getParent();
        if (upperPath != null) {
            updateList(upperPath);
        }

    }

    public void selectDiskAction(ActionEvent actionEvent) {
        ComboBox<String> element = (ComboBox<String>) actionEvent.getSource();
        updateList(Paths.get(element.getSelectionModel().getSelectedItem()));

    }

    public String getSelectedFilename() {
        if (!filesTable.isFocused()) {
            return null;
        }
        return filesTable.getSelectionModel().getSelectedItem().getFilename();
    }

    public long getSelectedFileSize() {
        return filesTable.getSelectionModel().getSelectedItem().getSize();
    }

    public String getCurrentPath() {
        return pathField.getText();
    }

    public void tryAuth(ActionEvent actionEvent) {
        if(loginField.getText() == null || loginField.getText() == "\\s+" || passField.getText() == null || passField.getText() == "\\s+") return;
        StringBuilder sb = new StringBuilder();
        sb.append(loginField.getText().trim() + " " + passField.getText().trim());
        if(regField.isSelected()){
            sb.append(" &");
        } else {
            sb.append(" #");
        }

        try {
            network.sendAuth(sb.toString(), new BoolCallback() {
                @Override
                public void callback(boolean getAuth) {
                    if(getAuth){
                        auth.setVisible(false);
                        makeList(network.askStorageInfo());
                        updateStorageList();
                    } else {
                        new Alert(Alert.AlertType.INFORMATION, "Неправильные данные", ButtonType.APPLY).showAndWait();
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void makeList(String info) {
        listStorage.clear();
        String[] infoList = info.split(" ");
        for(int i = 0; i < infoList.length; i += 2){
            listStorage.add(new StorageFileInfo(infoList[i], Long.parseLong(infoList[i + 1])));
        }
    }
    public void updateStorageList() {
        storageFilesTable.getItems().clear();
        storageFilesTable.getItems().addAll(listStorage);
        for (StorageFileInfo storageFileInfo : listStorage) {
            storageFileInfo.print();
        }
        storageFilesTable.sort();
    }


    public void btnExitAction(ActionEvent actionEvent) {
        exitAction();
    }

    public void exitAction() {
        network.closeConnection();
        Platform.exit();
    }

//    public void btnDownload(ActionEvent actionEvent) {
//        
//    }

    public void btnUpload(ActionEvent actionEvent) {
        hideButtons();
        network.uploadFIle(getCurrentPath(), getSelectedFilename(), getSelectedFileSize(), new BoolCallback() {
            @Override
            public void callback(boolean bool) {
                if(bool) new Alert(Alert.AlertType.INFORMATION, "Файл успешно загружен в облако", ButtonType.APPLY).showAndWait();
                makeList(network.askStorageInfo());
                updateStorageList();
                showButtons();
            }
        });
    }

    private void showButtons(){
        uploadBtn.setVisible(true);
        downloadBtn.setVisible(true);
        deleteBtn.setVisible(true);
    }
    private void hideButtons(){
        uploadBtn.setVisible(false);
        downloadBtn.setVisible(false);
        deleteBtn.setVisible(false);
    }
}