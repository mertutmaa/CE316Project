package org.example.ce316project;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;

/**
 * ConfigManagerController — Konfigürasyon yönetim ekranının controller'ı.
 *
 * Görevleri:
 *  - Mevcut konfigürasyonları listeler
 *  - Yeni konfigürasyon oluşturur (AssignmentManager üzerinden kaydeder)
 *  - Seçili konfigürasyonu düzenler ve günceller
 *  - Seçili konfigürasyonu siler
 *  - Derleyici dosyasını FileChooser ile seçer
 */
public class ConfigManagerController {

    // ─────────────────────────────────────────────
    // FXML Bileşenleri
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
    // Bağımlılıklar
    // ─────────────────────────────────────────────

    /** MainController tarafından inject edilir. */
    private AssignmentManager manager;

    /** Listeden seçili konfigürasyonun adı. null ise yeni kayıt modundayız. */
    private String selectedConfigName = null;

    // ─────────────────────────────────────────────
    // Başlatma
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
    // Liste & Form Yönetimi
    // ─────────────────────────────────────────────

    private void refreshList() {
        if (manager == null) return;
        configListView.getItems().clear();
        for (Configuration config : manager.getConfigurations()) {
            configListView.getItems().add(config.getConfigName());
        }
    }

    /**
     * Seçili konfigürasyonun bilgilerini form alanlarına doldurur.
     * Ad alanı kilitleniyor — ad değiştirilemez, diğer alanlar düzenlenebilir.
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
     * Tüm form alanlarını temizler, yeni konfigürasyon girişine hazırlar.
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
    // Button Handler'ları
    // ─────────────────────────────────────────────

    @FXML
    void handleCreateNew(ActionEvent event) {
        clearForm();
    }

    @FXML
    void handleBrowseCompiler(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Derleyici Seç");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Çalıştırılabilir Dosyalar", "*", "*.exe"),
                new FileChooser.ExtensionFilter("Tüm Dosyalar", "*.*")
        );

        Stage stage = (Stage) compilerPathField.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            compilerPathField.setText(selectedFile.getAbsolutePath());
            System.out.println("[Config] Derleyici seçildi: " + selectedFile.getAbsolutePath());
        }
    }

    /**
     * "Kaydet" butonu.
     * selectedConfigName null ise → yeni kayıt
     * selectedConfigName dolu ise  → güncelleme
     */
    @FXML
    void handleSave(ActionEvent event) {
        if (manager == null) {
            showAlert(Alert.AlertType.ERROR, "Hata", "Manager bağlı değil.");
            return;
        }

        String name        = nameField.getText().trim();
        String compilerStr = compilerPathField.getText().trim();
        String cArgs       = compilerArgsField.getText().trim();
        String execCmd     = runCmdField.getText().trim();
        String outputStr   = expectedOutputField.getText().trim();

        if (name.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Eksik Alan", "Konfigürasyon adı boş olamaz.");
            return;
        }
        if (execCmd.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Eksik Alan", "Çalıştırma komutu boş olamaz.");
            return;
        }

        Path compiler = compilerStr.isEmpty() ? null : Path.of(compilerStr);
        Path output   = outputStr.isEmpty()   ? null : Path.of(outputStr);
        String args   = cArgs.isEmpty()       ? null : cArgs;

        if (selectedConfigName != null) {
            // ── Güncelleme modu ──────────────────────
            Configuration updated = manager.updateConfiguration(
                    selectedConfigName, compiler, args, execCmd, output
            );
            if (updated != null) {
                refreshList();
                clearForm();
                showAlert(Alert.AlertType.INFORMATION, "Güncellendi",
                        "Konfigürasyon güncellendi: " + selectedConfigName);
                System.out.println("[Config] Güncellendi: " + selectedConfigName);
            } else {
                showAlert(Alert.AlertType.ERROR, "Hata", "Güncelleme başarısız.");
            }
        } else {
            // ── Yeni kayıt modu ──────────────────────
            Configuration saved = manager.createConfiguration(
                    name, compiler, args, execCmd, output
            );
            if (saved != null) {
                refreshList();
                clearForm();
                showAlert(Alert.AlertType.INFORMATION, "Kaydedildi",
                        "Konfigürasyon kaydedildi: " + name);
                System.out.println("[Config] Kaydedildi: " + name);
            } else {
                showAlert(Alert.AlertType.ERROR, "Hata",
                        "Kaydedilemedi. Aynı isimde bir kayıt mevcut olabilir.");
            }
        }
    }

    @FXML
    void handleDelete(ActionEvent event) {
        String selected = configListView.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Seçim Yok",
                    "Lütfen silmek için bir konfigürasyon seçin.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Silme Onayı");
        confirm.setHeaderText("Bu konfigürasyonu silmek istediğinizden emin misiniz?");
        confirm.setContentText(selected);

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                manager.deleteConfiguration(selected);
                refreshList();
                clearForm();
                System.out.println("[Config] Silindi: " + selected);
                showAlert(Alert.AlertType.INFORMATION, "Silindi",
                        "Konfigürasyon silindi: " + selected);
            }
        });
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