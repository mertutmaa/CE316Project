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
 *  - Display existing configurations in a list
 *  - Create new configurations (saved via AssignmentManager)
 *  - Edit and update selected configurations
 *  - Delete selected configurations
 *  - Browse for a compiler executable using a FileChooser
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

    /** Injected by MainController before this screen is shown. */
    private AssignmentManager manager;

    /** Name of the currently selected configuration. Null means new-record mode. */
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

    /**
     * Refreshes the ListView with the current configuration list from AssignmentManager.
     */
    private void refreshList() {
        if (manager == null) return;
        configListView.getItems().clear();
        for (Configuration config : manager.getConfigurations()) {
            configListView.getItems().add(config.getConfigName());
        }
    }

    /**
     * Populates the form fields with the selected configuration's details.
     * The name field is locked — name cannot be changed, other fields are editable.
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
     * Clears all form fields and prepares the form for a new configuration entry.
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
     * Save button handler.
     * If selectedConfigName is null → create new record
     * If selectedConfigName is set  → update existing record
     */
    @FXML
    void handleSave(ActionEvent event) {
        if (manager == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Manager is not connected.");
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
            showAlert(Alert.AlertType.WARNING, "Missing Field", "Run command cannot be empty.");
            return;
        }

        Path compiler = compilerStr.isEmpty() ? null : Path.of(compilerStr);
        Path output   = outputStr.isEmpty()   ? null : Path.of(outputStr);
        String args   = cArgs.isEmpty()       ? null : cArgs;

        if (selectedConfigName != null) {
            // ── Update mode ───────────────────────────
            String nameBeforeClear = selectedConfigName;
            Configuration updated = manager.updateConfiguration(
                    selectedConfigName, compiler, args, execCmd, output
            );
            if (updated != null) {
                refreshList();
                clearForm();
                showAlert(Alert.AlertType.INFORMATION, "Updated",
                        "Configuration updated: " + nameBeforeClear);
                System.out.println("[Config] Updated: " + nameBeforeClear);
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Update failed.");
            }
        } else {
            // ── New record mode ───────────────────────
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
                        "Could not save. A configuration with the same name may already exist.");
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
        confirm.setTitle("Confirm Deletion");
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