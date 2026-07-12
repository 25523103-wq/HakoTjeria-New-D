package com.hakotjeria.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.hakotjeria.model.BahanBaku;
import com.hakotjeria.model.JenisInventaris;
import com.hakotjeria.model.JenisMutasi;
import com.hakotjeria.model.MutasiStok;
import com.hakotjeria.model.ProdukJadi;
import com.hakotjeria.model.Satuan;
import com.hakotjeria.model.StokItem;
import com.hakotjeria.model.SumberProduk;
import com.hakotjeria.repository.BomRepository;
import com.hakotjeria.repository.JadwalTugasRepository;
import com.hakotjeria.repository.MasterDataRepository;
import com.hakotjeria.repository.MutasiStokRepository;
import com.hakotjeria.util.BusinessException;
import com.hakotjeria.util.Session;

/** Aturan bisnis Manajemen Master Data Dual-Inventory (UC-03). */
public class MasterDataService {

    public static final String KET_STOK_AWAL = "Stok Awal";

    private final MasterDataRepository masterRepo = new MasterDataRepository();
    private final MutasiStokRepository mutasiRepo = new MutasiStokRepository();
    private final BomRepository bomRepo = new BomRepository();
    private final JadwalTugasRepository tugasRepo = new JadwalTugasRepository();

    // ===================== QUERY =====================

    public List<StokItem> daftarStok(JenisInventaris jenis, String cari) {
        List<StokItem> semua = mutasiRepo.findStokSemua(jenis);
        if (cari == null || cari.isBlank()) {
            return semua;
        }
        String kunci = cari.trim().toLowerCase();
        return semua.stream()
                .filter(i -> i.getNama().toLowerCase().contains(kunci))
                .toList();
    }

    public List<BahanBaku> daftarBahanBaku() {
        return masterRepo.findBahanBaku(null);
    }

    public List<ProdukJadi> daftarProdukJadi() {
        return masterRepo.findProdukJadi(null);
    }

    public BahanBaku getBahanBaku(long id) {
        return masterRepo.findBahanBakuById(id)
                .orElseThrow(() -> new BusinessException("Bahan Baku tidak ditemukan."));
    }

    public ProdukJadi getProdukJadi(long id) {
        return masterRepo.findProdukJadiById(id)
                .orElseThrow(() -> new BusinessException("Produk Jadi tidak ditemukan."));
    }

    public boolean punyaRiwayatMutasi(JenisInventaris jenis, long barangId) {
        return mutasiRepo.adaMutasi(jenis, barangId);
    }

    // ===================== BAHAN BAKU =====================

    /**
     * Membuat Bahan Baku baru. Kuantitas Awal dicatat sebagai
     * mutasi IN berketerangan "Stok Awal" (R03.4).
     */
    public BahanBaku buatBahanBaku(String nama, Satuan satuan, BigDecimal batasMin, BigDecimal kuantitasAwal) {
        requireSupervisor();
        validasiUmum(nama, satuan, batasMin);
        if (kuantitasAwal == null || kuantitasAwal.signum() < 0) {
            throw new BusinessException("Kuantitas Awal tidak boleh negatif.");
        }
        if (masterRepo.existsNamaBahan(nama.trim(), null)) {
            throw new BusinessException("Nama Bahan Baku \"" + nama.trim() + "\" sudah terdaftar.");
        }
        BahanBaku b = new BahanBaku();
        b.setNama(nama.trim());
        b.setSatuan(satuan);
        b.setBatasMin(batasMin);
        long id = masterRepo.saveBahanBaku(b);
        b.setId(id);
        catatStokAwal(JenisInventaris.BAHAN_BAKU, id, kuantitasAwal);
        return b;
    }

    /**
     * Memperbarui Bahan Baku. Setelah barang memiliki riwayat mutasi,
     * satuan terkunci demi konsistensi kuantitas historis, dan kuantitas
     * hanya dapat berubah melalui mutasi (R03.4).
     */
    public void perbaruiBahanBaku(long id, String nama, Satuan satuan, BigDecimal batasMin) {
        requireSupervisor();
        validasiUmum(nama, satuan, batasMin);
        BahanBaku existing = getBahanBaku(id);
        if (masterRepo.existsNamaBahan(nama.trim(), id)) {
            throw new BusinessException("Nama Bahan Baku \"" + nama.trim() + "\" sudah terdaftar.");
        }
        boolean adaRiwayat = mutasiRepo.adaMutasi(JenisInventaris.BAHAN_BAKU, id);
        if (adaRiwayat && existing.getSatuan() != satuan) {
            throw new BusinessException("Satuan tidak dapat diubah karena barang telah memiliki riwayat mutasi.");
        }
        existing.setNama(nama.trim());
        existing.setSatuan(satuan);
        existing.setBatasMin(batasMin);
        masterRepo.updateBahanBaku(existing);
    }

    /** Penghapusan ditolak bila masih ada baris mutasi (R03.5) atau relasi BOM. */
    public void hapusBahanBaku(long id) {
        requireSupervisor();
        if (mutasiRepo.adaMutasi(JenisInventaris.BAHAN_BAKU, id)) {
            throw new BusinessException("Bahan Baku tidak dapat dihapus karena sudah memiliki riwayat mutasi (R03.5).");
        }
        if (masterRepo.bahanDipakaiBom(id)) {
            throw new BusinessException("Bahan Baku tidak dapat dihapus karena masih menjadi komponen BOM.");
        }
        masterRepo.deleteBahanBaku(id);
    }

    // ===================== PRODUK JADI =====================

    public ProdukJadi buatProdukJadi(String nama, Satuan satuan, SumberProduk sumber,
                                     BigDecimal batasMin, BigDecimal kuantitasAwal) {
        requireSupervisor();
        validasiUmum(nama, satuan, batasMin);
        if (sumber == null) {
            throw new BusinessException("Sumber Produk (Internal/Eksternal) wajib dipilih (R03.3).");
        }
        if (kuantitasAwal == null || kuantitasAwal.signum() < 0) {
            throw new BusinessException("Kuantitas Awal tidak boleh negatif.");
        }
        if (masterRepo.existsNamaProduk(nama.trim(), null)) {
            throw new BusinessException("Nama Produk Jadi \"" + nama.trim() + "\" sudah terdaftar.");
        }
        ProdukJadi p = new ProdukJadi();
        p.setNama(nama.trim());
        p.setSatuan(satuan);
        p.setSumber(sumber);
        p.setBatasMin(batasMin);
        long id = masterRepo.saveProdukJadi(p);
        p.setId(id);
        catatStokAwal(JenisInventaris.PRODUK_JADI, id, kuantitasAwal);
        return p;
    }

    public void perbaruiProdukJadi(long id, String nama, Satuan satuan, SumberProduk sumber, BigDecimal batasMin) {
        requireSupervisor();
        validasiUmum(nama, satuan, batasMin);
        if (sumber == null) {
            throw new BusinessException("Sumber Produk wajib dipilih (R03.3).");
        }
        ProdukJadi existing = getProdukJadi(id);
        if (masterRepo.existsNamaProduk(nama.trim(), id)) {
            throw new BusinessException("Nama Produk Jadi \"" + nama.trim() + "\" sudah terdaftar.");
        }
        boolean adaRiwayat = mutasiRepo.adaMutasi(JenisInventaris.PRODUK_JADI, id);
        if (adaRiwayat && existing.getSatuan() != satuan) {
            throw new BusinessException("Satuan tidak dapat diubah karena barang telah memiliki riwayat mutasi.");
        }
        if (existing.getSumber() != sumber && sumber == SumberProduk.EKSTERNAL
                && bomRepo.hasBomWithKomponen(id)) {
            throw new BusinessException("Produk masih memiliki BOM. Hapus BOM terlebih dahulu karena "
                    + "produk Eksternal tidak boleh memiliki relasi BOM (R04.2).");
        }
        existing.setNama(nama.trim());
        existing.setSatuan(satuan);
        existing.setSumber(sumber);
        existing.setBatasMin(batasMin);
        masterRepo.updateProdukJadi(existing);
    }

    public void hapusProdukJadi(long id) {
        requireSupervisor();
        if (mutasiRepo.adaMutasi(JenisInventaris.PRODUK_JADI, id)) {
            throw new BusinessException("Produk Jadi tidak dapat dihapus karena sudah memiliki riwayat mutasi (R03.5).");
        }
        if (tugasRepo.adaTugasUntukProduk(id)) {
            throw new BusinessException("Produk Jadi tidak dapat dihapus karena masih terikat jadwal tugas (R03.5).");
        }
        // BOM milik produk dihapus lebih dulu agar relasi tidak menggantung.
        bomRepo.deleteByProdukId(id);
        masterRepo.deleteProdukJadi(id);
    }

    // ===================== HELPER =====================

    private void catatStokAwal(JenisInventaris jenis, long barangId, BigDecimal kuantitasAwal) {
        if (kuantitasAwal.signum() <= 0) {
            return;
        }
        MutasiStok m = new MutasiStok();
        m.setTanggal(LocalDate.now());
        m.setShiftId(Session.getActiveShift().getId());
        m.setJenisInventaris(jenis);
        m.setBarangId(barangId);
        m.setJenis(JenisMutasi.IN);
        m.setQty(kuantitasAwal);
        m.setKeterangan(KET_STOK_AWAL);
        m.setCreatedBy(Session.getCurrentUser().getId());
        mutasiRepo.insert(m);
    }

    private void validasiUmum(String nama, Satuan satuan, BigDecimal batasMin) {
        if (nama == null || nama.isBlank()) {
            throw new BusinessException("Nama Barang wajib diisi.");
        }
        if (satuan == null) {
            throw new BusinessException("Satuan wajib dipilih dari Dropdown Satuan (R03.2).");
        }
        if (batasMin == null || batasMin.signum() < 0) {
            throw new BusinessException("Batas Minimum Stok Aman tidak boleh negatif.");
        }
    }

    private void requireSupervisor() {
        if (!Session.isSupervisor()) {
            throw new BusinessException("Staff hanya dapat melihat master data tanpa hak mengubah (R03.6).");
        }
    }
}
