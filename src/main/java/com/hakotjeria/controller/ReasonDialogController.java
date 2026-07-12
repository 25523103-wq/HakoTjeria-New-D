package com.hakotjeria.controller;

import com.hakotjeria.util.AlertUtil;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

/**
 * Dialog input alasan/catatan yang dapat dipakai ulang
 * (kegagalan tugas, penolakan opname, dsb.).
 */
public class ReasonDialogController {

    @FXML
    private Label judulLabel;
    @FXML
    private Label deskripsiLabel;
    @FXML
    private TextArea alasanArea;
    @FXML
    private Button simpanButton;

    private String hasil;

    public void init(String judul, String deskripsi, String labelTombol) {
        judulLabel.setText(judul);
        deskripsiLabel.setText(deskripsi);
        simpanButton.setText(labelTombol);
    }

    /** Mengembalikan teks alasan, atau null bila dibatalkan. */
    public String getHasil() {
        return hasil;
    }

    @FXML
    private void onSimpan() {
        String teks = alasanArea.getText();
        if (teks == null || teks.isBlank()) {
            AlertUtil.warn("Wajib Diisi", "Alasan/catatan tidak boleh kosong.");
            return;
        }
        hasil = teks.trim();
        close();
    }

    @FXML
    private void onBatal() {
        hasil = null;
        close();
    }

    private void close() {
        ((Stage) simpanButton.getScene().getWindow()).close();
    }
}
