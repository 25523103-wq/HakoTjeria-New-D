package com.hakotjeria;

import com.hakotjeria.config.DatabaseConfig;
import com.hakotjeria.util.FxUtil;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.Parent;
import javafx.stage.Stage;

/**
 * Kelas launcher utama aplikasi Hako Tjeria.
 * Menginisialisasi skema basis data lalu menampilkan halaman Login.
 */
public class MainApp extends Application {

    public static final String APP_TITLE = "Hako Tjeria";

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
        
        // --- 1. SETTING AWAL (FULLSCREEN MURNI SAAT START) ---
        stage.setFullScreen(true); 
        
        // --- 2. MEMASTIKAN SAAT KELUAR FULLSCREEN (VIA ESC) LANGSUNG MAXIMIZED ---
        // Listener ini akan mendeteksi kapan pun aplikasi keluar dari mode fullscreen
        stage.fullScreenProperty().addListener((observable, wasFullScreen, isNowFullScreen) -> {
            if (!isNowFullScreen) {
                // Saat fullscreen mati (misal karena tombol ESC ditekan), paksakan menjadi Maximized.
                // Ini akan memunculkan Taskbar dan tombol X, Minimize, Maximize di pojok kanan atas.
                stage.setMaximized(true);
            }
        });

        // --- 3. KEY MAPPING (F9, F10, F11) TETAP ADA ---
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.F11) {
                stage.setFullScreen(!stage.isFullScreen());
                event.consume();
            } 
            else if (event.getCode() == KeyCode.F10) {
                if (stage.isFullScreen()) {
                    stage.setFullScreen(false); 
                }
                stage.setMaximized(!stage.isMaximized());
                event.consume();
            } 
            else if (event.getCode() == KeyCode.F9) {
                stage.setIconified(true);
                event.consume();
            }
        });

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}