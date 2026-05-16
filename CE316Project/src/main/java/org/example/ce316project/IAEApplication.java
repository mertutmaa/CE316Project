package org.example.ce316project;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class IAEApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {

        // ── FXML yükleme ─────────────────────────────────
        FXMLLoader fxmlLoader = new FXMLLoader(
                IAEApplication.class.getResource("main-view.fxml")
        );
        Scene scene = new Scene(fxmlLoader.load(), 1000, 700);

        // ── MainController'a Stage referansı ver ──────────
        // Böylece controller FileChooser, yeni pencere vb. için Stage'e erişebilir
        MainController controller = fxmlLoader.getController();

        // ── Pencere kapatılırken veritabanını düzgün kapat ─
        stage.setOnCloseRequest(event -> {
            if (controller != null) {
                controller.onApplicationClose();
            }
        });

        // ── Pencere ayarları ──────────────────────────────
        stage.setTitle("IAE — Integrated Assignment Evaluator");
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
    }
}