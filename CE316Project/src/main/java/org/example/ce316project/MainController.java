package org.example.ce316project;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class MainController {

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
        // will set up table columns here if we use SQL
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
        System.out.println("manage config this one will open a new page");
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