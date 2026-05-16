package org.example.ce316project;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class IAEApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {

        // ── FXML Loading ─────────────────────────────────
        FXMLLoader fxmlLoader = new FXMLLoader(
                IAEApplication.class.getResource("main-view.fxml")
        );
        Scene scene = new Scene(fxmlLoader.load(), 1000, 700);

        // ── Provide Stage reference to MainController ────
        // This allows the controller to access the Stage for FileChooser, new windows, etc.
        MainController controller = fxmlLoader.getController();

        // ── Properly close the database when the window is closed ─
        stage.setOnCloseRequest(event -> {
            if (controller != null) {
                controller.onApplicationClose();
            }
        });

        // ── Window Settings ──────────────────────────────
        stage.setTitle("IAE — Integrated Assignment Evaluator");
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
    }
}