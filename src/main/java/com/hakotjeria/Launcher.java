package com.hakotjeria;

/**
 * Launcher non-modular agar aplikasi dapat dijalankan langsung
 * (java -cp ... com.hakotjeria.Launcher) tanpa konfigurasi module-path,
 * termasuk dari IntelliJ IDEA, VS Code, maupun NetBeans.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        MainApp.main(args);
    }
}
