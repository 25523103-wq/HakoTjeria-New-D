package com.hakotjeria.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Entitas Produk Jadi siap jual (hasil internal maupun eksternal). */
public class ProdukJadi {

    private long id;
    private String nama;
    private Satuan satuan;
    private SumberProduk sumber;
    private BigDecimal batasMin = BigDecimal.ZERO;
    private LocalDateTime createdAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public SumberProduk getSumber() {
        return sumber;
    }

    public void setSumber(SumberProduk sumber) {
        this.sumber = sumber;
    }

    public boolean isInternal() {
        return sumber == SumberProduk.INTERNAL;
    }

    public boolean isEksternal() {
        return sumber == SumberProduk.EKSTERNAL;
    }

    public BigDecimal getBatasMin() {
        return batasMin;
    }

    public void setBatasMin(BigDecimal batasMin) {
        this.batasMin = batasMin;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return nama;
    }
}
