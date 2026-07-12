package com.hakotjeria.model;

/** Kategori tugas operasional. */
public enum KategoriTugas {
    PRODUKSI_INTERNAL("Produksi Internal"),
    PENGAMBILAN_EKSTERNAL("Pengambilan Eksternal");

    private final String label;

    KategoriTugas(String label) {
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
