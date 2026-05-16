package org.example.ce316project;

import java.util.ArrayList;
import java.util.List;
import java.nio.file.Path;

public class AssignmentManager {
    private List<Configuration> configurations;

    public AssignmentManager() {
        this.configurations = new ArrayList<>();

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

    public List<Configuration> getConfigurations() {
        return configurations;
    }


}
