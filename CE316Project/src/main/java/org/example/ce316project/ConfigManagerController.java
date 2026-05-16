package org.example.ce316project;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;

/**
 * ConfigManagerController — Controller for the configuration management screen.
 *
 * Responsibilities:
 *  - Lists existing configurations
 *  - Creates a new configuration (saves it via AssignmentManager)
 *  - Edits and updates the selected configuration
 *  - Deletes the selected configuration
 *  - Selects the compiler file using FileChooser
 */
public class ConfigManagerController {

    // ─────────────────────────────────────────────
    // FXML Components
    // ─────────────────────────────────────────────

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
    private TextField expectedOutputField;

    // ─────────────────────────────────────────────
    // Dependencies
    // ─────────────────────────────────────────────

    /** Injected by MainController. */
    private AssignmentManager manager;

    /** Name of the selected configuration from the list. If null, we are in create mode. */
    private String selectedConfigName = null;

    // ─────────────────────────────────────────────
    // Initialization
    // ─────────────────────────────────────────────

    public void setManager(AssignmentManager manager) {
        this.manager = manager;
        refreshList();
    }

    @FXML
    public void initialize() {
        configListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        populateForm(newValue);
                    }
                }
        );
    }

    // ─────────────────────────────────────────────
    // List & Form Management
    // ─────────────────────────────────────────────

    private void refreshList() {
        if (manager == null) return;
        configListView.getItems().clear();
        for (Configuration config : manager.getConfigurations()) {
            configListView.getItems().add(config.getConfigName());
        }
    }

    /**
     * Fills the form fields with the details of the selected configuration.
     * The name field is locked — the name cannot be changed, while other fields can be edited.
     */
    private void populateForm(String configName) {
        if (manager == null) return;

        Configuration config = manager.findConfigurationByName(configName);
        if (config == null) return;

        selectedConfigName = configName;

        nameField.setText(config.getConfigName());
        nameField.setEditable(false);
        compilerPathField.setText(
                config.getCompilerPath() != null ? config.getCompilerPath().toString() : ""
        );
        compilerArgsField.setText(
                config.getCompileArguments() != null ? config.getCompileArguments() : ""
        );
        runCmdField.setText(
                config.getExecutionCommand() != null ? config.getExecutionCommand() : ""
        );
        expectedOutputField.setText(
                config.getExpectedOutputFilePath() != null
                        ? config.getExpectedOutputFilePath().toString()
                        : ""
        );
    }

    /**
     * Clears all form fields to prepare for a new configuration input.
     */
    private void clearForm() {
        configListView.getSelectionModel().clearSelection();
        selectedConfigName = null;
        nameField.clear();
        nameField.setEditable(true);
        compilerPathField.clear();
        compilerArgsField.clear();
        runCmdField.clear();
        expectedOutputField.clear();
    }

    // ─────────────────────────────────────────────
    // Button Handlers
    // ─────────────────────────────────────────────

    @FXML
    void handleCreateNew(ActionEvent event) {
        clearForm();
    }

    @FXML
    void handleBrowseCompiler(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Compiler");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Executable Files", "*", "*.exe"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        Stage stage = (Stage) compilerPathField.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            compilerPathField.setText(selectedFile.getAbsolutePath());
            System.out.println("[Config] Compiler selected: " + selectedFile.getAbsolutePath());
        }
    }

    /**
     * "Save" button handler.
     * If selectedConfigName is null → new record
     * If selectedConfigName is not null → update existing record
     */
    @FXML
    void handleSave(ActionEvent event) {
        if (manager == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Manager is not linked.");
            return;
        }

        String name        = nameField.getText().trim();
        String compilerStr = compilerPathField.getText().trim();
        String cArgs       = compilerArgsField.getText().trim();
        String execCmd     = runCmdField.getText().trim();
        String outputStr   = expectedOutputField.getText().trim();

        if (name.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Field", "Configuration name cannot be empty.");
            return;
        }
        if (execCmd.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Field", "Execution command cannot be empty.");
            return;
        }

        Path compiler = compilerStr.isEmpty() ? null : Path.of(compilerStr);
        Path output   = outputStr.isEmpty()   ? null : Path.of(outputStr);
        String args   = cArgs.isEmpty()       ? null : cArgs;

        if (selectedConfigName != null) {
            // ── Update Mode ──────────────────────
            Configuration updated = manager.updateConfiguration(
                    selectedConfigName, compiler, args, execCmd, output
            );
            if (updated != null) {
                refreshList();
                clearForm();
                showAlert(Alert.AlertType.INFORMATION, "Updated",
                        "Configuration updated: " + selectedConfigName);
                System.out.println("[Config] Updated: " + selectedConfigName);
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Update failed.");
            }
        } else {
            // ── Create Mode ──────────────────────
            Configuration saved = manager.createConfiguration(
                    name, compiler, args, execCmd, output
            );
            if (saved != null) {
                refreshList();
                clearForm();
                showAlert(Alert.AlertType.INFORMATION, "Saved",
                        "Configuration saved: " + name);
                System.out.println("[Config] Saved: " + name);
            } else {
                showAlert(Alert.AlertType.ERROR, "Error",
                        "Could not save. A record with the same name might already exist.");
            }
        }
    }

    @FXML
    void handleDelete(ActionEvent event) {
        String selected = configListView.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection",
                    "Please select a configuration to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Confirmation");
        confirm.setHeaderText("Are you sure you want to delete this configuration?");
        confirm.setContentText(selected);

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                manager.deleteConfiguration(selected);
                refreshList();
                clearForm();
                System.out.println("[Config] Deleted: " + selected);
                showAlert(Alert.AlertType.INFORMATION, "Deleted",
                        "Configuration deleted: " + selected);
            }
        });
    }

    // ─────────────────────────────────────────────
    // Helper Methods
    // ─────────────────────────────────────────────

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}