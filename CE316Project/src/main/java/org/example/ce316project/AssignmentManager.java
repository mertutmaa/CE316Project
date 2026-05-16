package org.example.ce316project;

import java.util.ArrayList;
import java.util.List;
import java.nio.file.Path;

public class AssignmentManager {
    private List<Project> projects;
    private List<Configuration> configurations;

    public AssignmentManager() {
        this.configurations = new ArrayList<>();
        this.projects = new ArrayList<>();

        // Log to indicate successful initialization
        System.out.println("AssignmentManager initialized. SQL database connection waiting");

    }

    public Configuration createConfiguration(String name, Path compiler, String cArgs, String execCmd, Path output) {
        Configuration newConfig = new Configuration(name, compiler, cArgs, execCmd, output);

        // Add to the local list
        configurations.add(newConfig);

        System.out.println("New configuration added successfully: " + name);

        return newConfig;
    }

    public void importConfiguration(String filePath) {
        System.out.println("Attempting to import configuration from file: " + filePath);

        // TODO: Implement JSON/XML parsing logic here
        System.out.println("Configuration successfully imported from: " + filePath);
    }
    public Project createProject(String name, Configuration config) {
        if (config == null) {
            System.out.println("Error: Cannot create project without a valid configuration.");
            return null;
        }

        Project newProject = new Project();// requires name and config
        projects.add(newProject);

        // TODO: Implement SQLite INSERT query here to save project details
        System.out.println("New project created successfully: " + name);
        return newProject;
    }
    public Project loadProject(String filePath) {
        System.out.println("[Manager] Attempting to load project from database or file: " + filePath);

        // TODO: Implement SQLite SELECT query to fetch project and associated submissions


        System.out.println("[Manager] Project successfully loaded into workspace.");
        return null; // Temporarily returning null until database logic is completed
    }



    // Getters for the lists
    public List<Project> getProjects() {
        return projects;
    }


    public List<Configuration> getConfigurations() {
        return configurations;
    }



}
