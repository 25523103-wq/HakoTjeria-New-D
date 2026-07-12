package com.hakotjeria.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.hakotjeria.model.JenisInventaris;
import com.hakotjeria.model.JenisMutasi;
import com.hakotjeria.model.MutasiFilter;
import com.hakotjeria.model.MutasiStok;
import com.hakotjeria.model.RingkasanMutasi;
import com.hakotjeria.model.StokItem;
import com.hakotjeria.repository.MutasiStokRepository;
import com.hakotjeria.repository.StockOpnameRepository;
import com.hakotjeria.util.BusinessException;
import com.hakotjeria.util.Formats;
import com.hakotjeria.util.Session;

/**
 * Aturan bisnis Pencatatan Mutasi Stok Manual (UC-08)
 * dan Riwayat Mutasi Stok (UC-09).
 */
public class MutasiStokService {

    private final MutasiStokRepository mutasiRepo = new MutasiStokRepository();
    private final StockOpnameRepository opnameRepo = new StockOpnameRepository();

    /**
     * Mencatat mutasi manual (penerimaan bahan / defect-waste).
     * Keterangan wajib diisi (R08.1, SR08); mutasi OUT tidak boleh melebihi
     * stok tersedia (R08.3); identitas penginput, tanggal, dan shift
     * direkam otomatis (R08.4).
     */
    public void catatMutasiManual(JenisInventaris jenisInventaris, Long barangId,
                                  JenisMutasi jenis, BigDecimal qty, String keterangan) {
        if (jenisInventaris == null) {
            throw new BusinessException("Kategori inventaris wajib dipilih.");
        }
        if (barangId == null) {
            throw new BusinessException("Nama Barang wajib dipilih.");
        }
        if (jenis == null) {
            throw new BusinessException("Jenis mutasi (IN/OUT) wajib dipilih (R08.1).");
        }
        if (qty == null || qty.signum() <= 0) {
            throw new BusinessException("Qty harus berupa angka lebih besar dari nol.");
        }
        if (keterangan == null || keterangan.isBlank()) {
            throw new BusinessException("Keterangan bersifat wajib isi (R08.1, SR08).");
        }

        LocalDate tanggal = LocalDate.now();
        long shiftId = Session.getActiveShift().getId();
        if (opnameRepo.isShiftTerkunci(tanggal, shiftId)) {
            throw new BusinessException("Shift aktif telah dikunci karena Stock Opname-nya sudah disetujui (R10.6).");
        }

        if (jenis == JenisMutasi.OUT) {
            BigDecimal stok = mutasiRepo.hitungStok(jenisInventaris, barangId);
            if (stok.compareTo(qty) < 0) {
                throw new BusinessException("Mutasi OUT melebihi stok tersedia (" + Formats.qty(stok)
                        + "). Kuantitas stok tidak boleh negatif (R08.3).");
            }
        }

        MutasiStok m = new MutasiStok();
        m.setTanggal(tanggal);
        m.setShiftId(shiftId);
        m.setJenisInventaris(jenisInventaris);
        m.setBarangId(barangId);
        m.setJenis(jenis);
        m.setQty(qty);
        m.setKeterangan(keterangan.trim());
        m.setCreatedBy(Session.getCurrentUser().getId());
        mutasiRepo.insert(m);
    }

    /** Riwayat mutasi terfilter, terurut dari kejadian teranyar (R09.1 - R09.4). */
    public List<MutasiStok> riwayat(MutasiFilter filter) {
        return mutasiRepo.findByFilter(filter);
    }

    /** Akumulasi Stok Awal, Total IN, Total OUT, Stok Akhir periode terfilter (R09.6). */
    public RingkasanMutasi ringkasan(MutasiFilter filter) {
        return mutasiRepo.ringkasan(filter);
    }

    public List<StokItem> stokSemua(JenisInventaris jenis) {
        return mutasiRepo.findStokSemua(jenis);
    }

    public BigDecimal stokBarang(JenisInventaris jenis, long barangId) {
        return mutasiRepo.hitungStok(jenis, barangId);
    }
}
