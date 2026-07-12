package com.hakotjeria.util;

import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

/**
 * Ikon vektor internal aplikasi (SVGPath) — tanpa library ikon eksternal.
 */
public final class Icons {

    public static final String DASHBOARD = "M3 3h8v8H3V3zm10 0h8v5h-8V3zM3 13h8v8H3v-8zm10-2h8v10h-8V11z";
    public static final String DATABASE = "M12 2C7.6 2 4 3.6 4 5.5v13C4 20.4 7.6 22 12 22s8-1.6 8-3.5v-13"
            + "C20 3.6 16.4 2 12 2zm0 2c3.9 0 6 1.1 6 1.5S15.9 7 12 7 6 5.9 6 5.5 8.1 4 12 4zm6 14.5"
            + "c0 .4-2.1 1.5-6 1.5s-6-1.1-6-1.5v-3.2c1.5.7 3.6 1.2 6 1.2s4.5-.5 6-1.2v3.2zm0-5.5"
            + "c0 .4-2.1 1.5-6 1.5S6 13.4 6 13V9.8C7.5 10.5 9.6 11 12 11s4.5-.5 6-1.2V13z";
    public static final String CALENDAR = "M7 2v2H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6"
            + "a2 2 0 0 0-2-2h-2V2h-2v2H9V2H7zm12 8v10H5V10h14z";
    public static final String SWAP = "M7 7h11l-3-3 1.4-1.4L21.8 8l-5.4 5.4L15 12.6l3-3H7V7zm10 10H6"
            + "l3 3-1.4 1.4L2.2 16l5.4-5.4L9 12l-3 3h11v2z";
    public static final String BOX = "M4 4h16v4H4V4zm1 6h14v10H5V10zm4 3h6v2H9v-2z";
    public static final String HISTORY = "M13 3a9 9 0 1 0 8.95 10h-2.02A7 7 0 1 1 13 5v3l4-4-4-4v3z"
            + "m-1 5h1.5v4.6l3.8 2.2-.75 1.3-4.55-2.6V8z";
    public static final String USERS = "M16 11c1.66 0 3-1.34 3-3s-1.34-3-3-3-3 1.34-3 3 1.34 3 3 3zm-8 0"
            + "c1.66 0 3-1.34 3-3S9.66 5 8 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5"
            + "C15 14.17 10.33 13 8 13zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5"
            + "c0-2.33-4.67-3.5-7-3.5z";
    public static final String LOGOUT = "M16 13v-2H7V8l-5 4 5 4v-3h9zm3-10h-8v2h8v14h-8v2h8"
            + "a2 2 0 0 0 2-2V5a2 2 0 0 0-2-2z";
    public static final String BREAD = "M12 4C7 4 3 6.5 3 9.5c0 1.6 1.2 3 3 3.9V20h12v-6.6"
            + "c1.8-.9 3-2.3 3-3.9C21 6.5 17 4 12 4z";
    public static final String SEARCH = "M15.5 14h-.79l-.28-.27a6.5 6.5 0 1 0-.7.7l.27.28v.79l5 4.99"
            + "L20.49 19l-4.99-5zm-6 0A4.5 4.5 0 1 1 14 9.5 4.5 4.5 0 0 1 9.5 14z";
    public static final String PLUS = "M11 5h2v6h6v2h-6v6h-2v-6H5v-2h6V5z";
    public static final String EYE = "M12 5C7 5 2.7 8.1 1 12c1.7 3.9 6 7 11 7s9.3-3.1 11-7"
            + "c-1.7-3.9-6-7-11-7zm0 11.5A4.5 4.5 0 1 1 16.5 12 4.5 4.5 0 0 1 12 16.5zm0-7"
            + "A2.5 2.5 0 1 0 14.5 12 2.5 2.5 0 0 0 12 9.5z";
    public static final String EDIT = "M3 17.25V21h3.75L17.8 9.94l-3.75-3.75L3 17.25zM20.7 7.04"
            + "a1 1 0 0 0 0-1.41l-2.34-2.34a1 1 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z";
    public static final String TRASH = "M6 19a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2V7H6v12zM19 4h-3.5l-1-1h-5"
            + "l-1 1H5v2h14V4z";
    public static final String PDF = "M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-6-6z"
            + "m4 18H6V4h7v5h5v11zM8 12h8v2H8v-2zm0 4h8v2H8v-2z";
    public static final String LOCK = "M12 17a2 2 0 1 0-2-2 2 2 0 0 0 2 2zm6-9h-1V6a5 5 0 0 0-10 0v2H6"
            + "a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V10a2 2 0 0 0-2-2zM9 6a3 3 0 0 1 6 0v2H9V6z";
    public static final String PERSON = "M12 12a4 4 0 1 0-4-4 4 4 0 0 0 4 4zm0 2c-3.33 0-8 1.67-8 5v2h16"
            + "v-2c0-3.33-4.67-5-8-5z";
    public static final String WARNING = "M12 2 1 21h22L12 2zm1 14h-2v2h2v-2zm0-6h-2v4h2v-4z";
    public static final String CHECK = "M9 16.17 4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41L9 16.17z";

    private Icons() {
    }

    /** Membuat ikon SVG dengan warna tetap. */
    public static SVGPath of(String pathData, double size, Color color) {
        SVGPath svg = new SVGPath();
        svg.setContent(pathData);
        svg.setFill(color);
        double scale = size / 24.0;
        svg.setScaleX(scale);
        svg.setScaleY(scale);
        return svg;
    }

    /** Membuat ikon SVG yang mewarisi warna dari CSS melalui style class "nav-icon". */
    public static SVGPath nav(String pathData, double size) {
        SVGPath svg = of(pathData, size, Color.web("#475467"));
        svg.getStyleClass().add("nav-icon");
        return svg;
    }

    /** Membungkus ikon dalam kotak berukuran tetap agar rapi di dalam tombol. */
    public static StackPane boxed(String pathData, double size, Color color) {
        StackPane pane = new StackPane(of(pathData, size, color));
        pane.setMinSize(size, size);
        pane.setPrefSize(size, size);
        pane.setMaxSize(size, size);
        return pane;
    }
}
