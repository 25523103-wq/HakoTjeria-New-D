package com.hakotjeria.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import com.hakotjeria.MainApp;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;

/** Utilitas pemuatan FXML, stylesheet, dan aset gambar. */
public final class FxUtil {

    private static final String FXML_BASE = "/com/hakotjeria/fxml/";
    private static final String CSS_PATH = "/com/hakotjeria/css/app.css";
    private static final String IMG_BASE = "/com/hakotjeria/";

    private FxUtil() {
    }

    public static Parent load(String fxmlName) {
        return loader(fxmlName).getRoot();
    }

    /** Memuat FXML dan mengembalikan loader agar controller dapat diakses. */
    public static FXMLLoader loader(String fxmlName) {
        URL url = MainApp.class.getResource(FXML_BASE + fxmlName);
        if (url == null) {
            throw new IllegalStateException("FXML tidak ditemukan: " + fxmlName);
        }
        try {
            FXMLLoader loader = new FXMLLoader(url);
            loader.load();
            return loader;
        } catch (IOException e) {
            throw new IllegalStateException("Gagal memuat FXML " + fxmlName + ": " + e.getMessage(), e);
        }
    }

    public static void applyStylesheet(Scene scene) {
        URL css = MainApp.class.getResource(CSS_PATH);
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
    }

    public static String stylesheet() {
        URL css = MainApp.class.getResource(CSS_PATH);
        return css == null ? null : css.toExternalForm();
    }

    /** Memuat gambar dari resources; mengembalikan null bila tidak tersedia (placeholder-friendly). */
    public static Image loadImage(String name) {
        InputStream in = MainApp.class.getResourceAsStream(IMG_BASE + name);
        return in == null ? null : new Image(in);
    }
}
