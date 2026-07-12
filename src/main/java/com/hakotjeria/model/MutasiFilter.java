package com.hakotjeria.model;

import java.time.LocalDate;

/** Parameter penyaringan Riwayat Mutasi Stok (R09.3). */
public class MutasiFilter {

    private JenisInventaris jenisInventaris;
    private LocalDate dariTanggal;
    private LocalDate sampaiTanggal;
    private Long shiftId;
    private JenisMutasi jenis;
    private Long barangId;

    public JenisInventaris getJenisInventaris() {
        return jenisInventaris;
    }

    public void setJenisInventaris(JenisInventaris jenisInventaris) {
        this.jenisInventaris = jenisInventaris;
    }

    public LocalDate getDariTanggal() {
        return dariTanggal;
    }

    public void setDariTanggal(LocalDate dariTanggal) {
        this.dariTanggal = dariTanggal;
    }

    public LocalDate getSampaiTanggal() {
        return sampaiTanggal;
    }

    public void setSampaiTanggal(LocalDate sampaiTanggal) {
        this.sampaiTanggal = sampaiTanggal;
    }

    public Long getShiftId() {
        return shiftId;
    }

    public void setShiftId(Long shiftId) {
        this.shiftId = shiftId;
    }

    public JenisMutasi getJenis() {
        return jenis;
    }

    public void setJenis(JenisMutasi jenis) {
        this.jenis = jenis;
    }

    public Long getBarangId() {
        return barangId;
    }

    public void setBarangId(Long barangId) {
        this.barangId = barangId;
    }
}
