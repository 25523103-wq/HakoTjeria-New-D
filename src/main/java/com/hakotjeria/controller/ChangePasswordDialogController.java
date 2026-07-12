package com.hakotjeria.controller;

import com.hakotjeria.model.User;
import com.hakotjeria.service.UserService;
import com.hakotjeria.util.AlertUtil;
import com.hakotjeria.util.BusinessException;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

/** Dialog penggantian password yang dipaksakan setelah reset (UC-13). */
public class ChangePasswordDialogController {

    @FXML
    private PasswordField oldPasswordField;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Button saveButton;

    private final UserService userService = new UserService();
    private User user;
    private boolean berhasil;

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isBerhasil() {
        return berhasil;
    }

    @FXML
    private void onSimpan() {
        try {
            if (!newPasswordField.getText().equals(confirmPasswordField.getText())) {
                throw new BusinessException("Konfirmasi password tidak sama dengan password baru.");
            }
            userService.gantiPassword(user.getId(), oldPasswordField.getText(), newPasswordField.getText());
            berhasil = true;
            AlertUtil.info("Berhasil", "Password berhasil diganti.");
            close();
        } catch (BusinessException e) {
            AlertUtil.error("Gagal", e.getMessage());
        }
    }

    @FXML
    private void onBatal() {
        berhasil = false;
        close();
    }

    private void close() {
        ((Stage) saveButton.getScene().getWindow()).close();
    }
}
