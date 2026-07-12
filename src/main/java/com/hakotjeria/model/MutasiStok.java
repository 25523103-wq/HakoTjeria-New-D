package com.hakotjeria.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Satu baris mutasi stok (Read-Only setelah tersimpan). */
public class MutasiStok {

    public static final String KET_PENYESUAIAN = "Penyesuaian Stock Opname";

    private long id;
    private LocalDate tanggal;
    private long shiftId;
    private JenisInventaris jenisInventaris;
    private long barangId;
    private JenisMutasi jenis;
    private BigDecimal qty;
    private String keterangan;
    private Long createdBy;
    private LocalDateTime createdAt;

    // Atribut tampilan (join).
    private String namaBarang;
    private Satuan satuanBarang;
    private String namaShift;
    private String namaPenginput;

    public boolean isPenyesuaian() {
        return KET_PENYESUAIAN.equals(keterangan);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public LocalDate getTanggal() {
        return tanggal;
    }

    public void setTanggal(LocalDate tanggal) {
        this.tanggal = tanggal;
    }

    public long getShiftId() {
        return shiftId;
    }

    public void setShiftId(long shiftId) {
        this.shiftId = shiftId;
    }

    public JenisInventaris getJenisInventaris() {
        return jenisInventaris;
    }

    public void setJenisInventaris(JenisInventaris jenisInventaris) {
        this.jenisInventaris = jenisInventaris;
    }

    public long getBarangId() {
        return barangId;
    }

    public void setBarangId(long barangId) {
        this.barangId = barangId;
    }

    public JenisMutasi getJenis() {
        return jenis;
    }

    public void setJenis(JenisMutasi jenis) {
        this.jenis = jenis;
    }

    public BigDecimal getQty() {
        return qty;
    }

    public void setQty(BigDecimal qty) {
        this.qty = qty;
    }

    public String getKeterangan() {
        return keterangan;
    }

    public void setKeterangan(String keterangan) {
        this.keterangan = keterangan;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getNamaBarang() {
        return namaBarang;
    }

    public void setNamaBarang(String namaBarang) {
        this.namaBarang = namaBarang;
    }

    public Satuan getSatuanBarang() {
        return satuanBarang;
    }

    public void setSatuanBarang(Satuan satuanBarang) {
        this.satuanBarang = satuanBarang;
    }

    public String getNamaShift() {
        return namaShift;
    }

    public void setNamaShift(String namaShift) {
        this.namaShift = namaShift;
    }

    public String getNamaPenginput() {
        return namaPenginput;
    }

    public void setNamaPenginput(String namaPenginput) {
        this.namaPenginput = namaPenginput;
    }
}
