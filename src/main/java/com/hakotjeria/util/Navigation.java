package com.hakotjeria.util;

import java.util.function.Consumer;

/** Jembatan navigasi antar halaman: diisi MainController, dipakai halaman anak. */
public final class Navigation {

    private static Consumer<String> handler;

    private Navigation() {
    }

    public static void setHandler(Consumer<String> navHandler) {
        handler = navHandler;
    }

    /** Berpindah ke halaman lain berdasarkan kunci menu (mis. "stock_opname"). */
    public static void goTo(String pageKey) {
        if (handler != null) {
            handler.accept(pageKey);
        }
    }
}
