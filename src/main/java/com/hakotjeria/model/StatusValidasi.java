package com.hakotjeria.model;

/**
 * Status dokumen Stock Opname.
 * DRAF_REVISI dipakai untuk draf yang masih dapat diedit Staff
 * (draf awal maupun draf hasil penolakan Supervisor).
 */
public enum StatusValidasi {
    DRAF_REVISI("Draf Revisi"),
    MENUNGGU_VALIDASI("Menunggu Validasi"),
    TERVALIDASI("Tervalidasi");

    private final String label;

    StatusValidasi(String label) {
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
