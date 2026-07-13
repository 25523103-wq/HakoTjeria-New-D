package com.hakotjeria;

import com.hakotjeria.config.DatabaseConfig;
import com.hakotjeria.util.FxUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.Parent;
import javafx.stage.Stage;

public class MainApp extends Application {

    public static final String APP_TITLE = "Hako Tjeria";

    private static Stage primaryStage;

    /** Jendela utama aplikasi; dipakai FxUtil untuk mengikat (initOwner) semua popup ke sini. */
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    @Override
    public void init() {
        // Membuat database, tabel, foreign key, dan data default saat pertama dijalankan.
        DatabaseConfig.getInstance().initSchema();
    }

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        FxUtil.loadCustomFonts();
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
        
            stage.fullScreenProperty().addListener((observable, wasFullScreen, isNowFullScreen) -> {
            if (!isNowFullScreen) {
                // Ditunda: jika dipanggil sinkron di sini, transisi native keluar-fullscreen
                // (khususnya via ESC di Windows) belum selesai, sehingga setFullScreen(true)
                // berikutnya (mis. dari F11) bisa diabaikan oleh window native.
                Platform.runLater(() -> {
                    // Fullscreen bisa "batal" secara native gara-gara popup (dialog/alert) merebut
                    // fokus, bukan karena user menekan F11/ESC. Saat itu terjadi, biarkan saja
                    // (jangan maximize) supaya jendela utama tidak berubah tampilan di belakang popup.
                    if (!FxUtil.isDialogOpen()) {
                        stage.setMaximized(true);
                    }
                });
            }
        });

        // --- 3. KEY MAPPING (F9, F10, F11) TETAP ADA ---
        // Dipasang di Stage (bukan Scene) agar tetap berfungsi walau Scene diganti
        // saat login (login.fxml -> main.fxml), karena Stage selalu berada di awal
        // event dispatch chain untuk Scene manapun yang sedang aktif.
        stage.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.F11) {
                boolean goFullScreen = !stage.isFullScreen();
                Platform.runLater(() -> stage.setFullScreen(goFullScreen));
                event.consume();
            }
            else if (event.getCode() == KeyCode.F10) {
                boolean wasFullScreen = stage.isFullScreen();
                if (wasFullScreen) {
                    stage.setFullScreen(false);
                }
                boolean toggleMaximized = wasFullScreen || !stage.isMaximized();
                Platform.runLater(() -> stage.setMaximized(toggleMaximized));
                event.consume();
            }
            else if (event.getCode() == KeyCode.F9) {
                stage.setIconified(true);
                event.consume();
            }
        });

        stage.show();
        stage.setFullScreen(true); 
        stage.setFullScreenExitHint("");
    }
        
    public static void main(String[] args) {
        launch(args);
    }
}