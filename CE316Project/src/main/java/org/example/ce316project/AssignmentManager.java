package org.example.ce316project;

import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * AssignmentManager — merkezi yönetici sınıf.
 *
 * Sorumluluğu:
 *  - SQLite veritabanı bağlantısını kurmak ve tabloları oluşturmak
 *  - Configuration nesnelerini oluşturmak, kaydetmek, yüklemek
 *  - Project nesnelerini oluşturmak, kaydetmek, yüklemek
 *  - RAM içi listeleri veritabanıyla senkronize tutmak
 */
public class AssignmentManager {

    // ─────────────────────────────────────────────
    // Sabitler
    // ─────────────────────────────────────────────

    /** Veritabanı dosyasının yolu. Uygulama dizininde oluşturulur. */
    private static final String DB_URL = "jdbc:sqlite:assignment_manager.db";

    // ─────────────────────────────────────────────
    // Alanlar
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
    // Veritabanı Kurulumu
    // ─────────────────────────────────────────────

    /**
     * SQLite bağlantısını açar ve gerekli tabloları oluşturur.
     * Tablolar zaten varsa dokunulmaz (IF NOT EXISTS).
     */
    private void initDatabase() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            System.out.println("[DB] SQLite bağlantısı kuruldu: " + DB_URL);
            createTables();
        } catch (SQLException e) {
            System.out.println("[DB] Bağlantı hatası: " + e.getMessage());
        }
    }

    /**
     * Configurations ve Projects tablolarını oluşturur.
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
            System.out.println("[DB] Tablolar hazır.");
        }
    }

    // ─────────────────────────────────────────────
    // Configuration — Oluşturma & Kaydetme
    // ─────────────────────────────────────────────

    /**
     * Yeni bir Configuration oluşturur, veritabanına kaydeder ve listeye ekler.
     *
     * @param name       Konfigürasyon adı (benzersiz olmalı)
     * @param compiler   Derleyici yolu (ör: /usr/bin/gcc) — null olabilir
     * @param cArgs      Derleme argümanları (ör: -o main main.c) — null olabilir
     * @param execCmd    Çalıştırma komutu (ör: ./main) — zorunlu
     * @param output     Beklenen çıktı dosyası yolu — null olabilir
     * @return Oluşturulan Configuration nesnesi, hata durumunda null
     */
    public Configuration createConfiguration(String name, Path compiler,
                                             String cArgs, String execCmd, Path output) {
        if (name == null || name.isBlank()) {
            System.out.println("[Manager] Hata: Konfigürasyon adı boş olamaz.");
            return null;
        }
        if (execCmd == null || execCmd.isBlank()) {
            System.out.println("[Manager] Hata: Çalıştırma komutu boş olamaz.");
            return null;
        }

        // Aynı isimde var mı kontrolü
        if (findConfigurationByName(name) != null) {
            System.out.println("[Manager] Hata: '" + name + "' adında bir konfigürasyon zaten mevcut.");
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
            System.out.println("[Manager] Konfigürasyon kaydedildi: " + name);
        } catch (SQLException e) {
            System.out.println("[Manager] Konfigürasyon kaydedilemedi: " + e.getMessage());
            return null;
        }

        configurations.add(newConfig);
        return newConfig;
    }

    /**
     * Konfigürasyonu JSON/XML dosyasından içe aktarır.
     * TODO: JSON/XML parse implementasyonu eklenecek.
     *
     * @param filePath İçe aktarılacak dosyanın yolu
     */
    public void importConfiguration(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            System.out.println("[Manager] Hata: Dosya yolu boş olamaz.");
            return;
        }
        System.out.println("[Manager] İçe aktarma henüz implement edilmedi: " + filePath);
        // TODO: JSON/XML parse edip createConfiguration() çağır
    }

    // ─────────────────────────────────────────────
    // Project — Oluşturma & Kaydetme
    // ─────────────────────────────────────────────

    /**
     * Yeni bir Project oluşturur, veritabanına kaydeder ve listeye ekler.
     *
     * @param name   Proje adı (benzersiz olmalı)
     * @param config Bu projeye bağlı Configuration — null olamaz
     * @return Oluşturulan Project nesnesi, hata durumunda null
     */
    public Project createProject(String name, Configuration config) {
        if (name == null || name.isBlank()) {
            System.out.println("[Manager] Hata: Proje adı boş olamaz.");
            return null;
        }
        if (config == null) {
            System.out.println("[Manager] Hata: Geçerli bir konfigürasyon olmadan proje oluşturulamaz.");
            return null;
        }

        // Aynı isimde proje var mı kontrolü
        if (findProjectByName(name) != null) {
            System.out.println("[Manager] Hata: '" + name + "' adında bir proje zaten mevcut.");
            return null;
        }

        Project newProject = new Project(name, config);

        // Veritabanına kaydet
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
            System.out.println("[Manager] Proje veritabanına kaydedildi: " + name);
        } catch (SQLException e) {
            System.out.println("[Manager] Proje kaydedilemedi: " + e.getMessage());
            return null;
        }

        projects.add(newProject);
        return newProject;
    }

    /**
     * Var olan bir projenin ZIP dizinini veritabanında günceller.
     *
     * @param project       Güncellenecek proje
     * @param zipDirectory  Yeni ZIP dizini
     */
    public void updateProjectZipDirectory(Project project, Path zipDirectory) {
        if (project == null || zipDirectory == null) return;

        project.setSubmissionZIPsDirectory(zipDirectory);

        String sql = "UPDATE projects SET zip_directory = ? WHERE name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, zipDirectory.toString());
            pstmt.setString(2, project.getName());
            pstmt.executeUpdate();
            System.out.println("[Manager] Proje ZIP dizini güncellendi: " + project.getName());
        } catch (SQLException e) {
            System.out.println("[Manager] ZIP dizini güncellenemedi: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // Yükleme — Veritabanından RAM'e
    // ─────────────────────────────────────────────

    /**
     * Uygulama başlarken tüm kayıtları veritabanından yükler.
     * Önce konfigürasyonlar, sonra projeler yüklenir
     * (projeler konfigürasyona bağımlı olduğu için sıra önemli).
     */
    private void loadAllFromDatabase() {
        loadConfigurations();
        loadProjects();
    }

    /**
     * Tüm konfigürasyonları veritabanından okuyup listeye ekler.
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

            System.out.println("[DB] " + configurations.size() + " konfigürasyon yüklendi.");

        } catch (SQLException e) {
            System.out.println("[DB] Konfigürasyonlar yüklenemedi: " + e.getMessage());
        }
    }

    /**
     * Tüm projeleri veritabanından okuyup listeye ekler.
     * Her projenin konfigürasyonunu RAM listesinden bulur.
     */
    private void loadProjects() {
        String sql = "SELECT name, config_name, zip_directory FROM projects";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String name       = rs.getString("name");
                String configName = rs.getString("config_name");
                String zipDir     = rs.getString("zip_directory");

                // Konfigürasyonu RAM listesinden bul
                Configuration config = findConfigurationByName(configName);
                if (config == null) {
                    System.out.println("[DB] Uyarı: '" + name
                            + "' projesinin konfigürasyonu bulunamadı: " + configName);
                    continue;
                }

                Project project = new Project(name, config);
                if (zipDir != null) {
                    project.setSubmissionZIPsDirectory(Path.of(zipDir));
                }

                projects.add(project);
            }

            System.out.println("[DB] " + projects.size() + " proje yüklendi.");

        } catch (SQLException e) {
            System.out.println("[DB] Projeler yüklenemedi: " + e.getMessage());
        }
    }

    /**
     * Belirli bir projeyi dosya yolundan veya adından yükler.
     * Önce RAM listesinde arar, bulamazsa veritabanına bakar.
     *
     * @param nameOrPath Proje adı
     * @return Bulunan Project nesnesi, bulunamazsa null
     */
    public Project loadProject(String nameOrPath) {
        if (nameOrPath == null || nameOrPath.isBlank()) return null;

        // Önce RAM'de ara
        Project found = findProjectByName(nameOrPath);
        if (found != null) {
            System.out.println("[Manager] Proje RAM'den yüklendi: " + nameOrPath);
            return found;
        }

        // Veritabanında ara
        String sql = "SELECT name, config_name, zip_directory FROM projects WHERE name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nameOrPath);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String configName = rs.getString("config_name");
                String zipDir     = rs.getString("zip_directory");

                Configuration config = findConfigurationByName(configName);
                if (config == null) {
                    System.out.println("[Manager] Konfigürasyon bulunamadı: " + configName);
                    return null;
                }

                Project project = new Project(nameOrPath, config);
                if (zipDir != null) {
                    project.setSubmissionZIPsDirectory(Path.of(zipDir));
                }

                projects.add(project);
                System.out.println("[Manager] Proje veritabanından yüklendi: " + nameOrPath);
                return project;
            }

        } catch (SQLException e) {
            System.out.println("[Manager] Proje yüklenemedi: " + e.getMessage());
        }

        System.out.println("[Manager] Proje bulunamadı: " + nameOrPath);
        return null;
    }

    // ─────────────────────────────────────────────
    // Silme İşlemleri
    // ─────────────────────────────────────────────

    /**
     * Bir konfigürasyonu hem RAM listesinden hem de veritabanından siler.
     *
     * @param name Silinecek konfigürasyonun adı
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
                System.out.println("[Manager] Konfigürasyon silindi: " + name);
            } else {
                System.out.println("[Manager] Silinecek konfigürasyon bulunamadı: " + name);
            }
        } catch (SQLException e) {
            System.out.println("[Manager] Konfigürasyon silinemedi: " + e.getMessage());
        }
    }

    /**
     * Bir projeyi hem RAM listesinden hem de veritabanından siler.
     *
     * @param name Silinecek projenin adı
     */
    public void deleteProject(String name) {
        projects.removeIf(p -> p.getName().equals(name));

        String sql = "DELETE FROM projects WHERE name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                System.out.println("[Manager] Proje silindi: " + name);
            } else {
                System.out.println("[Manager] Silinecek proje bulunamadı: " + name);
            }
        } catch (SQLException e) {
            System.out.println("[Manager] Proje silinemedi: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // Arama Yardımcıları
    // ─────────────────────────────────────────────

    /**
     * RAM listesinde ada göre konfigürasyon arar.
     */
    public Configuration findConfigurationByName(String name) {
        return configurations.stream()
                .filter(c -> c.getConfigName().equals(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * RAM listesinde ada göre proje arar.
     */
    public Project findProjectByName(String name) {
        return projects.stream()
                .filter(p -> p.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    // ─────────────────────────────────────────────
    // Bağlantı Kapatma
    // ─────────────────────────────────────────────

    /**
     * Uygulama kapanırken veritabanı bağlantısını kapatır.
     * Bu metodu Stage.setOnCloseRequest() içinde çağırın.
     */
    public void closeDatabase() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DB] Bağlantı kapatıldı.");
            }
        } catch (SQLException e) {
            System.out.println("[DB] Bağlantı kapatılırken hata: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // Getter'lar
    // ─────────────────────────────────────────────

    public List<Project> getProjects() {
        return projects;
    }

    public List<Configuration> getConfigurations() {
        return configurations;
    }
}