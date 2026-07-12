package com.hakotjeria.model;

import java.math.BigDecimal;

/**
 * Akumulasi Stok Awal, Total IN, Total OUT, dan Stok Akhir
 * untuk periode terfilter (R09.6).
 */
public class RingkasanMutasi {

    private final BigDecimal stokAwal;
    private final BigDecimal totalIn;
    private final BigDecimal totalOut;

    public RingkasanMutasi(BigDecimal stokAwal, BigDecimal totalIn, BigDecimal totalOut) {
        this.stokAwal = stokAwal;
        this.totalIn = totalIn;
        this.totalOut = totalOut;
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
