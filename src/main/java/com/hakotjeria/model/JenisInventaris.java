package com.hakotjeria.model;

/** Dua kategori inventaris pada arsitektur Dual-Inventory. */
public enum JenisInventaris {
    BAHAN_BAKU("Bahan Baku"),
    PRODUK_JADI("Produk Jadi");

    private final String label;

    JenisInventaris(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
