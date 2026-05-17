# IAE — Integrated Assignment Evaluator

A lightweight desktop application for managing and automatically evaluating programming assignments. Built with Java 21 and JavaFX, powered by SQLite for persistent storage.

---

## Overview

IAE allows lecturers to create assignment projects, define language-specific configurations, and automatically compile, run, and compare the output of student submissions — all from a simple graphical interface.

---

## Features

- **Project Management** — Create and open assignment projects, each linked to a configuration
- **Configuration Management** — Define compiler paths, arguments, and run commands for any programming language
- **Import / Export Configurations** — Back up and share configurations as JSON files
- **Automatic ZIP Processing** — Drop student ZIP files into a directory; IAE extracts and processes them automatically
- **Compilation & Execution** — Supports compiled languages (C, C++, Java) and interpreted languages (Python, etc.)
- **Output Comparison** — Compares student program output against an expected output file
- **Results Dashboard** — Displays PASSED / FAILED status and full execution logs for each student
- **Persistent Storage** — All projects and configurations are saved to a local SQLite database
- **Windows Installer** — Ships as a standalone `.exe` installer with a desktop shortcut

---

## Requirements

| Requirement                             | Status |
| --------------------------------------- | ------ |
| Windows installer with desktop shortcut | ✅     |
| Help menu with user manual              | ✅     |
| Create projects with configurations     | ✅     |
| Create, edit, and remove configurations | ✅     |
| Import and export configurations        | ✅     |
| Process student ZIP files automatically | ✅     |
| Compile or interpret source code        | ✅     |
| Compare output with expected output     | ✅     |
| Display results for each student        | ✅     |
| Open and save projects                  | ✅     |

---

## Installation

1. Run `IAE_Setup.exe`
2. Follow the installation wizard
3. A shortcut named **IAE** will be added to your desktop
4. Double-click the shortcut to launch the application

> **Prerequisite:** Java 21 must be installed on the target machine.

---

## Quick Test

A ready-to-use test environment is included with the project (`test_submissions` folder).

1. Open the application
2. Go to **Configuration → Manage Configurations**
   - Select `Test Config` from the list
   - Set the **Expected Output** field to the full path of `expected.txt`:
     ```
     Example: C:\Users\user\Desktop\test_submissions\expected.txt
     ```
   - Click **Save Configuration**
3. Go to **File → Open Project** → Enter: `Test Project`
4. Click **Browse...** and select the `test_submissions` folder
5. Click **Run Assignments**
6. Student `20210001` should show **✓ PASSED**

---

## General Usage

### Step 1 — Create a Configuration

Go to **Configuration → Manage Configurations**

- Click **+ New Configuration**
- Fill in the compiler path, arguments, run command, and expected output file path
- Click **Save Configuration**

**Example configurations:**

| Language | Compiler Path    | Args             | Run Command       |
| -------- | ---------------- | ---------------- | ----------------- |
| Python   | _(empty)_        | _(empty)_        | `python hello.py` |
| C        | `/usr/bin/gcc`   | `-o main main.c` | `./main`          |
| Java     | `/usr/bin/javac` | `Main.java`      | `java Main`       |

### Step 2 — Create a Project

Go to **File → New Project**

- Enter a project name
- Select a configuration from the **Active Configuration** dropdown

### Step 3 — Select ZIP Directory

- Click **Browse...** and select the folder containing student ZIP files
- ZIP files must be named with the student ID (e.g. `20210001.zip`)
- Each ZIP should contain the source file at its root (e.g. `hello.py`, `main.c`)

### Step 4 — Run Evaluation

- Click **Run Assignments**
- Results appear in the **Execution Reports** table
  - **✓ PASSED** — Output matched the expected output
  - **✗ FAILED** — Output did not match or an error occurred

---

## Project Structure

```
CE316Project/
├── src/main/java/org/example/ce316project/
│   ├── Launcher.java               # Application entry point
│   ├── IAEApplication.java         # JavaFX Application class
│   ├── MainController.java         # Main screen controller
│   ├── ConfigManagerController.java# Configuration manager controller
│   ├── AssignmentManager.java      # Central manager + SQLite integration
│   ├── Configuration.java          # Compile/run/compare logic
│   ├── Project.java                # ZIP processing + evaluation loop
│   └── StudentSubmission.java      # Student submission model
├── src/main/resources/
│   ├── main-view.fxml
│   └── config-manager.fxml
├── test_submissions/               # Sample test data
│   ├── 20210001.zip
│   └── expected.txt
├── installer.iss                   # Inno Setup installer script
└── pom.xml
```

---

## Technology Stack

| Technology               | Purpose                  |
| ------------------------ | ------------------------ |
| Java 21                  | Core language            |
| JavaFX 21                | GUI framework            |
| SQLite (via Xerial JDBC) | Persistent local storage |
| Jackson Databind         | JSON import/export       |
| Maven                    | Build tool               |
| Inno Setup               | Windows installer        |

---

## Building from Source

**Prerequisites:** Java 21, Maven 3.9+

```bash
git clone <repo-url>
cd CE316Project/CE316Project
mvn clean package
mvn javafx:run
```

To build the installer:

1. Install [Inno Setup 6](https://jrsoftware.org/isdl.php)
2. Open `installer.iss` in Inno Setup
3. Click **Build → Compile**
4. The installer will be generated in `installer_output/IAE_Setup.exe`

---

## Authors

CE316 Project Group
