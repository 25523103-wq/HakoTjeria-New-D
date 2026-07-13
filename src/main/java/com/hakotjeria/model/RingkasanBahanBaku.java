package com.hakotjeria.model;

import java.math.BigDecimal;

/**
 * Akumulasi Stok Awal, Total IN, Total OUT, dan Stok Akhir per bahan baku
 * pada periode terfilter. Dipisah per barang (bukan dijumlahkan lintas barang)
 * karena tiap bahan baku bisa memiliki satuan yang berbeda-beda (kg, liter, pcs, dst).
 */
public class RingkasanBahanBaku {

    private final long barangId;
    private final String nama;
    private final Satuan satuan;
    private final BigDecimal stokAwal;
    private final BigDecimal totalIn;
    private final BigDecimal totalOut;

    public RingkasanBahanBaku(long barangId, String nama, Satuan satuan, BigDecimal stokAwal,
                              BigDecimal totalIn, BigDecimal totalOut) {
        this.barangId = barangId;
        this.nama = nama;
        this.satuan = satuan;
        this.stokAwal = stokAwal;
        this.totalIn = totalIn;
        this.totalOut = totalOut;
    }

    public long getBarangId() {
        return barangId;
    }

    public String getNama() {
        return nama;
    }

    public Satuan getSatuan() {
        return satuan;
    }

    public BigDecimal getStokAwal() {
        return stokAwal;
    }

    public BigDecimal getTotalIn() {
        return totalIn;
    }

    public BigDecimal getTotalOut() {
        return totalOut;
    }

    public BigDecimal getStokAkhir() {
        return stokAwal.add(totalIn).subtract(totalOut);
    }
}
