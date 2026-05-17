package org.example.ce316project;

import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * AssignmentManager — central manager class.
 *
 * Responsibilities:
 *  - Establish SQLite database connection and create tables
 *  - Create, save, and load Configuration objects
 *  - Create, save, and load Project objects
 *  - Keep in-memory lists synchronized with the database
 */
public class AssignmentManager {

    // ─────────────────────────────────────────────
    // Constants
    // ─────────────────────────────────────────────

    /** Path to the database file. Created in the application directory. */
    private static final String DB_URL = "jdbc:sqlite:assignment_manager.db";

    // ─────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────

    private final List<Project> projects;
    private final List<Configuration> configurations;
    private Connection connection;

    // ─────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────

    public AssignmentManager() {
        this.projects = new ArrayList<>();
        this.configurations = new ArrayList<>();

        initDatabase();
        loadAllFromDatabase();
    }

    // ─────────────────────────────────────────────
    // Database Setup
    // ─────────────────────────────────────────────

    /**
     * Opens the SQLite connection and creates required tables.
     * Existing tables are left untouched (IF NOT EXISTS).
     */
    private void initDatabase() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            System.out.println("[DB] SQLite connection established: " + DB_URL);
            createTables();
        } catch (SQLException e) {
            System.out.println("[DB] Connection error: " + e.getMessage());
        }
    }

    /**
     * Creates the configurations and projects tables.
     */
    private void createTables() throws SQLException {
        String createConfigurations = """
                CREATE TABLE IF NOT EXISTS configurations (
                    id            INTEGER PRIMARY KEY AUTOINCREMENT,
                    name          TEXT    NOT NULL UNIQUE,
                    compiler_path TEXT,
                    compile_args  TEXT,
                    exec_cmd      TEXT    NOT NULL,
                    output_path   TEXT
                );
                """;

        String createProjects = """
                CREATE TABLE IF NOT EXISTS projects (
                    id               INTEGER PRIMARY KEY AUTOINCREMENT,
                    name             TEXT    NOT NULL UNIQUE,
                    config_name      TEXT    NOT NULL,
                    zip_directory    TEXT,
                    FOREIGN KEY (config_name) REFERENCES configurations(name)
                );
                """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createConfigurations);
            stmt.execute(createProjects);
            System.out.println("[DB] Tables are ready.");
        }
    }

    // ─────────────────────────────────────────────
    // Configuration — Create & Save
    // ─────────────────────────────────────────────

    /**
     * Creates a new Configuration, saves it to the database, and adds it to the list.
     *
     * @param name     Configuration name (must be unique)
     * @param compiler Compiler path (e.g. /usr/bin/gcc) — nullable
     * @param cArgs    Compile arguments (e.g. -o main main.c) — nullable
     * @param execCmd  Execution command (e.g. ./main) — required
     * @param output   Expected output file path — nullable
     * @return The created Configuration object, or null on failure
     */
    public Configuration createConfiguration(String name, Path compiler,
                                             String cArgs, String execCmd, Path output) {
        if (name == null || name.isBlank()) {
            System.out.println("[Manager] Error: Configuration name cannot be empty.");
            return null;
        }
        if (execCmd == null || execCmd.isBlank()) {
            System.out.println("[Manager] Error: Execution command cannot be empty.");
            return null;
        }

        // Check for duplicate name
        if (findConfigurationByName(name) != null) {
            System.out.println("[Manager] Error: A configuration named '" + name + "' already exists.");
            return null;
        }

        Configuration newConfig = new Configuration(name, compiler, cArgs, execCmd, output);

        // Save to database
        String sql = """
                INSERT INTO configurations (name, compiler_path, compile_args, exec_cmd, output_path)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, compiler != null ? compiler.toString() : null);
            pstmt.setString(3, cArgs);
            pstmt.setString(4, execCmd);
            pstmt.setString(5, output != null ? output.toString() : null);
            pstmt.executeUpdate();
            System.out.println("[Manager] Configuration saved: " + name);
        } catch (SQLException e) {
            System.out.println("[Manager] Failed to save configuration: " + e.getMessage());
            return null;
        }

        configurations.add(newConfig);
        return newConfig;
    }

    /**
 * Exports a configuration to a JSON file.
 *
 * @param configName Name of the configuration to export
 * @param filePath   Destination file path (e.g. C:\configs\myconfig.json)
 * @return true if successful, false otherwise
 */
public boolean exportConfiguration(String configName, String filePath) {
    Configuration config = findConfigurationByName(configName);
    if (config == null) {
        System.out.println("[Manager] Export failed: Configuration not found: " + configName);
        return false;
    }

    try {
        com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();

        java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
        map.put("name",         config.getConfigName());
        map.put("compilerPath", config.getCompilerPath() != null
                ? config.getCompilerPath().toString() : "");
        map.put("compileArgs",  config.getCompileArguments() != null
                ? config.getCompileArguments() : "");
        map.put("execCmd",      config.getExecutionCommand());
        map.put("outputPath",   config.getExpectedOutputFilePath() != null
                ? config.getExpectedOutputFilePath().toString() : "");

        mapper.writerWithDefaultPrettyPrinter()
              .writeValue(new java.io.File(filePath), map);

        System.out.println("[Manager] Configuration exported: " + filePath);
        return true;

    } catch (Exception e) {
        System.out.println("[Manager] Export failed: " + e.getMessage());
        return false;
    }
}

/**
 * Imports a configuration from a JSON file.
 * If a configuration with the same name already exists, it is skipped.
 *
 * @param filePath Path to the JSON file to import
 * @return The imported Configuration object, or null on failure
 */
public Configuration importConfiguration(String filePath) {
    try {
        com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();

        java.util.Map<String, String> map = mapper.readValue(
                new java.io.File(filePath),
                new com.fasterxml.jackson.core.type.TypeReference<
                        java.util.Map<String, String>>() {}
        );

        String name        = map.getOrDefault("name", "");
        String compilerStr = map.getOrDefault("compilerPath", "");
        String cArgs       = map.getOrDefault("compileArgs", "");
        String execCmd     = map.getOrDefault("execCmd", "");
        String outputStr   = map.getOrDefault("outputPath", "");

        if (name.isBlank() || execCmd.isBlank()) {
            System.out.println("[Manager] Import failed: 'name' or 'execCmd' is missing in file.");
            return null;
        }

        Path compiler = compilerStr.isBlank() ? null : Path.of(compilerStr);
        Path output   = outputStr.isBlank()   ? null : Path.of(outputStr);
        String args   = cArgs.isBlank()       ? null : cArgs;

        Configuration imported = createConfiguration(name, compiler, args, execCmd, output);
        if (imported != null) {
            System.out.println("[Manager] Configuration imported: " + name);
        }
        return imported;

    } catch (Exception e) {
        System.out.println("[Manager] Import failed: " + e.getMessage());
        return null;
    }
}

    // ─────────────────────────────────────────────
    // Project — Create & Save
    // ─────────────────────────────────────────────

    /**
     * Creates a new Project, saves it to the database, and adds it to the list.
     *
     * @param name   Project name (must be unique)
     * @param config The Configuration linked to this project — cannot be null
     * @return The created Project object, or null on failure
     */
    public Project createProject(String name, Configuration config) {
        if (name == null || name.isBlank()) {
            System.out.println("[Manager] Error: Project name cannot be empty.");
            return null;
        }
        if (config == null) {
            System.out.println("[Manager] Error: Cannot create a project without a valid configuration.");
            return null;
        }

        // Check for duplicate name
        if (findProjectByName(name) != null) {
            System.out.println("[Manager] Error: A project named '" + name + "' already exists.");
            return null;
        }

        Project newProject = new Project(name, config);

        // Save to database
        String sql = """
                INSERT INTO projects (name, config_name, zip_directory)
                VALUES (?, ?, ?)
                """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, config.getConfigName());
            pstmt.setString(3, newProject.getSubmissionZIPsDirectory() != null
                    ? newProject.getSubmissionZIPsDirectory().toString()
                    : null);
            pstmt.executeUpdate();
            System.out.println("[Manager] Project saved to database: " + name);
        } catch (SQLException e) {
            System.out.println("[Manager] Failed to save project: " + e.getMessage());
            return null;
        }

        projects.add(newProject);
        return newProject;
    }

    /**
     * Updates the ZIP directory of an existing project in the database.
     *
     * @param project      The project to update
     * @param zipDirectory The new ZIP directory path
     */
    public void updateProjectZipDirectory(Project project, Path zipDirectory) {
        if (project == null || zipDirectory == null) return;

        project.setSubmissionZIPsDirectory(zipDirectory);

        String sql = "UPDATE projects SET zip_directory = ? WHERE name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, zipDirectory.toString());
            pstmt.setString(2, project.getName());
            pstmt.executeUpdate();
            System.out.println("[Manager] Project ZIP directory updated: " + project.getName());
        } catch (SQLException e) {
            System.out.println("[Manager] Failed to update ZIP directory: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // Loading — Database to Memory
    // ─────────────────────────────────────────────

    /**
     * Loads all records from the database on application startup.
     * Configurations are loaded before projects
     * since projects depend on configurations.
     */
    private void loadAllFromDatabase() {
        loadConfigurations();
        loadProjects();
    }

    /**
     * Reads all configurations from the database and adds them to the list.
     */
    private void loadConfigurations() {
        String sql = "SELECT name, compiler_path, compile_args, exec_cmd, output_path FROM configurations";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String name        = rs.getString("name");
                String compilerStr = rs.getString("compiler_path");
                String cArgs       = rs.getString("compile_args");
                String execCmd     = rs.getString("exec_cmd");
                String outputStr   = rs.getString("output_path");

                Path compiler = compilerStr != null ? Path.of(compilerStr) : null;
                Path output   = outputStr   != null ? Path.of(outputStr)   : null;

                Configuration config = new Configuration(name, compiler, cArgs, execCmd, output);
                configurations.add(config);
            }

            System.out.println("[DB] " + configurations.size() + " configuration(s) loaded.");

        } catch (SQLException e) {
            System.out.println("[DB] Failed to load configurations: " + e.getMessage());
        }
    }

    /**
     * Reads all projects from the database and adds them to the list.
     * Finds each project's configuration from the in-memory list.
     */
    private void loadProjects() {
        String sql = "SELECT name, config_name, zip_directory FROM projects";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String name       = rs.getString("name");
                String configName = rs.getString("config_name");
                String zipDir     = rs.getString("zip_directory");

                // Find configuration from in-memory list
                Configuration config = findConfigurationByName(configName);
                if (config == null) {
                    System.out.println("[DB] Warning: Configuration not found for project '"
                            + name + "': " + configName);
                    continue;
                }

                Project project = new Project(name, config);
                if (zipDir != null) {
                    project.setSubmissionZIPsDirectory(Path.of(zipDir));
                }

                projects.add(project);
            }

            System.out.println("[DB] " + projects.size() + " project(s) loaded.");

        } catch (SQLException e) {
            System.out.println("[DB] Failed to load projects: " + e.getMessage());
        }
    }

    /**
     * Loads a specific project by name.
     * Searches the in-memory list first, then the database.
     *
     * @param nameOrPath Project name
     * @return The found Project object, or null if not found
     */
    public Project loadProject(String nameOrPath) {
        if (nameOrPath == null || nameOrPath.isBlank()) return null;

        // Search in memory first
        Project found = findProjectByName(nameOrPath);
        if (found != null) {
            System.out.println("[Manager] Project loaded from memory: " + nameOrPath);
            return found;
        }

        // Search in database
        String sql = "SELECT name, config_name, zip_directory FROM projects WHERE name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nameOrPath);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String configName = rs.getString("config_name");
                String zipDir     = rs.getString("zip_directory");

                Configuration config = findConfigurationByName(configName);
                if (config == null) {
                    System.out.println("[Manager] Configuration not found: " + configName);
                    return null;
                }

                Project project = new Project(nameOrPath, config);
                if (zipDir != null) {
                    project.setSubmissionZIPsDirectory(Path.of(zipDir));
                }

                projects.add(project);
                System.out.println("[Manager] Project loaded from database: " + nameOrPath);
                return project;
            }

        } catch (SQLException e) {
            System.out.println("[Manager] Failed to load project: " + e.getMessage());
        }

        System.out.println("[Manager] Project not found: " + nameOrPath);
        return null;
    }

    // ─────────────────────────────────────────────
    // Update
    // ─────────────────────────────────────────────

    /**
     * Updates an existing configuration in both memory and the database.
     *
     * @param originalName The current name of the configuration to update
     * @param compiler     New compiler path — nullable
     * @param cArgs        New compile arguments — nullable
     * @param execCmd      New execution command — required
     * @param output       New expected output file path — nullable
     * @return The updated Configuration object, or null on failure
     */
    public Configuration updateConfiguration(String originalName, Path compiler,
                                             String cArgs, String execCmd, Path output) {
        configurations.removeIf(c -> c.getConfigName().equals(originalName));

        String sql = """
                UPDATE configurations
                SET compiler_path = ?, compile_args = ?, exec_cmd = ?, output_path = ?
                WHERE name = ?
                """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, compiler != null ? compiler.toString() : null);
            pstmt.setString(2, cArgs);
            pstmt.setString(3, execCmd);
            pstmt.setString(4, output != null ? output.toString() : null);
            pstmt.setString(5, originalName);
            pstmt.executeUpdate();
            System.out.println("[Manager] Configuration updated: " + originalName);
        } catch (SQLException e) {
            System.out.println("[Manager] Update failed: " + e.getMessage());
            return null;
        }

        Configuration updated = new Configuration(originalName, compiler, cArgs, execCmd, output);
        configurations.add(updated);
        return updated;
    }

    // ─────────────────────────────────────────────
    // Delete
    // ─────────────────────────────────────────────

    /**
     * Deletes a configuration from both the in-memory list and the database.
     *
     * @param name Name of the configuration to delete
     */
    public void deleteConfiguration(String name) {
        configurations.removeIf(c -> c.getConfigName().equals(name));

        String sql = "DELETE FROM configurations WHERE name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                System.out.println("[Manager] Configuration deleted: " + name);
            } else {
                System.out.println("[Manager] Configuration not found for deletion: " + name);
            }
        } catch (SQLException e) {
            System.out.println("[Manager] Failed to delete configuration: " + e.getMessage());
        }
    }

    /**
     * Deletes a project from both the in-memory list and the database.
     *
     * @param name Name of the project to delete
     */
    public void deleteProject(String name) {
        projects.removeIf(p -> p.getName().equals(name));

        String sql = "DELETE FROM projects WHERE name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                System.out.println("[Manager] Project deleted: " + name);
            } else {
                System.out.println("[Manager] Project not found for deletion: " + name);
            }
        } catch (SQLException e) {
            System.out.println("[Manager] Failed to delete project: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // Search Helpers
    // ─────────────────────────────────────────────

    /**
     * Searches the in-memory list for a configuration by name.
     */
    public Configuration findConfigurationByName(String name) {
        return configurations.stream()
                .filter(c -> c.getConfigName().equals(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Searches the in-memory list for a project by name.
     */
    public Project findProjectByName(String name) {
        return projects.stream()
                .filter(p -> p.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    // ─────────────────────────────────────────────
    // Close Connection
    // ─────────────────────────────────────────────

    /**
     * Closes the database connection when the application shuts down.
     * Call this method inside Stage.setOnCloseRequest().
     */
    public void closeDatabase() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DB] Connection closed.");
            }
        } catch (SQLException e) {
            System.out.println("[DB] Error while closing connection: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // Getters
    // ─────────────────────────────────────────────

    public List<Project> getProjects() {
        return projects;
    }

    public List<Configuration> getConfigurations() {
        return configurations;
    }
}