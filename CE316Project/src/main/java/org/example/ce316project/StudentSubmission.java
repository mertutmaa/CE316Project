package org.example.ce316project;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * StudentSubmission — Represents a student's assignment submission.
 *
 * Evaluation stages:
 *   Submitted → ZIP Extracted → Compiled → Executed → Passed Testing
 */
public class StudentSubmission {

    // ─────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────

    /** Student identifier — parsed from the ZIP filename (e.g., "20210001"). Immutable. */
    private final String studentID;

    /** Directory path where the source code has been extracted. */
    private String extractedSourceDirectory;

    /** Name of the configuration profile used for evaluation. */
    private String usedConfigName;

    /** Indicates whether the compilation stage was successful. */
    private boolean isCompiled;

    /** Indicates whether the execution stage was successful. */
    private boolean ranSuccessfully;

    /** Indicates whether the program output matched the expected output. */
    private boolean passedTesting;

    /** Evaluation report — contains error messages or detailed PASSED/FAILED data. */
    private String reportDetails;

    /** Timestamp recording when the submission record was instantiated. */
    private final LocalDateTime submittedAt;

    /** Timestamp recording when the evaluation pipeline was completed. */
    private LocalDateTime evaluatedAt;

    // ─────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────

    /**
     * @param id   Student identifier (e.g., "20210001")
     * @param path Source directory path where the ZIP file was extracted
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
    // Setters
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
     * Invoked when the evaluation process completes to log the finish timestamp.
     */
    public void markEvaluated() {
        this.evaluatedAt = LocalDateTime.now();
    }

    // ─────────────────────────────────────────────
    // Getters
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
     * Standard Java boolean getter: is + FieldName (CamelCase).
     * Fully compliant with JavaFX PropertyValueFactory and other reflection-based libraries.
     */
    public boolean isCompiled() {
        return isCompiled;
    }

    /**
     * Required for JavaFX TableView PropertyValueFactory mapping:
     * The property "isCompiled" looks for the isCompiled() method pattern first.
     * getIsCompiled() is maintained here to guarantee backward compatibility.
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
    // Helper Methods
    // ─────────────────────────────────────────────

    /**
     * Returns a short string summary of the evaluation status.
     * Convenient for log console dumps and testing debug points.
     */
    public String getStatusSummary() {
        if (!isCompiled)      return "COMPILE_ERROR";
        if (!ranSuccessfully) return "RUNTIME_ERROR";
        if (!passedTesting)   return "WRONG_OUTPUT";
        return "PASSED";
    }

    /**
     * Converts the internal submission timestamp into a human-readable format.
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