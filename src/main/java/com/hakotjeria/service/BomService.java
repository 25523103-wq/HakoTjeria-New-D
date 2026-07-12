package com.hakotjeria.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.hakotjeria.model.Bom;
import com.hakotjeria.model.KomponenBom;
import com.hakotjeria.model.ProdukJadi;
import com.hakotjeria.repository.BomRepository;
import com.hakotjeria.repository.JadwalTugasRepository;
import com.hakotjeria.repository.MasterDataRepository;
import com.hakotjeria.util.BusinessException;
import com.hakotjeria.util.Session;

/** Aturan bisnis Manajemen BOM / resep digital (UC-04). */
public class BomService {

    private final BomRepository bomRepo = new BomRepository();
    private final MasterDataRepository masterRepo = new MasterDataRepository();
    private final JadwalTugasRepository tugasRepo = new JadwalTugasRepository();

    public Optional<Bom> getBom(long produkId) {
        return bomRepo.findByProdukId(produkId);
    }

    /**
     * Menyimpan resep BOM sebuah Produk Jadi Internal
     * (R04.1, R04.2, R04.3, R04.5).
     */
    public void simpanBom(long produkId, List<KomponenBom> komponen) {
        requireSupervisor();
        ProdukJadi produk = masterRepo.findProdukJadiById(produkId)
                .orElseThrow(() -> new BusinessException("Produk Jadi tidak ditemukan."));
        if (produk.isEksternal()) {
            throw new BusinessException("Produk eksternal diterima dalam wujud jadi sehingga tidak memerlukan "
                    + "resep BOM (R04.2).");
        }
        if (komponen == null || komponen.isEmpty()) {
            throw new BusinessException("BOM harus memiliki minimal satu komponen Bahan Baku (R04.1).");
        }
        Set<Long> unik = new HashSet<>();
        for (KomponenBom k : komponen) {
            if (k.getQty() == null || k.getQty().signum() <= 0) {
                throw new BusinessException("Kuantitas komponen tidak boleh nol atau negatif (R04.5).");
            }
            if (!unik.add(k.getBahanBakuId())) {
                throw new BusinessException("Terdapat Bahan Baku ganda pada daftar komponen.");
            }
        }
        bomRepo.save(produkId, komponen);
    }

    /** Penghapusan BOM terkunci selama produk terdaftar pada jadwal tugas aktif (R04.4). */
    public void hapusBom(long produkId) {
        requireSupervisor();
        if (tugasRepo.adaTugasAktifUntukProduk(produkId)) {
            throw new BusinessException("BOM tidak dapat dihapus karena produk masih terdaftar pada "
                    + "jadwal tugas aktif (R04.4).");
        }
        bomRepo.deleteByProdukId(produkId);
    }

    private void requireSupervisor() {
        if (!Session.isSupervisor()) {
            throw new BusinessException("Staff hanya dapat melihat resep BOM tanpa hak mengubah (R04.6).");
        }
    }
}
