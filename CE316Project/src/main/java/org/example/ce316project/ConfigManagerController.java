package org.example.ce316project;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class ConfigManagerController {

    @FXML
    private ListView<String> configListView;

    @FXML
    private TextField nameField;

    @FXML
    private TextField compilerPathField;

    @FXML
    private TextField compilerArgsField;

    @FXML
    private TextField runCmdField;

    @FXML
    public void initialize() {
        configListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                System.out.println("Selected: " + newValue);
            }
        });
    }

    @FXML
    void handleCreateNew(ActionEvent event) {
        configListView.getSelectionModel().clearSelection();
        nameField.clear();
        compilerPathField.clear();
        compilerArgsField.clear();
        runCmdField.clear();
    }

    @FXML
    void handleBrowseCompiler(ActionEvent event) {
        System.out.println("browsing");
    }

    @FXML
    void handleSave(ActionEvent event) {
        String name = nameField.getText();
        String path = compilerPathField.getText();
        System.out.println("saving config: " + name);
    }

    @FXML
    void handleDelete(ActionEvent event) {
        System.out.println("deleting config");
    }
}