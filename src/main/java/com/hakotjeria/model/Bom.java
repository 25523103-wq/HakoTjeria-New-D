package com.hakotjeria.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Entitas BOM (resep digital) milik satu Produk Jadi internal. */
public class Bom {

    private long id;
    private long produkId;
    private LocalDateTime createdAt;
    private final List<KomponenBom> komponen = new ArrayList<>();

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getProdukId() {
        return produkId;
    }

    public void setProdukId(long produkId) {
        this.produkId = produkId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<KomponenBom> getKomponen() {
        return komponen;
    }

    public int getTotalKomponen() {
        return komponen.size();
    }
}
