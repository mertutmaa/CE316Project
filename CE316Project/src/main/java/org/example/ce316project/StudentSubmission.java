package org.example.ce316project;

public class StudentSubmission {
    private String studentID;
    private String extractedSourceDirectory;
    private boolean isCompiled;
    private boolean ranSuccessfully;
    private boolean passedTesting;
    private String reportDetails;

    public StudentSubmission(String id, String path) {
        this.studentID = id;
        this.extractedSourceDirectory = path;

        this.isCompiled = false;
        this.ranSuccessfully = false;
        this.passedTesting = false;
        this.reportDetails = "Pending evaluation.";
    }

    public void setCompiled(boolean compiled) {
        isCompiled = compiled;
    }

    public void setExtractedSourceDirectory(String extractedSourceDirectory) {
        this.extractedSourceDirectory = extractedSourceDirectory;
    }

    public void setPassedTesting(boolean passedTesting) {
        this.passedTesting = passedTesting;
    }

    public void setRanSuccessfully(boolean ranSuccessfully) {
        this.ranSuccessfully = ranSuccessfully;
    }

    public void setReportDetails(String reportDetails) {
        this.reportDetails = reportDetails;
    }

    public void setStudentID(String studentID) {
        this.studentID = studentID;
    }

    public String getExtractedSourceDirectory() {
        return extractedSourceDirectory;
    }

    public String getReportDetails() {
        return reportDetails;
    }

    public String getStudentID() {
        return studentID;
    }

    public boolean getIsCompiled() {
        return isCompiled;
    }

    public boolean getRanSuccessfully() {
        return ranSuccessfully;
    }

    public boolean getPassedTesting() {
        return passedTesting;
    }
}