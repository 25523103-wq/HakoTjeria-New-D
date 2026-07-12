package com.hakotjeria.model;

import java.math.BigDecimal;

/** Satu baris item pada dokumen Stock Opname. */
public class DetailStockOpname {

    private long id;
    private long opnameId;
    private JenisInventaris jenisInventaris;
    private long barangId;
    private BigDecimal stokSistem = BigDecimal.ZERO;
    private BigDecimal stokFisik = BigDecimal.ZERO;
    private BigDecimal selisih = BigDecimal.ZERO;

    // Atribut tampilan (join).
    private String namaBarang;
    private Satuan satuanBarang;

    /** Selisih = Stok Fisik - Stok Sistem. */
    public void hitungSelisih() {
        selisih = stokFisik.subtract(stokSistem);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getOpnameId() {
        return opnameId;
    }

    public void setOpnameId(long opnameId) {
        this.opnameId = opnameId;
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

    public BigDecimal getStokSistem() {
        return stokSistem;
    }

    public void setStokSistem(BigDecimal stokSistem) {
        this.stokSistem = stokSistem;
    }

    public BigDecimal getStokFisik() {
        return stokFisik;
    }

    public void setStokFisik(BigDecimal stokFisik) {
        this.stokFisik = stokFisik;
    }

    public BigDecimal getSelisih() {
        return selisih;
    }

    public void setSelisih(BigDecimal selisih) {
        this.selisih = selisih;
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
}
