package com.hakotjeria.model;

/** Status tugas operasional beserta sifat finalnya. */
public enum StatusTugas {
    BELUM_DIKERJAKAN("Belum Dikerjakan", false),
    SELESAI("Selesai", true),
    TIDAK_TERPENUHI("Tidak Terpenuhi", true),
    BELUM_DIAMBIL("Belum Diambil", false),
    SUDAH_DIAMBIL("Sudah Diambil", true);

    private final String label;
    private final boolean fin;

    StatusTugas(String label, boolean fin) {
        this.label = label;
        this.fin = fin;
    }

    public String getLabel() {
        return label;
    }

    /** Status final tidak dapat diubah kembali (R05.5, R06.8, R07.4). */
    public boolean isFinal() {
        return fin;
    }

    @Override
    public String toString() {
        return label;
    }
}
