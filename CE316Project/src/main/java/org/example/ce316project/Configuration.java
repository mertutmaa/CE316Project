package org.example.ce316project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Configuration {

    // ─────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────

    private final String configName;
    private final Path compilerPath;
    private final String compileArguments;
    private final String executionCommand;
    private final Path expectedOutputFilePath;

    /** Default timeout duration (seconds). Protection against infinite loops. */
    private static final int TIMEOUT_SECONDS = 30;

    // ─────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────

    public Configuration(String name, Path compiler, String cArgs, String execCmd, Path output) {
        this.configName            = name;
        this.compilerPath          = compiler;
        this.compileArguments      = cArgs;
        this.executionCommand      = execCmd;
        this.expectedOutputFilePath = output;
    }

    // ─────────────────────────────────────────────
    // Main Execution Method
    // ─────────────────────────────────────────────

    /**
     * Compiles the code (if necessary) and runs it, returning the full log.
     *
     * If compilation fails, it exits early.
     * A 30-second timeout is applied to each step.
     *
     * @param workingDirectory The directory containing the student's code.
     *                         If null, the current directory of the JVM is used.
     * @return The full log containing compilation and execution output
     */
    public String executeScript(Path workingDirectory) {
        StringBuilder fullLog = new StringBuilder();

        // ── Step 1: Compilation (optional) ─────────────────
        if (compilerPath != null
                && compileArguments != null
                && !compileArguments.trim().isEmpty()) {

            List<String> compileCommand = prepareCommand(compilerPath, compileArguments);
            ProcessResult compileResult = runProcess(compileCommand, workingDirectory);

            fullLog.append("=== Compilation ===\n");
            fullLog.append("Exit code: ").append(compileResult.exitCode).append("\n");
            fullLog.append(compileResult.output);

            // If compilation failed, do not proceed to the execution step
            if (compileResult.exitCode != 0) {
                return fullLog.toString();
            }
        }

        // ── Step 2: Execution ──────────────────────────
        if (executionCommand == null || executionCommand.trim().isEmpty()) {
            fullLog.append("\n=== Execution ===\n");
            fullLog.append("Error: No execution command provided. For compiled languages (C, C++, Java),");
            fullLog.append(" you must provide an executionCommand (e.g., 'java -cp bin Main' or './program').\n");
            return fullLog.toString();
        }

        List<String> executionList = parseCommand(executionCommand);
        ProcessResult execResult   = runProcess(executionList, workingDirectory);

        if (!fullLog.isEmpty()) {
            fullLog.append("\n");
        }
        fullLog.append("=== Execution ===\n");
        fullLog.append("Exit code: ").append(execResult.exitCode).append("\n");
        fullLog.append(execResult.output);

        return fullLog.toString();
    }

    /**
     * For backward compatibility — can be called without specifying a working directory.
     * Required for legacy calls inside Project.java.
     */
    public String executeScript() {
        return executeScript(null);
    }

    // ─────────────────────────────────────────────
    // Command Preparation
    // ─────────────────────────────────────────────

    /**
     * Creates a command list from the compiler path and arguments.
     *
     * Example:
     *   compiler = /usr/bin/gcc
     *   args     = -o main main.c
     *   result   = ["/usr/bin/gcc", "-o", "main", "main.c"]
     */
    private List<String> prepareCommand(Path executable, String args) {
        List<String> list = new ArrayList<>();
        list.add(executable.toString());

        if (args != null && !args.trim().isEmpty()) {
            list.addAll(parseCommand(args));
        }

        return list;
    }

    /**
     * Splits a command string into individual pieces.
     * Whitespace within quotation marks is preserved.
     *
     * Example:
     *   "java -cp \"My Programs\" Main"
     *   → ["java", "-cp", "My Programs", "Main"]
     *
     * Fix: The original regex "[^\"\\s]\\S*" missed the first character.
     * Correct pattern: "(\"[^\"]*\"|\\S+)" — captures all tokens successfully.
     */
    private List<String> parseCommand(String commandStr) {
        List<String> list = new ArrayList<>();

        if (commandStr == null || commandStr.trim().isEmpty()) {
            return list;
        }

        // Corrected regex:
        // "([^"]*)"  → extract everything inside quotes as a single token
        // \S+        → extract any sequence of non-whitespace characters
        Pattern pattern = Pattern.compile("\"([^\"]*)\"|\\S+");
        Matcher matcher = pattern.matcher(commandStr);

        while (matcher.find()) {
            if (matcher.group(1) != null) {
                // Token inside quotation marks — add without the quotes
                list.add(matcher.group(1));
            } else {
                // Standard word token
                list.add(matcher.group(0));
            }
        }

        return list;
    }

    // ─────────────────────────────────────────────
    // Process Execution
    // ─────────────────────────────────────────────

    /**
     * Minimal helper class — encapsulates the process exit code and its text output.
     */
    private static class ProcessResult {
        final int exitCode;
        final String output;

        ProcessResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output   = output;
        }
    }

    /**
     * Runs the provided command as a subprocess.
     * Merges stdout and stderr into a single stream.
     * Forcibly terminates the process if TIMEOUT_SECONDS is exceeded.
     *
     * @param command          Command and parameters to run
     * @param workingDirectory Directory where the process will run (null → current JVM directory)
     * @return ProcessResult (exit code + output string)
     */
    private ProcessResult runProcess(List<String> command, Path workingDirectory) {
        StringBuilder output = new StringBuilder();
        int exitCode = -1;

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true); // Merge stdout + stderr

            if (workingDirectory != null && workingDirectory.toFile().isDirectory()) {
                pb.directory(workingDirectory.toFile());
            }

            Process process = pb.start();

            // Read output in a separate thread — prevents buffer blockages on large data outputs
            ExecutorService reader = Executors.newSingleThreadExecutor();
            Future<String> outputFuture = reader.submit(() -> {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                }
                return sb.toString();
            });

            // Wait with strict timeout constraints
            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                reader.shutdownNow();
                output.append("Error: Process timed out after ")
                      .append(TIMEOUT_SECONDS)
                      .append(" seconds. Possible infinite loop.\n");
                return new ProcessResult(-1, output.toString());
            }

            // Fetch the logged execution output
            try {
                output.append(outputFuture.get(5, TimeUnit.SECONDS));
            } catch (TimeoutException | ExecutionException e) {
                output.append("Warning: Could not fully read process output.\n");
            }

            reader.shutdown();
            exitCode = process.exitValue();

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            exitCode = -1;
            output.append("Error: Process execution was interrupted.\n");

        } catch (IOException e) {
            exitCode = -1;
            output.append("Error: Could not start process. Verify the command path is correct.\n");
            output.append("Details: ").append(e.getMessage()).append("\n");
        }

        return new ProcessResult(exitCode, output.toString());
    }

    // ─────────────────────────────────────────────
    // Output Comparison
    // ─────────────────────────────────────────────

    /**
     * Normalizes and compares two output logs.
     *
     * @param actualOutput   The actual program output
     * @param expectedOutput The expected base output
     * @return true → they match, false → they mismatch
     */
    public boolean compareOutput(String actualOutput, String expectedOutput) {
        if (actualOutput == null || expectedOutput == null) {
            return Objects.equals(actualOutput, expectedOutput);
        }

        String normalized1 = normalizeOutput(actualOutput);
        String normalized2 = normalizeOutput(expectedOutput);

        return normalized1.equals(normalized2);
    }

    /**
     * Compares the executeScript() output directly with the expected output file.
     * Reads the expected verification content from expectedOutputFilePath.
     *
     * @param fullLog The complete log produced by executeScript()
     * @return true → they match, false → they mismatch or file could not be read
     */
    public boolean compareWithExpectedFile(String fullLog) {
        if (expectedOutputFilePath == null) {
            System.out.println("[Config] Error: Expected output file path is not defined.");
            return false;
        }

        try {
            String expectedOutput = Files.readString(expectedOutputFilePath);
            String actualOutput   = extractExecutionOutput(fullLog);
            return compareOutput(actualOutput, expectedOutput);
        } catch (IOException e) {
            System.out.println("[Config] Expected output file could not be read: " + e.getMessage());
            return false;
        }
    }

    /**
     * Compares the executeScript() output with an already parsed string block.
     * Useful if the reference string has already been buffered into RAM.
     *
     * @param fullLog        The complete log produced by executeScript()
     * @param expectedOutput The expected string to compare against
     * @return true → they match safely
     */
    public boolean compareExecutionOutput(String fullLog, String expectedOutput) {
        String rawOutput = extractExecutionOutput(fullLog);
        return compareOutput(rawOutput, expectedOutput);
    }

    /**
     * Normalizes text structure for clean data comparisons:
     * - standardizes line endings to \n (Cross-platform safe)
     * - drops trailing spaces across each line block
     * - strips leading and trailing outer block whitespaces
     */
    private String normalizeOutput(String output) {
        if (output == null) return "";

        String normalized = output.replaceAll("\\r\\n|\\r", "\n");
        normalized = normalized.replaceAll("(?m)[ \\t]+$", "");
        return normalized.trim();
    }

    /**
     * Isolates only the pure program script output from the broader logs.
     * Targets text block below "=== Execution ===", filtering out any
     * lines prefixed by "Exit code:".
     *
     * @param fullLog The complete log produced by executeScript()
     * @return Isolated clean program string
     */
    public static String extractExecutionOutput(String fullLog) {
        if (fullLog == null) return "";

        String[] parts = fullLog.split("=== Execution ===");
        if (parts.length > 1) {
            String execPart = parts[1];
            String[] lines  = execPart.split("\n");
            StringBuilder output = new StringBuilder();

            for (int i = 0; i < lines.length; i++) {
                if (!lines[i].startsWith("Exit code:") && !lines[i].isBlank()) {
                    output.append(lines[i]);
                    if (i < lines.length - 1) {
                        output.append("\n");
                    }
                }
            }
            return output.toString().trim();
        }

        return fullLog.trim();
    }

    // ─────────────────────────────────────────────
    // Getters
    // ─────────────────────────────────────────────

    public String getConfigName()            { return configName; }
    public Path   getCompilerPath()          { return compilerPath; }
    public String getCompileArguments()      { return compileArguments; }
    public String getExecutionCommand()      { return executionCommand; }
    public Path   getExpectedOutputFilePath(){ return expectedOutputFilePath; }
}