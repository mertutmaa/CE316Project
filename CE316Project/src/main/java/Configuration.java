import java.nio.file.Path;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Configuration {
    private final String configName;
    private final Path compilerPath;
    private final String compileArguments;
    private final String executionCommand;
    private final Path expectedOutputFilePath;

    public Configuration(String name, Path compiler, String cArgs, String execCmd, Path output) {
        this.configName = name;
        this.compilerPath = compiler;
        this.compileArguments = cArgs;
        this.executionCommand = execCmd;
        this.expectedOutputFilePath = output;
    }

    /**
     * Compiles the code if needed, then runs it and returns the full log.
     * The log includes both the compile step and the run step.
     *
     * If compilation fails, the method stops and returns that result.
     * If an execution command is missing, it returns a clear error message.
     */
    public String executeScript() {
        StringBuilder fullLog = new StringBuilder();

        // Step 1: Compile if compiler path and arguments are provided
        if (compilerPath != null && compileArguments != null && !compileArguments.trim().isEmpty()) {
            List<String> compileCommand = prepareCommand(compilerPath, compileArguments);
            ProcessResult compileResult = runProcess(compileCommand);

            fullLog.append("=== Compilation ===\n");
            fullLog.append("Exit code: ").append(compileResult.exitCode).append("\n");
            fullLog.append(compileResult.output);

            // If compilation failed, return early
            if (compileResult.exitCode != 0) {
                return fullLog.toString();
            }
        }

        // Step 2: Execute using executionCommand
        if (executionCommand == null || executionCommand.trim().isEmpty()) {
            // This is a configuration error; return early with a clear message.
            fullLog.append("\n=== Execution ===\n");
            fullLog.append("Error: No execution command provided. For compiled languages (C, C++, Java),");
            fullLog.append(" you must provide an executionCommand (e.g., 'java -cp bin Main' or './program').\n");
            return fullLog.toString();
        }

        List<String> executionList = parseCommand(executionCommand);

        ProcessResult execResult = runProcess(executionList);

        if (!fullLog.isEmpty()) {
            fullLog.append("\n=== Execution ===\n");
        }
        fullLog.append("Exit code: ").append(execResult.exitCode).append("\n");
        fullLog.append(execResult.output);

        return fullLog.toString();
    }

    /**
     * Builds the command list for the compiler or other executable.
     */
    private List<String> prepareCommand(Path executable, String args) {
        List<String> list = new ArrayList<>();
        list.add(executable.toString());

        if (args != null && !args.trim().isEmpty()) {
            // Keep the same parsing rule here so quoted arguments stay intact.
            list.addAll(parseCommand(args));
        }

        return list;
    }

    /**
     * Splits a command string into parts.
     * Quoted text stays together, so paths with spaces still work.
     *
     * Example: java -cp "My Programs" Main
     * becomes: [java, -cp, My Programs, Main]
     */
    private List<String> parseCommand(String commandStr) {
        List<String> list = new ArrayList<>();

        if (commandStr == null || commandStr.trim().isEmpty()) {
            return list;
        }

        // Match either a quoted piece or a normal space-free piece.
        Pattern pattern = Pattern.compile("[^\"\\s]\\S*|\"[^\"]*\"");
        Matcher matcher = pattern.matcher(commandStr);

        while (matcher.find()) {
            String token = matcher.group(0);
            // Remove surrounding quotes if present
            if (token.startsWith("\"") && token.endsWith("\"")) {
                token = token.substring(1, token.length() - 1);
            }
            list.add(token);
        }

        return list;
    }

    /**
     * Small holder for a process exit code and its output.
     */
    private static class ProcessResult {
        int exitCode;
        String output;

        ProcessResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }
    }

    /**
     * Runs a command and collects its output.
     * stdout and stderr are merged so the result is easier to read.
     */
    private ProcessResult runProcess(List<String> command) {
        StringBuilder output = new StringBuilder();
        int exitCode = -1;

        try {
            ProcessBuilder pb = new ProcessBuilder(command);

            // Put stdout and stderr together so we do not miss errors.
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // Read all output before waiting for the process to finish.
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // Wait until the command finishes.
            exitCode = process.waitFor();

        } catch (InterruptedException ie) {
            // Keep the interrupt flag set so higher-level code can notice it.
            Thread.currentThread().interrupt();
            exitCode = -1;
            output.append("Error: Process execution was interrupted.\n");

        } catch (IOException e) {
            // Usually means the command was wrong or not on PATH.
            exitCode = -1;
            output.append("Error: Could not start process. Verify the command path is correct.\n");
            output.append("Details: ").append(e.getMessage()).append("\n");
        }

        return new ProcessResult(exitCode, output.toString());
    }

    /**
     * Compares two outputs after cleaning them up a bit first.
     * Use this when you already have the raw program output.
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
     * Shortcut for comparing the output from executeScript() with an expected file.
     */
    public boolean compareExecutionOutput(String fullLog, String expectedOutput) {
        String rawOutput = extractExecutionOutput(fullLog);
        return compareOutput(rawOutput, expectedOutput);
    }

    /**
     * Makes output easier to compare by fixing line endings and extra spaces.
     */
    private String normalizeOutput(String output) {
        if (output == null) {
            return "";
        }

        // Turn all line endings into \n.
        String normalized = output.replaceAll("\\r\\n|\\r", "\n");

        // Remove extra spaces at the end of each line.
        normalized = normalized.replaceAll("(?m)[ \\t]+$", "");

        // Remove blank space around the whole output.
        return normalized.trim();
    }

    /**
     * Pulls out just the program output from the full log.
     */
    public static String extractExecutionOutput(String fullLog) {
        String[] parts = fullLog.split("=== Execution ===");
        if (parts.length > 1) {
            String execPart = parts[1];
            String[] lines = execPart.split("\n");
            StringBuilder output = new StringBuilder();

            for (int i = 1; i < lines.length; i++) {
                // Skip the exit code line and keep the actual program output.
                if (!lines[i].startsWith("Exit code:")) {
                    output.append(lines[i]);
                    if (i < lines.length - 1) {
                        output.append("\n");
                    }
                }
            }
            return output.toString();
        }
        return fullLog;
    }

    // Simple getters
    public String getConfigName() { return configName; }
    public Path getCompilerPath() { return compilerPath; }
    public String getCompileArguments() { return compileArguments; }
    public String getExecutionCommand() { return executionCommand; }
    public Path getExpectedOutputFilePath() { return expectedOutputFilePath; }

}
