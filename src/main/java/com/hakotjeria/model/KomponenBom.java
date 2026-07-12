package com.hakotjeria.model;

import java.math.BigDecimal;

/** Satu komponen Bahan Baku dalam BOM beserta kuantitas per unit produk. */
public class KomponenBom {

    private long id;
    private long bomId;
    private long bahanBakuId;
    private BigDecimal qty;

    // Atribut tampilan (diisi repository melalui join).
    private String namaBahan;
    private Satuan satuanBahan;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getBomId() {
        return bomId;
    }

    public void setBomId(long bomId) {
        this.bomId = bomId;
    }

    public long getBahanBakuId() {
        return bahanBakuId;
    }

    public void setBahanBakuId(long bahanBakuId) {
        this.bahanBakuId = bahanBakuId;
    }

    public BigDecimal getQty() {
        return qty;
    }

    public void setQty(BigDecimal qty) {
        this.qty = qty;
    }

    public BigDecimal getQtyPerUnit() {
        return qty;
    }

    public String getNamaBahan() {
        return namaBahan;
    }

    public void setNamaBahan(String namaBahan) {
        this.namaBahan = namaBahan;
    }

    public Satuan getSatuanBahan() {
        return satuanBahan;
    }

    public void setSatuanBahan(Satuan satuanBahan) {
        this.satuanBahan = satuanBahan;
    }
}
