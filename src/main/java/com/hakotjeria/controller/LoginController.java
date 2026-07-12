package com.hakotjeria.controller;

import com.hakotjeria.MainApp;
import com.hakotjeria.model.User;
import com.hakotjeria.service.UserService;
import com.hakotjeria.util.AlertUtil;
import com.hakotjeria.util.BusinessException;
import com.hakotjeria.util.FxUtil;
import com.hakotjeria.util.Icons;
import com.hakotjeria.util.Session;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

/** Controller halaman Login (UC-01) dan opsi Lupa Password (UC-13). */
public class LoginController {

    @FXML
    private StackPane logoBox;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private CheckBox rememberMeCheck;
    @FXML
    private Button loginButton;

    private final UserService userService = new UserService();

    @FXML
    private void initialize() {
        logoBox.getChildren().add(Icons.of(Icons.BREAD, 24, Color.web("#9DB2F0")));
    }

    @FXML
    private void onLogin() {
        try {
            User user = userService.login(usernameField.getText(), passwordField.getText());
            if (user.isMustChangePassword()) {
                boolean diganti = bukaDialogGantiPassword(user);
                if (!diganti) {
                    userService.logout();
                    return;
                }
            }
            bukaHalamanUtama();
        } catch (BusinessException e) {
            AlertUtil.error("Login Gagal", e.getMessage());
            passwordField.clear();
        }
    }

    /** Menampilkan informasi kontak tim pengelola sistem (R13.5). */
    @FXML
    private void onForgotPassword() {
        AlertUtil.info("Lupa Password",
                """
                Reset password hanya dapat dilakukan oleh Tim Pengelola Sistem (Admin IT).

                Silakan hubungi:
                IT Support Hako Tjeria
                Telepon/WA : 0822-8126-5130 
                Email      : dzonncoc@gmail.com

                Setelah identitas terverifikasi, Admin akan mereset password dan
                membuka akun yang dibekukan. Anda wajib mengganti password baru
                pada login pertama.""");
    }

/** Pemaksaan penggantian sandi pada kesempatan pertama setelah reset (UC-13). */
    private boolean bukaDialogGantiPassword(User user) {
        FXMLLoader loader = FxUtil.loader("change_password_dialog.fxml");
        ChangePasswordDialogController controller = loader.getController();
        controller.setUser(user);

        Stage dialog = new Stage();
        dialog.setTitle("Ganti Password - Hako Tjeria");
        
        // --- PERBAIKAN POP-UP: IKAT KE JENDELA UTAMA ---
        Stage ownerStage = (Stage) loginButton.getScene().getWindow();
        dialog.initOwner(ownerStage);
        // -----------------------------------------------

        dialog.initModality(Modality.APPLICATION_MODAL);
        Scene scene = new Scene(loader.getRoot());
        FxUtil.applyStylesheet(scene);
        dialog.setScene(scene);
        dialog.showAndWait();
        return controller.isBerhasil();
    }

    private void bukaHalamanUtama() {
        Parent root = FxUtil.load("main.fxml");
        Stage stage = (Stage) loginButton.getScene().getWindow();
        
        // --- PERBAIKAN FULLSCREEN: SIMPAN STATUS SAAT INI ---
        boolean isFullscreen = stage.isFullScreen();
        boolean isMaximized = stage.isMaximized();
        // ----------------------------------------------------

        Scene scene = new Scene(root, Math.max(1280, stage.getWidth()), Math.max(800, stage.getHeight()));
        FxUtil.applyStylesheet(scene);
        stage.setScene(scene);
        stage.setTitle(MainApp.APP_TITLE + " - " + Session.getCurrentUser().getNamaLengkap()
                + " (" + Session.getCurrentUser().getRole().getLabel() + ")");
        
        // --- KEMBALIKAN STATUS JENDELA SETELAH SCENE DIGANTI ---
        if (isFullscreen) {
            stage.setFullScreen(true);
        } else if (isMaximized) {
            stage.setMaximized(true);
        } else {
            stage.centerOnScreen();
        }
        // -------------------------------------------------------
    }
}