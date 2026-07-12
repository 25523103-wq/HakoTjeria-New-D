package com.hakotjeria;

import com.hakotjeria.config.DatabaseConfig;
import com.hakotjeria.util.FxUtil;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.Parent;
import javafx.stage.Stage;

/**
 * Kelas launcher utama aplikasi Hako Tjeria.
 * Menginisialisasi skema basis data lalu menampilkan halaman Login.
 */
public class MainApp extends Application {

    public static final String APP_TITLE = "Hako Tjeria - Industrial Bakery Management";

    @Override
    public void init() {
        // Membuat database, tabel, foreign key, dan data default saat pertama dijalankan.
        DatabaseConfig.getInstance().initSchema();
    }

    @Override
    public void start(Stage stage) {
        Parent root = FxUtil.load("login.fxml");
        Scene scene = new Scene(root, 1280, 800);
        FxUtil.applyStylesheet(scene);
        stage.setTitle(APP_TITLE);
        Image icon = FxUtil.loadImage("icon.png");
        if (icon != null) {
            stage.getIcons().add(icon);
        }
        stage.setScene(scene);
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
