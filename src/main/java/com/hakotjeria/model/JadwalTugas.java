package com.hakotjeria.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Entitas tugas operasional harian (produksi internal / pengambilan eksternal). */
public class JadwalTugas {

    private long id;
    private LocalDate tanggal;
    private long shiftId;
    private KategoriTugas kategori;
    private long produkId;
    private BigDecimal qtyTarget;
    private BigDecimal qtyAktual;
    private BigDecimal qtyDiterima;
    private StatusTugas status;
    private Long staffId;
    private String catatan;
    private LocalDateTime createdAt;

    // Atribut tampilan (join).
    private String namaProduk;
    private Satuan satuanProduk;
    private SumberProduk sumberProduk;
    private String namaShift;
    private String namaStaff;

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

    public KategoriTugas getKategori() {
        return kategori;
    }

    public void setKategori(KategoriTugas kategori) {
        this.kategori = kategori;
    }

    public long getProdukId() {
        return produkId;
    }

    public void setProdukId(long produkId) {
        this.produkId = produkId;
    }

    public BigDecimal getQtyTarget() {
        return qtyTarget;
    }

    public void setQtyTarget(BigDecimal qtyTarget) {
        this.qtyTarget = qtyTarget;
    }

    public BigDecimal getQtyAktual() {
        return qtyAktual;
    }

    public void setQtyAktual(BigDecimal qtyAktual) {
        this.qtyAktual = qtyAktual;
    }

    public BigDecimal getQtyDiterima() {
        return qtyDiterima;
    }

    public void setQtyDiterima(BigDecimal qtyDiterima) {
        this.qtyDiterima = qtyDiterima;
    }

    public StatusTugas getStatus() {
        return status;
    }

    public void setStatus(StatusTugas status) {
        this.status = status;
    }

    public Long getStaffId() {
        return staffId;
    }

    public void setStaffId(Long staffId) {
        this.staffId = staffId;
    }

    public String getCatatan() {
        return catatan;
    }

    public void setCatatan(String catatan) {
        this.catatan = catatan;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getNamaProduk() {
        return namaProduk;
    }

    public void setNamaProduk(String namaProduk) {
        this.namaProduk = namaProduk;
    }

    public Satuan getSatuanProduk() {
        return satuanProduk;
    }

    public void setSatuanProduk(Satuan satuanProduk) {
        this.satuanProduk = satuanProduk;
    }

    public SumberProduk getSumberProduk() {
        return sumberProduk;
    }

    public void setSumberProduk(SumberProduk sumberProduk) {
        this.sumberProduk = sumberProduk;
    }

    public String getNamaShift() {
        return namaShift;
    }

    public void setNamaShift(String namaShift) {
        this.namaShift = namaShift;
    }

    public String getNamaStaff() {
        return namaStaff;
    }

    public void setNamaStaff(String namaStaff) {
        this.namaStaff = namaStaff;
    }
}
