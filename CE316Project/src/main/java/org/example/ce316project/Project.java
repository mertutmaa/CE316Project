package org.example.ce316project;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;

public class Project {

    private String name;
    private Path submissionZIPsDirectory;
    private Configuration configuration;
    private List<StudentSubmission> submissions;

    // ─────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────

    public Project(String name, Configuration config) {
        this.name = name;
        this.configuration = config;
        this.submissions = new ArrayList<>();
        System.out.println("[Project] Created: " + name);
    }

    // ─────────────────────────────────────────────
    // ZIP Processing
    // ─────────────────────────────────────────────

    /**
     * Scans the submissionZIPsDirectory, extracts each ZIP file,
     * and creates a StudentSubmission for each one.
     *
     * Expected ZIP naming convention: <studentID>.zip
     * Example: 20210001.zip → studentID = "20210001"
     */
    public void processZIPs() {
        if (submissionZIPsDirectory == null || !Files.isDirectory(submissionZIPsDirectory)) {
            System.out.println("[Project] Error: Submission directory is not set or does not exist.");
            return;
        }

        submissions.clear();

        File[] zipFiles = submissionZIPsDirectory.toFile().listFiles(
                (dir, fileName) -> fileName.toLowerCase().endsWith(".zip")
        );

        if (zipFiles == null || zipFiles.length == 0) {
            System.out.println("[Project] No ZIP files found in: " + submissionZIPsDirectory);
            return;
        }

        for (File zipFile : zipFiles) {
            String studentID = zipFile.getName().replace(".zip", "");

            // Extract to: submissionZIPsDirectory/extracted/<studentID>/
            Path extractTarget = submissionZIPsDirectory
                    .resolve("extracted")
                    .resolve(studentID);

            try {
                extractZIP(zipFile.toPath(), extractTarget);
                StudentSubmission submission = new StudentSubmission(studentID, extractTarget.toString());
                submissions.add(submission);
                System.out.println("[Project] Extracted submission for student: " + studentID);

            } catch (IOException e) {
                System.out.println("[Project] Failed to extract ZIP for student: " + studentID);
                System.out.println("  Details: " + e.getMessage());

                // Still add submission but mark it as failed
                StudentSubmission failedSubmission = new StudentSubmission(studentID, null);
                failedSubmission.setReportDetails("Error: Could not extract ZIP file. " + e.getMessage());
                submissions.add(failedSubmission);
            }
        }

        System.out.println("[Project] processZIPs complete. Total submissions: " + submissions.size());
    }

    /**
     * Extracts a ZIP file to the given target directory.
     * Creates the target directory if it does not exist.
     */
    private void extractZIP(Path zipPath, Path targetDir) throws IOException {
        Files.createDirectories(targetDir);

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath.toFile()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = targetDir.resolve(entry.getName()).normalize();

                // Security: prevent zip-slip attack
                if (!entryPath.startsWith(targetDir)) {
                    throw new IOException("ZIP entry outside target directory: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }

                zis.closeEntry();
            }
        }
    }

    // ─────────────────────────────────────────────
    // Evaluation Loop
    // ─────────────────────────────────────────────

    /**
     * Iterates over all StudentSubmissions and evaluates each one.
     *
     * For each submission:
     *   1. Validates the source directory
     *   2. Compiles and runs via Configuration.executeScript()
     *   3. Compares output with the expected output file
     *   4. Updates the StudentSubmission fields accordingly
     */
    public void runEvaluationLoop() {
        if (configuration == null) {
            System.out.println("[Project] Error: No configuration set. Cannot evaluate.");
            return;
        }

        if (submissions.isEmpty()) {
            System.out.println("[Project] No submissions to evaluate. Run processZIPs() first.");
            return;
        }

        // Read expected output once — same for all students
        String expectedOutput = readExpectedOutput();
        if (expectedOutput == null) {
            System.out.println("[Project] Error: Could not read expected output file.");
            return;
        }

        System.out.println("[Project] Starting evaluation for " + submissions.size() + " submission(s)...");

        for (StudentSubmission submission : submissions) {
            System.out.println("\n[Project] Evaluating student: " + submission.getStudentID());
            evaluateSubmission(submission, expectedOutput);
        }

        System.out.println("\n[Project] Evaluation complete.");
        printSummary();
    }

    /**
     * Evaluates a single StudentSubmission.
     */
    private void evaluateSubmission(StudentSubmission submission, String expectedOutput) {

        // ── Step 1: Check source directory ──────────────
        String sourceDir = submission.getExtractedSourceDirectory();
        if (sourceDir == null || sourceDir.isBlank()) {
            submission.setReportDetails("Error: Source directory is missing or invalid.");
            System.out.println("  [SKIP] No source directory.");
            return;
        }

        if (!Files.isDirectory(Path.of(sourceDir))) {
            submission.setReportDetails("Error: Source directory does not exist on disk: " + sourceDir);
            System.out.println("  [SKIP] Source directory not found on disk.");
            return;
        }

        // ── Step 2: Compile and run ──────────────────────
        String fullLog;
        try {
            // Run with a timeout to guard against infinite loops
            fullLog = runWithTimeout(() -> configuration.executeScript(Path.of(sourceDir)), 30, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            submission.setCompiled(false);
            submission.setRanSuccessfully(false);
            submission.setPassedTesting(false);
            submission.setReportDetails("Error: Execution timed out after 30 seconds. Possible infinite loop.");
            System.out.println("  [TIMEOUT] Student: " + submission.getStudentID());
            return;
        } catch (Exception e) {
            submission.setReportDetails("Error: Unexpected error during execution.\nDetails: " + e.getMessage());
            System.out.println("  [ERROR] " + e.getMessage());
            return;
        }

        // ── Step 3: Parse compile result ─────────────────
        boolean compiledOk = parseCompileSuccess(fullLog);
        submission.setCompiled(compiledOk);

        if (!compiledOk) {
            submission.setRanSuccessfully(false);
            submission.setPassedTesting(false);
            submission.setReportDetails("Compilation failed.\n\n" + fullLog);
            System.out.println("  [FAIL] Compilation error.");
            return;
        }

        // ── Step 4: Parse run result ──────────────────────
        boolean ranOk = parseRunSuccess(fullLog);
        submission.setRanSuccessfully(ranOk);

        if (!ranOk) {
            submission.setPassedTesting(false);
            submission.setReportDetails("Execution failed.\n\n" + fullLog);
            System.out.println("  [FAIL] Runtime error.");
            return;
        }

        // ── Step 5: Compare output ────────────────────────
        String actualOutput = Configuration.extractExecutionOutput(fullLog);
        boolean passed = configuration.compareOutput(actualOutput, expectedOutput);
        submission.setPassedTesting(passed);

        if (passed) {
            submission.setReportDetails("PASSED\n\nActual output matched expected output.\n\nLog:\n" + fullLog);
            System.out.println("  [PASS] Output matches.");
        } else {
            submission.setReportDetails(
                    "FAILED\n\nOutput did not match.\n\n"
                    + "Expected:\n" + expectedOutput + "\n\n"
                    + "Actual:\n" + actualOutput + "\n\n"
                    + "Full Log:\n" + fullLog
            );
            System.out.println("  [FAIL] Output mismatch.");
        }
    }

    // ─────────────────────────────────────────────
    // Helper Methods
    // ─────────────────────────────────────────────

    /**
     * Reads the expected output from the file specified in the configuration.
     * Returns null if the file cannot be read.
     */
    private String readExpectedOutput() {
        Path outputFilePath = configuration.getExpectedOutputFilePath();
        if (outputFilePath == null) {
            System.out.println("[Project] Warning: No expected output file path in configuration.");
            return null;
        }
        try {
            return Files.readString(outputFilePath);
        } catch (IOException e) {
            System.out.println("[Project] Error reading expected output file: " + e.getMessage());
            return null;
        }
    }

    /**
     * Checks if compilation succeeded by looking at the compile section exit code.
     * If there is no compile section (interpreted language), returns true.
     */
    private boolean parseCompileSuccess(String fullLog) {
        if (!fullLog.contains("=== Compilation ===")) {
            // No compilation step — interpreted language (e.g. Python)
            return true;
        }
        // Compilation section present — check its exit code
        String[] lines = fullLog.split("\n");
        boolean inCompileSection = false;
        for (String line : lines) {
            if (line.contains("=== Compilation ===")) {
                inCompileSection = true;
                continue;
            }
            if (inCompileSection && line.startsWith("Exit code:")) {
                String code = line.replace("Exit code:", "").trim();
                return "0".equals(code);
            }
        }
        return false;
    }

    /**
     * Checks if execution succeeded by looking at the execution section exit code.
     */
    private boolean parseRunSuccess(String fullLog) {
        if (!fullLog.contains("=== Execution ===")) {
            return false;
        }
        String[] parts = fullLog.split("=== Execution ===");
        if (parts.length < 2) return false;

        for (String line : parts[1].split("\n")) {
            if (line.startsWith("Exit code:")) {
                String code = line.replace("Exit code:", "").trim();
                return "0".equals(code);
            }
        }
        return false;
    }

    /**
     * Runs a Callable with a timeout.
     * Throws TimeoutException if the task exceeds the time limit.
     */
    private <T> T runWithTimeout(Callable<T> task, long timeout, TimeUnit unit)
            throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<T> future = executor.submit(task);
        try {
            return future.get(timeout, unit);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw e;
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Prints a summary table to the console after evaluation.
     */
    private void printSummary() {
        System.out.println("\n===== EVALUATION SUMMARY =====");
        System.out.printf("%-15s %-10s %-10s %-10s%n",
                "Student ID", "Compiled", "Ran OK", "Passed");
        System.out.println("-".repeat(50));
        for (StudentSubmission s : submissions) {
            System.out.printf("%-15s %-10s %-10s %-10s%n",
                    s.getStudentID(),
                    s.getIsCompiled()       ? "✓" : "✗",
                    s.getRanSuccessfully()  ? "✓" : "✗",
                    s.getPassedTesting()    ? "✓" : "✗"
            );
        }
        System.out.println("==============================");
    }

    // ─────────────────────────────────────────────
    // Getters & Setters
    // ─────────────────────────────────────────────

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Path getSubmissionZIPsDirectory() { return submissionZIPsDirectory; }
    public void setSubmissionZIPsDirectory(Path path) { this.submissionZIPsDirectory = path; }

    public Configuration getConfiguration() { return configuration; }
    public void setConfiguration(Configuration configuration) { this.configuration = configuration; }

    public List<StudentSubmission> getSubmissions() { return submissions; }
}