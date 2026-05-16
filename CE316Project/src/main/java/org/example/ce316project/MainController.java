package org.example.ce316project;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import java.nio.file.Path;

public class MainController {


    private AssignmentManager manager;

    @FXML
    private ListView<String> studentListView;

    @FXML
    private ComboBox<String> configComboBox;

    @FXML
    private TextField directoryTextField;

    @FXML
    private TableView<?> resultsTableView;

    @FXML
    private TableColumn<?, ?> studentIdCol;

    @FXML
    private TableColumn<?, ?> statusCol;

    @FXML
    private TableColumn<?, ?> detailsCol;

    @FXML
    public void initialize() {
        // Initialize the manager when the GUI loads
        manager = new AssignmentManager();
        System.out.println("AssignmentManager connected.");
    }

    // menu actions
    @FXML
    void handleNewProject(ActionEvent event) {
        System.out.println("new");
    }

    @FXML
    void handleOpenProject(ActionEvent event) {
        System.out.println("open");
    }

    @FXML
    void handleSaveProject(ActionEvent event) {
        System.out.println("save");
    }

    @FXML
    void handleExit(ActionEvent event) {
        System.exit(0);
    }

    @FXML
    void handleManageConfigs(ActionEvent event) {

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("config-manager.fxml"));
            javafx.scene.Parent root = fxmlLoader.load();
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Manage Configurations");
            stage.setScene(new javafx.scene.Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }

        manager.createConfiguration("Test Config", Path.of("/usr/bin/gcc"), "-o main", "./main", Path.of("out.txt"));

        // Clear the current items and update the ComboBox with the new configuration list
        configComboBox.getItems().clear();
        for (Configuration conf : manager.getConfigurations()) {
            configComboBox.getItems().add(conf.getConfigName());
        }

        System.out.println("Configuration list updated in the interface.");
    }
    @FXML
    void handleImportConfig(ActionEvent event) {
        System.out.println("import conf");
    }

    @FXML
    void handleExportConfig(ActionEvent event) {
        System.out.println("export co");
    }

    @FXML
    void handleOpenManual(ActionEvent event) {
        System.out.println("manual");
    }

    @FXML
    void handleBrowseDirectory(ActionEvent event) {
        System.out.println("browse");
    }

    @FXML
    void handleRunAssignments(ActionEvent event) {
        System.out.println("run");
    }
}