package com.hakotjeria.model;

import java.math.BigDecimal;

/** Ringkasan stok terkini satu barang (hasil akumulasi mutasi). */
public class StokItem {

    private JenisInventaris jenisInventaris;
    private long barangId;
    private String nama;
    private Satuan satuan;
    private BigDecimal batasMin = BigDecimal.ZERO;
    private BigDecimal stok = BigDecimal.ZERO;
    private SumberProduk sumber; // hanya untuk Produk Jadi

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

    public String getNama() {
        return nama;
    }

    public void setNama(String nama) {
        this.nama = nama;
    }

    public Satuan getSatuan() {
        return satuan;
    }

    public void setSatuan(Satuan satuan) {
        this.satuan = satuan;
    }

    public BigDecimal getBatasMin() {
        return batasMin;
    }

    public void setBatasMin(BigDecimal batasMin) {
        this.batasMin = batasMin;
    }

    public BigDecimal getStok() {
        return stok;
    }

    public void setStok(BigDecimal stok) {
        this.stok = stok;
    }

    public SumberProduk getSumber() {
        return sumber;
    }

    public void setSumber(SumberProduk sumber) {
        this.sumber = sumber;
    }

    /** Stok berada di bawah batas aman minimum (R02.5). */
    public boolean isDiBawahBatasAman() {
        return stok.compareTo(batasMin) < 0;
    }
}
