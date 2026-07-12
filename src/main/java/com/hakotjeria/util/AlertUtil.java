package com.hakotjeria.util;

import java.util.Optional;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;

/** Dialog notifikasi dan konfirmasi yang konsisten (ER14). */
public final class AlertUtil {

    private AlertUtil() {
    }

    public static void info(String judul, String pesan) {
        show(Alert.AlertType.INFORMATION, judul, pesan);
    }

    public static void error(String judul, String pesan) {
        show(Alert.AlertType.ERROR, judul, pesan);
    }

    public static void warn(String judul, String pesan) {
        show(Alert.AlertType.WARNING, judul, pesan);
    }

    public static boolean confirm(String judul, String pesan) {
        Alert alert = build(Alert.AlertType.CONFIRMATION, judul, pesan);
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.YES;
    }

    private static void show(Alert.AlertType type, String judul, String pesan) {
        build(type, judul, pesan).showAndWait();
    }

    private static Alert build(Alert.AlertType type, String judul, String pesan) {
        Alert alert = new Alert(type);
        alert.setTitle("Hako Tjeria");
        alert.setHeaderText(judul);
        Label label = new Label(pesan);
        label.setWrapText(true);
        label.setMaxWidth(420);
        alert.getDialogPane().setContent(label);
        String css = FxUtil.stylesheet();
        if (css != null) {
            alert.getDialogPane().getStylesheets().add(css);
        }
        return alert;
    }
}
