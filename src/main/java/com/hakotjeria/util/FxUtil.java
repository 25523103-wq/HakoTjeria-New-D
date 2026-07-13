package com.hakotjeria.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Optional;

import com.hakotjeria.MainApp;

import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

/** Utilitas pemuatan FXML, stylesheet, dan aset gambar. */
public final class FxUtil {

    private static final String FXML_BASE = "/com/hakotjeria/fxml/";
    private static final String CSS_PATH = "/com/hakotjeria/css/app.css";
    private static final String IMG_BASE = "/com/hakotjeria/";
    private static final String FONT_BASE = "/com/hakotjeria/fonts/";

    /** Berkas font kustom (Plus Jakarta Sans, Manrope, Inter) yang didaftarkan saat startup. */
    private static final String[] CUSTOM_FONTS = {
            "PlusJakartaSans-Regular.ttf", "PlusJakartaSans-Medium.ttf", "PlusJakartaSans-SemiBold.ttf",
            "PlusJakartaSans-Bold.ttf", "PlusJakartaSans-ExtraBold.ttf",
            "Manrope-Regular.ttf", "Manrope-Medium.ttf", "Manrope-SemiBold.ttf",
            "Manrope-Bold.ttf", "Manrope-ExtraBold.ttf",
            "Inter-Regular.ttf", "Inter-Medium.ttf", "Inter-SemiBold.ttf", "Inter-Bold.ttf",
    };

    /** Jumlah popup (Stage dialog / Alert / TextInputDialog) yang sedang terbuka. */
    private static int openDialogCount = 0;

    private FxUtil() {
    }

    /** Mendaftarkan seluruh berkas TTF kustom ke JavaFX; wajib dipanggil sekali sebelum Scene pertama dibuat. */
    public static void loadCustomFonts() {
        for (String fileName : CUSTOM_FONTS) {
            try (InputStream in = MainApp.class.getResourceAsStream(FONT_BASE + fileName)) {
                if (in != null) {
                    Font.loadFont(in, 12);
                }
            } catch (IOException e) {
                // Font gagal dimuat: biarkan CSS jatuh ke fallback berikutnya di font stack.
            }
        }
    }

    /** True selama ada popup yang terbuka; dipakai MainApp agar tidak memaksa maximize jendela utama. */
    public static boolean isDialogOpen() {
        return openDialogCount > 0;
    }

    /**
     * Menampilkan Stage modal: otomatis diikat (initOwner) ke jendela utama bila belum
     * punya owner, dan melacak status "dialog terbuka" agar jendela utama tidak keluar
     * fullscreen. Popup tanpa owner diperlakukan Windows sebagai window independen, yang
     * memicu jendela utama kehilangan mode fullscreen native saat popup itu fokus.
     */
    public static void showAndWait(Stage dialog) {
        if (dialog.getOwner() == null) {
            Window owner = resolveOwner();
            if (owner != null && owner != dialog) {
                dialog.initOwner(owner);
            }
        }
        openDialogCount++;
        try {
            dialog.showAndWait();
        } finally {
            openDialogCount--;
        }
    }

    /** Varian untuk Alert/TextInputDialog (semua turunan javafx.scene.control.Dialog). */
    public static <T> Optional<T> showAndWait(Dialog<T> dialog) {
        if (dialog.getOwner() == null) {
            Window owner = resolveOwner();
            if (owner != null) {
                dialog.initOwner(owner);
            }
        }
        openDialogCount++;
        try {
            return dialog.showAndWait();
        } finally {
            openDialogCount--;
        }
    }

    /** Jendela yang sedang fokus, atau jendela utama aplikasi bila tidak ada yang fokus. */
    private static Window resolveOwner() {
        Window focused = Window.getWindows().stream()
                .filter(Window::isFocused)
                .findFirst()
                .orElse(null);
        return focused != null ? focused : MainApp.getPrimaryStage();
    }

    /**
     * Memutar animasi fade-in pada root sebuah Scene yang baru saja dipasang ke Stage
     * (mis. transisi login&lt;-&gt;main), supaya pergantian konten terasa lebih halus
     * ketimbang muncul instan. Panggil setelah root diberi opacity 0 sebelum ditampilkan.
     */
    public static void fadeIn(Parent root) {
        FadeTransition ft = new FadeTransition(Duration.millis(320), root);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
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
