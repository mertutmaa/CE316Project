package org.example.ce316project;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * StudentSubmission — Bir öğrencinin ödev gönderisini temsil eder.
 *
 * Değerlendirme aşamaları:
 *   Gönderildi → ZIP Çıkartıldı → Derlendi → Çalıştı → Testi Geçti
 */
public class StudentSubmission {

    // ─────────────────────────────────────────────
    // Alanlar
    // ─────────────────────────────────────────────

    /** Öğrenci kimliği — ZIP dosya adından alınır (ör: "20210001"). Değiştirilemez. */
    private final String studentID;

    /** Kaynak kodun çıkartıldığı dizin yolu. */
    private String extractedSourceDirectory;

    /** Hangi konfigürasyonla değerlendirildi. */
    private String usedConfigName;

    /** Derleme aşaması başarılı mı? */
    private boolean isCompiled;

    /** Çalıştırma aşaması başarılı mı? */
    private boolean ranSuccessfully;

    /** Beklenen çıktıyla eşleşti mi? */
    private boolean passedTesting;

    /** Değerlendirme raporu — hata mesajları veya PASSED/FAILED detayı. */
    private String reportDetails;

    /** Gönderinin oluşturulma zamanı. */
    private final LocalDateTime submittedAt;

    /** Değerlendirmenin tamamlandığı zaman. */
    private LocalDateTime evaluatedAt;

    // ─────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────

    /**
     * @param id   Öğrenci kimliği (ör: "20210001")
     * @param path ZIP'in çıkartıldığı kaynak dizin yolu
     */
    public StudentSubmission(String id, String path) {
        this.studentID                = id;
        this.extractedSourceDirectory = path;

        this.isCompiled       = false;
        this.ranSuccessfully  = false;
        this.passedTesting    = false;
        this.reportDetails    = "Pending evaluation.";
        this.submittedAt      = LocalDateTime.now();
        this.evaluatedAt      = null;
        this.usedConfigName   = null;
    }

    // ─────────────────────────────────────────────
    // Setter'lar
    // ─────────────────────────────────────────────

    public void setExtractedSourceDirectory(String extractedSourceDirectory) {
        this.extractedSourceDirectory = extractedSourceDirectory;
    }

    public void setUsedConfigName(String configName) {
        this.usedConfigName = configName;
    }

    public void setCompiled(boolean compiled) {
        this.isCompiled = compiled;
    }

    public void setRanSuccessfully(boolean ranSuccessfully) {
        this.ranSuccessfully = ranSuccessfully;
    }

    public void setPassedTesting(boolean passedTesting) {
        this.passedTesting = passedTesting;
    }

    public void setReportDetails(String reportDetails) {
        this.reportDetails = reportDetails;
    }

    /**
     * Değerlendirme tamamlandığında çağrılır, zamanı kaydeder.
     */
    public void markEvaluated() {
        this.evaluatedAt = LocalDateTime.now();
    }

    // ─────────────────────────────────────────────
    // Getter'lar
    // ─────────────────────────────────────────────

    public String getStudentID() {
        return studentID;
    }

    public String getExtractedSourceDirectory() {
        return extractedSourceDirectory;
    }

    public String getUsedConfigName() {
        return usedConfigName;
    }

    /**
     * Java boolean getter standardı: is + FieldName (büyük harf).
     * JavaFX PropertyValueFactory ve diğer kütüphanelerle uyumlu.
     */
    public boolean isCompiled() {
        return isCompiled;
    }

    /**
     * JavaFX TableView PropertyValueFactory için gerekli:
     * "isCompiled" property'si → isCompiled() metodunu arar.
     * Geriye dönük uyumluluk için getIsCompiled() de korundu.
     */
    public boolean getIsCompiled() {
        return isCompiled;
    }

    public boolean isRanSuccessfully() {
        return ranSuccessfully;
    }

    public boolean getRanSuccessfully() {
        return ranSuccessfully;
    }

    public boolean isPassedTesting() {
        return passedTesting;
    }

    public boolean getPassedTesting() {
        return passedTesting;
    }

    public String getReportDetails() {
        return reportDetails;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public LocalDateTime getEvaluatedAt() {
        return evaluatedAt;
    }

    // ─────────────────────────────────────────────
    // Yardımcı Metotlar
    // ─────────────────────────────────────────────

    /**
     * Değerlendirme durumunu kısa bir string olarak döndürür.
     * Konsol özeti ve debug için kullanışlı.
     */
    public String getStatusSummary() {
        if (!isCompiled)      return "COMPILE_ERROR";
        if (!ranSuccessfully) return "RUNTIME_ERROR";
        if (!passedTesting)   return "WRONG_OUTPUT";
        return "PASSED";
    }

    /**
     * Zaman damgasını okunabilir formatta döndürür.
     */
    public String getFormattedSubmittedAt() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return submittedAt.format(formatter);
    }

    @Override
    public String toString() {
        return "StudentSubmission{"
                + "studentID='" + studentID + '\''
                + ", status=" + getStatusSummary()
                + ", submittedAt=" + getFormattedSubmittedAt()
                + '}';
    }
}