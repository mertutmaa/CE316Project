package org.example.ce316project;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;

public class MainController {

    // ─────────────────────────────────────────────
    // Dependencies
    // ─────────────────────────────────────────────

    private AssignmentManager manager;
    private Project currentProject;

    // ─────────────────────────────────────────────
    // FXML Components
    // ─────────────────────────────────────────────

    @FXML
    private ListView<String> studentListView;

    @FXML
    private ComboBox<String> configComboBox;

    @FXML
    private TextField directoryTextField;

    @FXML
    private TableView<StudentSubmission> resultsTableView;

    @FXML
    private TableColumn<StudentSubmission, String> studentIdCol;

    @FXML
    private TableColumn<StudentSubmission, Boolean> statusCol;

    @FXML
    private TableColumn<StudentSubmission, String> detailsCol;

    // ─────────────────────────────────────────────
    // Initialization
    // ─────────────────────────────────────────────

    @FXML
    public void initialize() {
        manager = new AssignmentManager();
        System.out.println("[Main] AssignmentManager initialized.");

        setupTableColumns();
        refreshConfigComboBox();
    }

    /**
     * Binds TableView columns to StudentSubmission fields.
     */
    private void setupTableColumns() {
        studentIdCol.setCellValueFactory(new PropertyValueFactory<>("studentID"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("passedTesting"));
        detailsCol.setCellValueFactory(new PropertyValueFactory<>("reportDetails"));

        // Render status column as readable text: true → PASSED, false → FAILED
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean passed, boolean empty) {
                super.updateItem(passed, empty);
                if (empty || passed == null) {
                    setText(null);
                    setStyle("");
                } else if (passed) {
                    setText("✓ PASSED");
                    setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                } else {
                    setText("✗ FAILED");
                    setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                }
            }
        });
    }

    /**
     * Refreshes the ComboBox with the current configuration list from AssignmentManager.
     */
    private void refreshConfigComboBox() {
        configComboBox.getItems().clear();
        for (Configuration config : manager.getConfigurations()) {
            configComboBox.getItems().add(config.getConfigName());
        }
    }

    // ─────────────────────────────────────────────
    // Menu — Project Operations
    // ─────────────────────────────────────────────

    /**
     * Creates a new project using the selected configuration.
     */
    @FXML
    void handleNewProject(ActionEvent event) {
        String selectedConfig = configComboBox.getValue();
        if (selectedConfig == null || selectedConfig.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "No Configuration Selected",
                    "Please select a configuration first.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Project");
        dialog.setHeaderText("Enter Project Name");
        dialog.setContentText("Name:");

        dialog.showAndWait().ifPresent(projectName -> {
            if (projectName.isBlank()) {
                showAlert(Alert.AlertType.WARNING, "Invalid Name", "Project name cannot be empty.");
                return;
            }

            Configuration config = manager.findConfigurationByName(selectedConfig);
            if (config == null) {
                showAlert(Alert.AlertType.ERROR, "Error", "Configuration not found: " + selectedConfig);
                return;
            }

            Project project = manager.createProject(projectName, config);
            if (project == null) {
                showAlert(Alert.AlertType.ERROR, "Error",
                        "Could not create project. A project with the same name may already exist.");
                return;
            }

            currentProject = project;
            clearUI();
            showAlert(Alert.AlertType.INFORMATION, "Success",
                    "Project created: " + projectName);
            System.out.println("[Main] New project created: " + projectName);
        });
    }

    /**
     * Loads an existing project by name.
     */
    @FXML
    void handleOpenProject(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Open Project");
        dialog.setHeaderText("Enter Project Name");
        dialog.setContentText("Name:");

        dialog.showAndWait().ifPresent(projectName -> {
            if (projectName.isBlank()) return;

            Project loaded = manager.loadProject(projectName);
            if (loaded == null) {
                showAlert(Alert.AlertType.ERROR, "Not Found",
                        "Project not found: " + projectName);
                return;
            }

            currentProject = loaded;
            loadProjectIntoUI(currentProject);
            System.out.println("[Main] Project loaded: " + projectName);
        });
    }

    /**
     * Saves the current project's ZIP directory to the database.
     */
    @FXML
    void handleSaveProject(ActionEvent event) {
        if (currentProject == null) {
            showAlert(Alert.AlertType.WARNING, "No Project", "There is no open project to save.");
            return;
        }

        String dirText = directoryTextField.getText();
        if (dirText != null && !dirText.isBlank()) {
            manager.updateProjectZipDirectory(currentProject, Path.of(dirText));
        }

        showAlert(Alert.AlertType.INFORMATION, "Saved",
                currentProject.getName() + " has been saved.");
        System.out.println("[Main] Project saved: " + currentProject.getName());
    }

    /**
     * Exits the application.
     */
    @FXML
    void handleExit(ActionEvent event) {
        onApplicationClose();
        System.exit(0);
    }

    // ─────────────────────────────────────────────
    // Menu — Configuration Operations
    // ─────────────────────────────────────────────

    /**
     * Opens the configuration management screen as a modal window.
     * Updates the ComboBox after the window is closed.
     */
    @FXML
    void handleManageConfigs(ActionEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(
                    getClass().getResource("config-manager.fxml")
            );
            javafx.scene.Parent root = fxmlLoader.load();

            // Inject manager into ConfigManagerController
            ConfigManagerController configController = fxmlLoader.getController();
            configController.setManager(manager);

            Stage stage = new Stage();
            stage.setTitle("Manage Configurations");
            stage.setScene(new javafx.scene.Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            // Refresh ComboBox after the window closes
            refreshConfigComboBox();
            System.out.println("[Main] Configuration list updated.");

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error",
                    "Could not open configuration screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
void handleImportConfig(ActionEvent event) {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Import Configuration");
    fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("JSON Files", "*.json")
    );

    Stage stage = (Stage) directoryTextField.getScene().getWindow();
    File selectedFile = fileChooser.showOpenDialog(stage);

    if (selectedFile != null) {
        Configuration imported = manager.importConfiguration(selectedFile.getAbsolutePath());
        if (imported != null) {
            refreshConfigComboBox();
            showAlert(Alert.AlertType.INFORMATION, "Imported",
                    "Configuration imported: " + imported.getConfigName());
        } else {
            showAlert(Alert.AlertType.ERROR, "Import Failed",
                    "Could not import configuration. File may be invalid or a configuration with the same name already exists.");
        }
    }
}

@FXML
void handleExportConfig(ActionEvent event) {
    String selectedConfig = configComboBox.getValue();
    if (selectedConfig == null || selectedConfig.isBlank()) {
        showAlert(Alert.AlertType.WARNING, "No Configuration Selected",
                "Please select a configuration from the dropdown to export.");
        return;
    }

    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Export Configuration");
    fileChooser.setInitialFileName(selectedConfig + ".json");
    fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("JSON Files", "*.json")
    );

    Stage stage = (Stage) directoryTextField.getScene().getWindow();
    File saveFile = fileChooser.showSaveDialog(stage);

    if (saveFile != null) {
        boolean success = manager.exportConfiguration(selectedConfig, saveFile.getAbsolutePath());
        if (success) {
            showAlert(Alert.AlertType.INFORMATION, "Exported",
                    "Configuration exported to: " + saveFile.getAbsolutePath());
        } else {
            showAlert(Alert.AlertType.ERROR, "Export Failed",
                    "Could not export configuration.");
        }
    }
}

    // ─────────────────────────────────────────────
    // Menu — Help
    // ─────────────────────────────────────────────

    @FXML
    void handleOpenManual(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("User Manual");
        alert.setHeaderText("IAE — Integrated Assignment Evaluator");
        alert.setContentText(
            "─── QUICK TEST (using test_submissions folder) ───\n" +
            "The test_submissions folder included with the project\n" +
            "contains a ready-to-use test environment.\n\n" +
            "1. Configuration → Manage Configurations\n" +
            "   • Select 'Test Config' from the list\n" +
            "   • Update the Expected Output field:\n" +
            "     <full path to test_submissions>\\expected.txt\n" +
            "     Example: C:\\Users\\user\\Desktop\\test_submissions\\expected.txt\n" +
            "   • Click Save Configuration\n\n" +
            "2. File → Open Project\n" +
            "   • Enter: Test Project\n\n" +
            "3. Click Browse... on the main screen\n" +
            "   • Select the test_submissions folder\n\n" +
            "4. Click Run Assignments\n" +
            "   • Student 20210001 should show ✓ PASSED\n\n" +

            "────────────────────────────────────────────\n\n" +

            "─── GENERAL USAGE ───\n\n" +
            "STEP 1 — Create a Configuration:\n" +
            "  Configuration → Manage Configurations\n" +
            "  → + New Configuration\n" +
            "  → Fill in compiler path, arguments, run command\n" +
            "  → Set the expected output file path\n" +
            "  → Save Configuration\n\n" +
            "STEP 2 — Create a Project:\n" +
            "  File → New Project\n" +
            "  → Enter a project name\n" +
            "  → Select a configuration from Active Configuration\n\n" +
            "STEP 3 — Select ZIP Directory:\n" +
            "  → Click Browse... and select the folder\n" +
            "     containing student ZIP files\n" +
            "  → ZIP files must be named with student ID\n" +
            "     Example: 20210001.zip\n\n" +
            "STEP 4 — Run Evaluation:\n" +
            "  → Click Run Assignments\n" +
            "  → Results appear in the Execution Reports table\n" +
            "  ✓ PASSED : Output matched the expected output\n" +
            "  ✗ FAILED : Output did not match or an error occurred\n\n" +

            "─── EXAMPLE CONFIGURATIONS ───\n" +
            "Python : Run Command → python hello.py\n" +
            "C      : Compiler → gcc | Args → -o main main.c\n" +
            "         Run Command → ./main\n" +
            "Java   : Compiler → javac | Args → Main.java\n" +
            "         Run Command → java Main"
        );

        alert.setResizable(true);
        alert.getDialogPane().setMinWidth(520);
        alert.getDialogPane().setMinHeight(580);
        alert.showAndWait();
    }

    // ─────────────────────────────────────────────
    // Main Screen Operations
    // ─────────────────────────────────────────────

    /**
     * Opens a DirectoryChooser to select the ZIP directory.
     * Assigns the selected directory to the current project.
     */
    @FXML
    void handleBrowseDirectory(ActionEvent event) {
        if (currentProject == null) {
            showAlert(Alert.AlertType.WARNING, "No Project",
                    "Please create or open a project first.");
            return;
        }

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Student ZIP Directory");

        // Start from the current directory if already set
        String current = directoryTextField.getText();
        if (current != null && !current.isBlank()) {
            File currentDir = new File(current);
            if (currentDir.isDirectory()) {
                directoryChooser.setInitialDirectory(currentDir);
            }
        }

        Stage stage = (Stage) directoryTextField.getScene().getWindow();
        File selectedDir = directoryChooser.showDialog(stage);

        if (selectedDir != null) {
            directoryTextField.setText(selectedDir.getAbsolutePath());
            currentProject.setSubmissionZIPsDirectory(Path.of(selectedDir.getAbsolutePath()));
            System.out.println("[Main] ZIP directory selected: " + selectedDir.getAbsolutePath());
        }
    }

    /**
     * Evaluates all student submissions.
     * Processes ZIPs, runs the evaluation loop, and loads results into the UI.
     */
    @FXML
    void handleRunAssignments(ActionEvent event) {
        if (currentProject == null) {
            showAlert(Alert.AlertType.WARNING, "No Project",
                    "Please create or open a project first.");
            return;
        }

        String dirText = directoryTextField.getText();
        if (dirText == null || dirText.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "No Directory Selected",
                    "Please select the folder containing student ZIP files.");
            return;
        }

        String selectedConfig = configComboBox.getValue();
        if (selectedConfig == null || selectedConfig.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "No Configuration Selected",
                    "Please select a configuration.");
            return;
        }

        Configuration config = manager.findConfigurationByName(selectedConfig);
        if (config == null) {
            showAlert(Alert.AlertType.ERROR, "Error",
                    "Configuration not found: " + selectedConfig);
            return;
        }

        currentProject.setConfiguration(config);
        currentProject.setSubmissionZIPsDirectory(Path.of(dirText));

        System.out.println("[Main] Processing ZIPs...");
        currentProject.processZIPs();

        if (currentProject.getSubmissions().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Submissions",
                    "No ZIP files found in the selected directory.");
            return;
        }

        System.out.println("[Main] Starting evaluation...");
        currentProject.runEvaluationLoop();

        loadResultsIntoUI();

        showAlert(Alert.AlertType.INFORMATION, "Done",
                currentProject.getSubmissions().size() + " submission(s) evaluated.");
    }

    // ─────────────────────────────────────────────
    // UI Update Helpers
    // ─────────────────────────────────────────────

    /**
     * Loads the given project's data into the UI components.
     */
    private void loadProjectIntoUI(Project project) {
        if (project.getSubmissionZIPsDirectory() != null) {
            directoryTextField.setText(project.getSubmissionZIPsDirectory().toString());
        } else {
            directoryTextField.clear();
        }

        if (project.getConfiguration() != null) {
            configComboBox.setValue(project.getConfiguration().getConfigName());
        }

        loadResultsIntoUI();
    }

    /**
     * Loads the current project's submission list into the TableView and studentListView.
     */
    private void loadResultsIntoUI() {
        resultsTableView.getItems().clear();
        studentListView.getItems().clear();

        if (currentProject == null) return;

        for (StudentSubmission submission : currentProject.getSubmissions()) {
            resultsTableView.getItems().add(submission);
            studentListView.getItems().add(submission.getStudentID());
        }
    }

    /**
     * Clears all UI components (called when a new project is created).
     */
    private void clearUI() {
        resultsTableView.getItems().clear();
        studentListView.getItems().clear();
        directoryTextField.clear();
    }

    // ─────────────────────────────────────────────
    // Application Close
    // ─────────────────────────────────────────────

    /**
     * Called by IAEApplication.setOnCloseRequest().
     * Closes the SQLite connection gracefully.
     */
    public void onApplicationClose() {
        if (manager != null) {
            manager.closeDatabase();
            System.out.println("[Main] Application closed. Database connection terminated.");
        }
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