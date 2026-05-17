package org.example.ce316project;

import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * AssignmentManager — Central manager class.
 *
 * Responsibility:
 *  - Establishing the SQLite database connection and creating tables
 *  - Creating, saving, and loading Configuration objects
 *  - Creating, saving, and loading Project objects
 *  - Keeping in-memory lists synchronized with the database
 */
public class AssignmentManager {

    // ─────────────────────────────────────────────
    // Constants
    // ─────────────────────────────────────────────

    /** Veritabanı dosyasının yolu. Uygulama dizininde oluşturulur. */
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
     * Opens the SQLite connection and creates the required tables.
     * If the tables already exist, they are left untouched (IF NOT EXISTS).
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
     * @param name       Configuration name (must be unique)
     * @param compiler   Compiler path (e.g., /usr/bin/gcc) — can be null
     * @param cArgs      Compilation arguments (e.g., -o main main.c) — can be null
     * @param execCmd    Execution command (e.g., ./main) — mandatory
     * @param output     Expected output file path — can be null
     * @return The created Configuration object, or null in case of error
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

        // Check if a configuration with the same name already exists
        if (findConfigurationByName(name) != null) {
            System.out.println("[Manager] Error: A configuration named '" + name + "' already exists.");
            return null;
        }

        Configuration newConfig = new Configuration(name, compiler, cArgs, execCmd, output);

        // Veritabanına kaydet
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
            System.out.println("[Manager] Configuration could not be saved: " + e.getMessage());
            return null;
        }

        configurations.add(newConfig);
        return newConfig;
    }

    /**
     * Imports configuration from a JSON/XML file.
     * TODO: JSON/XML parsing implementation to be added.
     *
     * @param filePath Path of the file to be imported
     */
    public void importConfiguration(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            System.out.println("[Manager] Error: File path cannot be empty.");
            return;
        }
        System.out.println("[Manager] Import not yet implemented: " + filePath);
        // TODO: Parse JSON/XML and call createConfiguration()
    }

    // ─────────────────────────────────────────────
    // Project — Create & Save
    // ─────────────────────────────────────────────

    /**
     * Creates a new Project, saves it to the database, and adds it to the list.
     *
     * @param name   Project name (must be unique)
     * @param config The Configuration linked to this project — cannot be null
     * @return The created Project object, or null in case of error
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

        // Check if a project with the same name already exists
        if (findProjectByName(name) != null) {
            System.out.println("[Manager] Error: A project named '" + name + "' already exists.");
            return null;
        }

        Project newProject = new Project(name, config);

        // Add into database
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
            System.out.println("[Manager] Project could not be saved: " + e.getMessage());
            return null;
        }

        projects.add(newProject);
        return newProject;
    }

    /**
     * Updates the ZIP directory of an existing project in the database.
     *
     * @param project       The project to be updated
     * @param zipDirectory  The new ZIP directory
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
            System.out.println("[Manager] ZIP directory could not be updated: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // Loading — From Database to RAM
    // ─────────────────────────────────────────────

    /**
     * Loads all records from the database upon application startup.
     * Configurations are loaded first, followed by projects
     * (the order is important since projects depend on configurations).
     */
    private void loadAllFromDatabase() {
        loadConfigurations();
        loadProjects();
    }

    /**
     * Reads all configuration from the database and adds them to the list
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

            System.out.println("[DB] " + configurations.size() + " configurations loaded.");

        } catch (SQLException e) {
            System.out.println("[DB] Configurations could not be loaded:" + e.getMessage());
        }
    }

    /**
     * Reads all projects from the database and adds them to the list.
     * Finds the configuration for each project from the in-memory list.
     */
    private void loadProjects() {
        String sql = "SELECT name, config_name, zip_directory FROM projects";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String name       = rs.getString("name");
                String configName = rs.getString("config_name");
                String zipDir     = rs.getString("zip_directory");

                // Find the configuration from the in-memory list
                Configuration config = findConfigurationByName(configName);
                if (config == null) {
                    System.out.println("[DB] Warning: Configuration not found for project '" + name
                            + "': " + configName);
                    continue;
                }

                Project project = new Project(name, config);
                if (zipDir != null) {
                    project.setSubmissionZIPsDirectory(Path.of(zipDir));
                }

                projects.add(project);
            }

            System.out.println("[DB] " + projects.size() + " projects loaded.");

        } catch (SQLException e) {
            System.out.println("[DB] Projects could not be loaded: " + e.getMessage());
        }
    }

    /**
     * Loads a specific project by its name or file path.
     * Searches the in-memory list first, then falls back to the database.
     *
     * @param nameOrPath Project name
     * @return The found Project object, or null if not found
     */
    public Project loadProject(String nameOrPath) {
        if (nameOrPath == null || nameOrPath.isBlank()) return null;

        // Search in RAM first
        Project found = findProjectByName(nameOrPath);
        if (found != null) {
            System.out.println("[Manager] Project loaded from RAM: " + nameOrPath);
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
            System.out.println("[Manager] Project could not be loaded: " + e.getMessage());
        }

        System.out.println("[Manager] Project not found: " + nameOrPath);
        return null;
    }

    // ─────────────────────────────────────────────
    // Deletion & Update Operations
    // ─────────────────────────────────────────────

    
    /**
     * Deletes a configuration from both the in-memory list and the database.
     *
     * @param name Name of the configuration to delete
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
        System.out.println("[Manager] Konfigürasyon güncellendi: " + originalName);
    } catch (SQLException e) {
        System.out.println("[Manager] Güncelleme başarısız: " + e.getMessage());
        return null;
    }

    Configuration updated = new Configuration(originalName, compiler, cArgs, execCmd, output);
    configurations.add(updated);
    return updated;
}


    public void deleteConfiguration(String name) {
        configurations.removeIf(c -> c.getConfigName().equals(name));

        String sql = "DELETE FROM configurations WHERE name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                System.out.println("[Manager] Configuration deleted: " + name);
            } else {
                System.out.println("[Manager] Configuration to delete not found: " + name);
            }
        } catch (SQLException e) {
            System.out.println("[Manager] Configuration could not be deleted: " + e.getMessage());
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
                System.out.println("[Manager] Project to delete not found: " + name);
            }
        } catch (SQLException e) {
            System.out.println("[Manager] Project could not be deleted: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // Search Helpers
    // ─────────────────────────────────────────────

    /**
     * Searches for a configuration by name in the in-memory list.
     */
    public Configuration findConfigurationByName(String name) {
        return configurations.stream()
                .filter(c -> c.getConfigName().equals(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Searches for a project by name in the in-memory list.
     */
    public Project findProjectByName(String name) {
        return projects.stream()
                .filter(p -> p.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    // ─────────────────────────────────────────────
    // Closing Connection
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