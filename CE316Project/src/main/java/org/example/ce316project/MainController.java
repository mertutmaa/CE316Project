package org.example.ce316project;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

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

        // Make the status column human-readable: true → PASSED, false → FAILED
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
     * Populates the ComboBox with the current configuration list from AssignmentManager.
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
     * Creates a new project.
     * Generates a Project object using the selected configuration and directory details.
     */
    @FXML
    void handleNewProject(ActionEvent event) {
        String selectedConfig = configComboBox.getValue();
        if (selectedConfig == null || selectedConfig.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "No Configuration Selected",
                    "Please select a configuration first.");
            return;
        }

        // Dialog for project name
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
                        "Could not create project. A project with the same name might already exist.");
                return;
            }

            currentProject = project;
            clearUI();
            showAlert(Alert.AlertType.INFORMATION, "Success",
                    "Project created successfully: " + projectName);
            System.out.println("[Main] New project created: " + projectName);
        });
    }

    /**
     * Loads an existing project by its name.
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
     * Updates the ZIP directory of the current project in the database (save functionality).
     */
    @FXML
    void handleSaveProject(ActionEvent event) {
        if (currentProject == null) {
            showAlert(Alert.AlertType.WARNING, "No Open Project", "There is no active project to save.");
            return;
        }

        String dirText = directoryTextField.getText();
        if (dirText != null && !dirText.isBlank()) {
            manager.updateProjectZipDirectory(currentProject, Path.of(dirText));
        }

        showAlert(Alert.AlertType.INFORMATION, "Saved",
                "Project '" + currentProject.getName() + "' has been saved.");
        System.out.println("[Main] Project saved: " + currentProject.getName());
    }

    /**
     * Shuts down the application.
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
     * Opens the configuration management window as an application modal modal.
     * Refreshes the ComboBox selection values after closing.
     */
    @FXML
    void handleManageConfigs(ActionEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(
                    getClass().getResource("config-manager.fxml")
            );
            javafx.scene.Parent root = fxmlLoader.load();

            // Inject the manager dependency into ConfigManagerController
            ConfigManagerController configController = fxmlLoader.getController();
            configController.setManager(manager);

            Stage stage = new Stage();
            stage.setTitle("Manage Configurations");
            stage.setScene(new javafx.scene.Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            // Update ComboBox selections after the configuration screen closes
            refreshConfigComboBox();
            System.out.println("[Main] Configuration checklist updated.");

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error",
                    "Could not open configuration management window: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Imports a configuration setup profile from an external data file.
     * TODO: This will be operational once AssignmentManager.importConfiguration() is implemented.
     */
    @FXML
    void handleImportConfig(ActionEvent event) {
        showAlert(Alert.AlertType.INFORMATION, "Coming Soon",
                "The configuration import feature has not been implemented yet.");
        System.out.println("[Main] Import config — feature pending implementation.");
    }

    /**
     * Exports a configuration setup profile into an external data file.
     * TODO: Export architecture to be added.
     */
    @FXML
    void handleExportConfig(ActionEvent event) {
        showAlert(Alert.AlertType.INFORMATION, "Coming Soon",
                "The configuration export feature has not been implemented yet.");
        System.out.println("[Main] Export config — feature pending implementation.");
    }

    // ─────────────────────────────────────────────
    // Menu — Help
    // ─────────────────────────────────────────────

    @FXML
    void handleOpenManual(ActionEvent event) {
        showAlert(Alert.AlertType.INFORMATION, "User Manual",
                "IAE — Integrated Assignment Evaluator\n\n"
                + "1. Create a running configuration profile (Configurations → Manage)\n"
                + "2. Instantiate a new project space (File → New Project)\n"
                + "3. Select your target submission ZIP archive directory\n"
                + "4. Click run to start validation evaluators loops");
    }

    // ─────────────────────────────────────────────
    // Main Panel Operations
    // ─────────────────────────────────────────────

    /**
     * Opens a DirectoryChooser pane to isolate specific assignment ZIP directories.
     * Binds selected path parameters right into the current active project model.
     */
    @FXML
    void handleBrowseDirectory(ActionEvent event) {
        if (currentProject == null) {
            showAlert(Alert.AlertType.WARNING, "No Project Active",
                    "Please create or open a project workspace first.");
            return;
        }

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Student Submission ZIP Directory");

        // Start from current folder path if populated
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
     * Unpacks and evaluates all gathered student submission archives inside target folder sets.
     * Processes individual ZIPs, launches testing pipelines, and renders lists data arrays.
     */
    @FXML
    void handleRunAssignments(ActionEvent event) {
        if (currentProject == null) {
            showAlert(Alert.AlertType.WARNING, "No Project Active",
                    "Please create or open a project workspace first.");
            return;
        }

        String dirText = directoryTextField.getText();
        if (dirText == null || dirText.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Directory Not Selected",
                    "Please select the directory containing the student submission ZIP files.");
            return;
        }

        String selectedConfig = configComboBox.getValue();
        if (selectedConfig == null || selectedConfig.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Configuration Not Selected",
                    "Please choose a target runtime configuration profile.");
            return;
        }

        // Apply chosen configuration profile parameters
        Configuration config = manager.findConfigurationByName(selectedConfig);
        if (config == null) {
            showAlert(Alert.AlertType.ERROR, "Error",
                    "Configuration profile not found: " + selectedConfig);
            return;
        }
        currentProject.setConfiguration(config);
        currentProject.setSubmissionZIPsDirectory(Path.of(dirText));

        // Process data packages blocks
        System.out.println("[Main] Processing submission archives...");
        currentProject.processZIPs();

        if (currentProject.getSubmissions().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Submissions Found",
                    "No valid ZIP data archives detected within the target file directory.");
            return;
        }

        // Run assignment evaluators pipelines loops
        System.out.println("[Main] Starting evaluation loops...");
        currentProject.runEvaluationLoop();

        // Populate grids display panels
        loadResultsIntoUI();

        showAlert(Alert.AlertType.INFORMATION, "Evaluation Complete",
                currentProject.getSubmissions().size()
                        + " student assignments have been processed.");
    }

    // ─────────────────────────────────────────────
    // UI Refresh Utilities
    // ─────────────────────────────────────────────

    /**
     * Binds loaded project workspace variables onto corresponding GUI component nodes.
     */
    private void loadProjectIntoUI(Project project) {
        // Output path strings to text fields
        if (project.getSubmissionZIPsDirectory() != null) {
            directoryTextField.setText(project.getSubmissionZIPsDirectory().toString());
        } else {
            directoryTextField.clear();
        }

        // Select matched profile key on drop box elements
        if (project.getConfiguration() != null) {
            configComboBox.setValue(project.getConfiguration().getConfigName());
        }

        // Re-render display layout tables
        loadResultsIntoUI();
    }

    /**
     * Feeds metadata from project submission records directly onto list arrays and tables grids.
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
     * Flushes panel grid visuals down to default empty base clean layout values.
     */
    private void clearUI() {
        resultsTableView.getItems().clear();
        studentListView.getItems().clear();
        directoryTextField.clear();
    }

    // ─────────────────────────────────────────────
    // Application Tear Down
    // ─────────────────────────────────────────────

    /**
     * Executed automatically by active hooks inside IAEApplication.setOnCloseRequest().
     * Closes working engine SQLite thread references cleanly.
     */
    public void onApplicationClose() {
        if (manager != null) {
            manager.closeDatabase();
            System.out.println("[Main] Application closed cleanly; database infrastructure severed.");
        }
    }

    // ─────────────────────────────────────────────
    // Interface Helpers
    // ─────────────────────────────────────────────

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}