package org.example.ce316project;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * StudentSubmission — Represents a single student's assignment submission.
 *
 * Evaluation stages:
 *   Submitted → ZIP Extracted → Compiled → Executed → Passed Testing
 */
public class StudentSubmission {

    // ─────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────

    /** Student ID — taken from the ZIP file name (e.g. "20210001"). Immutable. */
    private final String studentID;

    /** Path to the directory where the source code was extracted. */
    private String extractedSourceDirectory;

    /** Name of the configuration used for evaluation. */
    private String usedConfigName;

    /** Whether the compilation step succeeded. */
    private boolean isCompiled;

    /** Whether the execution step succeeded. */
    private boolean ranSuccessfully;

    /** Whether the output matched the expected output. */
    private boolean passedTesting;

    /** Evaluation report — error messages or PASSED/FAILED details. */
    private String reportDetails;

    /** Timestamp when the submission object was created. */
    private final LocalDateTime submittedAt;

    /** Timestamp when evaluation was completed. */
    private LocalDateTime evaluatedAt;

    // ─────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────

    /**
     * @param id   Student ID (e.g. "20210001")
     * @param path Path to the directory where the ZIP was extracted
     */
    public StudentSubmission(String id, String path) {
        this.studentID                = id;
        this.extractedSourceDirectory = path;

        this.isCompiled      = false;
        this.ranSuccessfully = false;
        this.passedTesting   = false;
        this.reportDetails   = "Pending evaluation.";
        this.submittedAt     = LocalDateTime.now();
        this.evaluatedAt     = null;
        this.usedConfigName  = null;
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
     * Call this when evaluation is complete to record the evaluation timestamp.
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
     * Standard Java boolean getter: is + FieldName (capitalized).
     * Compatible with JavaFX PropertyValueFactory and other libraries.
     */
    public boolean isCompiled() {
        return isCompiled;
    }

    /**
     * Kept for backward compatibility.
     * JavaFX TableView PropertyValueFactory looks for isCompiled() first.
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
     * Returns the evaluation status as a short string.
     * Useful for console summaries and debugging.
     */
    public String getStatusSummary() {
        if (!isCompiled)      return "COMPILE_ERROR";
        if (!ranSuccessfully) return "RUNTIME_ERROR";
        if (!passedTesting)   return "WRONG_OUTPUT";
        return "PASSED";
    }

    /**
     * Returns the submission timestamp in a human-readable format.
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