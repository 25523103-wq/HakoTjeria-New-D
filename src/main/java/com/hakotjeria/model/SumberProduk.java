package com.hakotjeria.model;

/** Sumber Produk Jadi: Internal (ber-BOM) atau Eksternal (tanpa BOM). */
public enum SumberProduk {
    INTERNAL("Internal"),
    EKSTERNAL("Eksternal");

    private final String label;

    SumberProduk(String label) {
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
