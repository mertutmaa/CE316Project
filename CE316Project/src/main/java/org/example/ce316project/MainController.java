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
    // Bağımlılıklar
    // ─────────────────────────────────────────────

    private AssignmentManager manager;
    private Project currentProject;

    // ─────────────────────────────────────────────
    // FXML Bileşenleri
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
    // Başlatma
    // ─────────────────────────────────────────────

    @FXML
    public void initialize() {
        manager = new AssignmentManager();
        System.out.println("[Main] AssignmentManager başlatıldı.");

        setupTableColumns();
        refreshConfigComboBox();
    }

    /**
     * TableView sütunlarını StudentSubmission alanlarına bağlar.
     */
    private void setupTableColumns() {
        studentIdCol.setCellValueFactory(new PropertyValueFactory<>("studentID"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("passedTesting"));
        detailsCol.setCellValueFactory(new PropertyValueFactory<>("reportDetails"));

        // Durum sütununu okunabilir hale getir: true → PASSED, false → FAILED
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
     * ComboBox'ı AssignmentManager'daki güncel konfigürasyon listesiyle doldurur.
     */
    private void refreshConfigComboBox() {
        configComboBox.getItems().clear();
        for (Configuration config : manager.getConfigurations()) {
            configComboBox.getItems().add(config.getConfigName());
        }
    }

    // ─────────────────────────────────────────────
    // Menü — Proje İşlemleri
    // ─────────────────────────────────────────────

    /**
     * Yeni proje oluşturur.
     * Seçili konfigürasyon ve dizin bilgisiyle Project nesnesi üretir.
     */
    @FXML
    void handleNewProject(ActionEvent event) {
        String selectedConfig = configComboBox.getValue();
        if (selectedConfig == null || selectedConfig.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Konfigürasyon Seçilmedi",
                    "Lütfen önce bir konfigürasyon seçin.");
            return;
        }

        // Proje adı için dialog
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Yeni Proje");
        dialog.setHeaderText("Proje Adı Girin");
        dialog.setContentText("Ad:");

        dialog.showAndWait().ifPresent(projectName -> {
            if (projectName.isBlank()) {
                showAlert(Alert.AlertType.WARNING, "Geçersiz Ad", "Proje adı boş olamaz.");
                return;
            }

            Configuration config = manager.findConfigurationByName(selectedConfig);
            if (config == null) {
                showAlert(Alert.AlertType.ERROR, "Hata", "Konfigürasyon bulunamadı: " + selectedConfig);
                return;
            }

            Project project = manager.createProject(projectName, config);
            if (project == null) {
                showAlert(Alert.AlertType.ERROR, "Hata",
                        "Proje oluşturulamadı. Aynı isimde bir proje mevcut olabilir.");
                return;
            }

            currentProject = project;
            clearUI();
            showAlert(Alert.AlertType.INFORMATION, "Başarılı",
                    "Proje oluşturuldu: " + projectName);
            System.out.println("[Main] Yeni proje oluşturuldu: " + projectName);
        });
    }

    /**
     * Mevcut bir projeyi ada göre yükler.
     */
    @FXML
    void handleOpenProject(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Proje Aç");
        dialog.setHeaderText("Proje Adını Girin");
        dialog.setContentText("Ad:");

        dialog.showAndWait().ifPresent(projectName -> {
            if (projectName.isBlank()) return;

            Project loaded = manager.loadProject(projectName);
            if (loaded == null) {
                showAlert(Alert.AlertType.ERROR, "Bulunamadı",
                        "Proje bulunamadı: " + projectName);
                return;
            }

            currentProject = loaded;
            loadProjectIntoUI(currentProject);
            System.out.println("[Main] Proje yüklendi: " + projectName);
        });
    }

    /**
     * Mevcut projenin ZIP dizinini veritabanında günceller (kaydetme işlevi).
     */
    @FXML
    void handleSaveProject(ActionEvent event) {
        if (currentProject == null) {
            showAlert(Alert.AlertType.WARNING, "Proje Yok", "Kaydedilecek açık bir proje yok.");
            return;
        }

        String dirText = directoryTextField.getText();
        if (dirText != null && !dirText.isBlank()) {
            manager.updateProjectZipDirectory(currentProject, Path.of(dirText));
        }

        showAlert(Alert.AlertType.INFORMATION, "Kaydedildi",
                currentProject.getName() + " projesi kaydedildi.");
        System.out.println("[Main] Proje kaydedildi: " + currentProject.getName());
    }

    /**
     * Uygulamayı kapatır.
     */
    @FXML
    void handleExit(ActionEvent event) {
        onApplicationClose();
        System.exit(0);
    }

    // ─────────────────────────────────────────────
    // Menü — Konfigürasyon İşlemleri
    // ─────────────────────────────────────────────

    /**
     * Konfigürasyon yönetim ekranını modal olarak açar.
     * Kapandıktan sonra ComboBox'ı günceller.
     */
    @FXML
    void handleManageConfigs(ActionEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(
                    getClass().getResource("config-manager.fxml")
            );
            javafx.scene.Parent root = fxmlLoader.load();

            // Manager'ı ConfigManagerController'a inject et
            ConfigManagerController configController = fxmlLoader.getController();
            configController.setManager(manager);

            Stage stage = new Stage();
            stage.setTitle("Konfigürasyonları Yönet");
            stage.setScene(new javafx.scene.Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            // Pencere kapandıktan sonra ComboBox'ı güncelle
            refreshConfigComboBox();
            System.out.println("[Main] Konfigürasyon listesi güncellendi.");

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Hata",
                    "Konfigürasyon ekranı açılamadı: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Konfigürasyonu dosyadan içe aktarır.
     * TODO: AssignmentManager.importConfiguration() implement edilince burası da çalışacak.
     */
    @FXML
    void handleImportConfig(ActionEvent event) {
        showAlert(Alert.AlertType.INFORMATION, "Yakında",
                "İçe aktarma özelliği henüz implement edilmedi.");
        System.out.println("[Main] Import config — henüz implement edilmedi.");
    }

    /**
     * Konfigürasyonu dosyaya dışa aktarır.
     * TODO: Dışa aktarma implement edilecek.
     */
    @FXML
    void handleExportConfig(ActionEvent event) {
        showAlert(Alert.AlertType.INFORMATION, "Yakında",
                "Dışa aktarma özelliği henüz implement edilmedi.");
        System.out.println("[Main] Export config — henüz implement edilmedi.");
    }

    // ─────────────────────────────────────────────
    // Menü — Yardım
    // ─────────────────────────────────────────────

    @FXML
    void handleOpenManual(ActionEvent event) {
        showAlert(Alert.AlertType.INFORMATION, "Kullanım Kılavuzu",
                "IAE — Integrated Assignment Evaluator\n\n"
                + "1. Konfigürasyon oluşturun (Konfigürasyonlar → Yönet)\n"
                + "2. Yeni proje oluşturun (Dosya → Yeni Proje)\n"
                + "3. ZIP dizinini seçin\n"
                + "4. Değerlendirmeyi başlatın");
    }

    // ─────────────────────────────────────────────
    // Ana Ekran İşlemleri
    // ─────────────────────────────────────────────

    /**
     * ZIP dizini seçmek için DirectoryChooser açar.
     * Seçilen dizin mevcut projeye atanır.
     */
    @FXML
    void handleBrowseDirectory(ActionEvent event) {
        if (currentProject == null) {
            showAlert(Alert.AlertType.WARNING, "Proje Yok",
                    "Lütfen önce bir proje oluşturun veya açın.");
            return;
        }

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Öğrenci ZIP Dizinini Seç");

        // Mevcut dizin varsa oradan başla
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
            System.out.println("[Main] ZIP dizini seçildi: " + selectedDir.getAbsolutePath());
        }
    }

    /**
     * Tüm öğrenci gönderilerini değerlendirir.
     * Önce ZIP'leri işler, sonra evaluation loop'u çalıştırır,
     * sonuçları TableView ve studentListView'e yükler.
     */
    @FXML
    void handleRunAssignments(ActionEvent event) {
        if (currentProject == null) {
            showAlert(Alert.AlertType.WARNING, "Proje Yok",
                    "Lütfen önce bir proje oluşturun veya açın.");
            return;
        }

        String dirText = directoryTextField.getText();
        if (dirText == null || dirText.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Dizin Seçilmedi",
                    "Lütfen öğrenci ZIP dosyalarının bulunduğu dizini seçin.");
            return;
        }

        String selectedConfig = configComboBox.getValue();
        if (selectedConfig == null || selectedConfig.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Konfigürasyon Seçilmedi",
                    "Lütfen bir konfigürasyon seçin.");
            return;
        }

        // Konfigürasyonu güncelle
        Configuration config = manager.findConfigurationByName(selectedConfig);
        if (config == null) {
            showAlert(Alert.AlertType.ERROR, "Hata",
                    "Konfigürasyon bulunamadı: " + selectedConfig);
            return;
        }
        currentProject.setConfiguration(config);
        currentProject.setSubmissionZIPsDirectory(Path.of(dirText));

        // ZIP'leri işle
        System.out.println("[Main] ZIP'ler işleniyor...");
        currentProject.processZIPs();

        if (currentProject.getSubmissions().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Gönderi Yok",
                    "Seçilen dizinde ZIP dosyası bulunamadı.");
            return;
        }

        // Değerlendirmeyi çalıştır
        System.out.println("[Main] Değerlendirme başlatılıyor...");
        currentProject.runEvaluationLoop();

        // Sonuçları UI'a yükle
        loadResultsIntoUI();

        showAlert(Alert.AlertType.INFORMATION, "Tamamlandı",
                currentProject.getSubmissions().size()
                        + " gönderi değerlendirildi.");
    }

    // ─────────────────────────────────────────────
    // UI Güncelleme Yardımcıları
    // ─────────────────────────────────────────────

    /**
     * Yüklenen projenin bilgilerini UI bileşenlerine yazar.
     */
    private void loadProjectIntoUI(Project project) {
        // Dizini yaz
        if (project.getSubmissionZIPsDirectory() != null) {
            directoryTextField.setText(project.getSubmissionZIPsDirectory().toString());
        } else {
            directoryTextField.clear();
        }

        // Konfigürasyonu ComboBox'ta seç
        if (project.getConfiguration() != null) {
            configComboBox.setValue(project.getConfiguration().getConfigName());
        }

        // Sonuçları TableView'e yükle
        loadResultsIntoUI();
    }

    /**
     * Mevcut projenin submission listesini TableView ve studentListView'e yükler.
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
     * UI bileşenlerini temizler (yeni proje açılınca).
     */
    private void clearUI() {
        resultsTableView.getItems().clear();
        studentListView.getItems().clear();
        directoryTextField.clear();
    }

    // ─────────────────────────────────────────────
    // Uygulama Kapatma
    // ─────────────────────────────────────────────

    /**
     * IAEApplication.setOnCloseRequest() tarafından çağrılır.
     * SQLite bağlantısını düzgün kapatır.
     */
    public void onApplicationClose() {
        if (manager != null) {
            manager.closeDatabase();
            System.out.println("[Main] Uygulama kapatıldı, veritabanı bağlantısı sonlandırıldı.");
        }
    }

    // ─────────────────────────────────────────────
    // Yardımcı Metotlar
    // ─────────────────────────────────────────────

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}