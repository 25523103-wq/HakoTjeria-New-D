package com.hakotjeria.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import com.hakotjeria.config.DatabaseConfig;
import com.hakotjeria.model.DetailStockOpname;
import com.hakotjeria.model.JenisInventaris;
import com.hakotjeria.model.JenisMutasi;
import com.hakotjeria.model.MutasiStok;
import com.hakotjeria.model.Shift;
import com.hakotjeria.model.StatusValidasi;
import com.hakotjeria.model.StockOpname;
import com.hakotjeria.model.StokItem;
import com.hakotjeria.repository.MutasiStokRepository;
import com.hakotjeria.repository.RepositoryException;
import com.hakotjeria.repository.ShiftRepository;
import com.hakotjeria.repository.StockOpnameRepository;
import com.hakotjeria.util.BusinessException;
import com.hakotjeria.util.Session;

/**
 * Aturan bisnis Stock Opname (UC-10) dan Riwayat Stock Opname (UC-11):
 * penyusunan draf, pengajuan, validasi Supervisor, mutasi penyesuaian,
 * serta penguncian shift.
 */
public class StockOpnameService {

    private static final DateTimeFormatter KODE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final StockOpnameRepository opnameRepo = new StockOpnameRepository();
    private final MutasiStokRepository mutasiRepo = new MutasiStokRepository();
    private final ShiftRepository shiftRepo = new ShiftRepository();

    /**
     * Memuat dokumen opname untuk tanggal + shift tertentu.
     * Bila belum ada, disusun draf baru berisi seluruh barang Dual-Inventory
     * dengan Stok Sistem terkini (R10.1). Bila draf revisi telah ada,
     * Stok Sistem disegarkan tanpa menghilangkan isian Stok Fisik.
     */
    public StockOpname muatDokumen(LocalDate tanggal, long shiftId) {
        Optional<StockOpname> existing = opnameRepo.findByTanggalShift(tanggal, shiftId);
        if (existing.isPresent()) {
            StockOpname doc = existing.get();
            if (doc.getStatus() == StatusValidasi.DRAF_REVISI) {
                segarkanStokSistem(doc);
            }
            return doc;
        }
        StockOpname doc = new StockOpname();
        doc.setTanggal(tanggal);
        doc.setShiftId(shiftId);
        doc.setStatus(StatusValidasi.DRAF_REVISI);
        isiDetailBaru(doc);
        return doc;
    }

    /** Draf dapat diedit hanya bila berstatus Draf Revisi (R10.4). */
    public boolean bisaDiedit(StockOpname doc) {
        return doc.getStatus() == StatusValidasi.DRAF_REVISI;
    }

    /** Menyimpan draf tanpa mengajukan validasi (tombol "Simpan Draft"). */
    public StockOpname simpanDraft(StockOpname doc) {
        pastikanBolehDisimpan(doc);
        hitungUlangSelisih(doc);
        if (doc.getId() == 0) {
            doc.setKode(generateKode(doc));
            doc.setInputBy(Session.getCurrentUser().getId());
            doc.setInputAt(LocalDateTime.now());
            long id = opnameRepo.insertHeader(doc);
            doc.setId(id);
        }
        opnameRepo.replaceDetail(doc.getId(), doc.getDetail());
        return doc;
    }

    /**
     * Mengirim ajuan audit: seluruh kolom Stok Fisik wajib terisi angka >= 0,
     * status menjadi "Menunggu Validasi", angka terkunci (R10.4, R10.9).
     */
    public StockOpname kirimAjuan(StockOpname doc) {
        pastikanBolehDisimpan(doc);
        for (DetailStockOpname d : doc.getDetail()) {
            if (d.getStokFisik() == null || d.getStokFisik().signum() < 0) {
                throw new BusinessException("Seluruh kolom Stok Fisik wajib terisi angka nol atau lebih "
                        + "sebelum ajuan dikirim (" + d.getNamaBarang() + ").");
            }
        }
        simpanDraft(doc);
        doc.setInputBy(Session.getCurrentUser().getId());
        doc.setInputAt(LocalDateTime.now());
        opnameRepo.updateStatusPengajuan(doc.getId(), StatusValidasi.MENUNGGU_VALIDASI,
                doc.getInputBy(), doc.getInputAt());
        doc.setStatus(StatusValidasi.MENUNGGU_VALIDASI);
        return doc;
    }

    /** Daftar ajuan yang menunggu validasi Supervisor. */
    public List<StockOpname> daftarMenungguValidasi() {
        return opnameRepo.findByStatus(StatusValidasi.MENUNGGU_VALIDASI);
    }

    /**
     * Persetujuan Supervisor (R10.3, R10.5 - R10.8): membuat mutasi penyesuaian
     * IN/OUT sebesar selisih dengan keterangan "Penyesuaian Stock Opname"
     * dalam satu transaksi, lalu mengunci shift secara permanen.
     */
    public void setujui(long opnameId) {
        requireSupervisor();
        StockOpname doc = opnameRepo.findById(opnameId)
                .orElseThrow(() -> new BusinessException("Dokumen Stock Opname tidak ditemukan."));
        if (doc.getStatus() != StatusValidasi.MENUNGGU_VALIDASI) {
            throw new BusinessException("Hanya dokumen berstatus \"Menunggu Validasi\" yang dapat disetujui.");
        }
        try (Connection con = DatabaseConfig.getInstance().getConnection()) {
            con.setAutoCommit(false);
            try {
                for (DetailStockOpname d : doc.getDetail()) {
                    if (d.getSelisih().signum() == 0) {
                        continue;
                    }
                    MutasiStok m = new MutasiStok();
                    m.setTanggal(doc.getTanggal());
                    m.setShiftId(doc.getShiftId());
                    m.setJenisInventaris(d.getJenisInventaris());
                    m.setBarangId(d.getBarangId());
                    m.setJenis(d.getSelisih().signum() > 0 ? JenisMutasi.IN : JenisMutasi.OUT);
                    m.setQty(d.getSelisih().abs());
                    m.setKeterangan(MutasiStok.KET_PENYESUAIAN);
                    m.setCreatedBy(Session.getCurrentUser().getId());
                    mutasiRepo.insert(con, m);
                }
                opnameRepo.updateStatusValidasi(con, opnameId, StatusValidasi.TERVALIDASI,
                        Session.getCurrentUser().getId(), LocalDateTime.now(), null);
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RepositoryException("Transaksi persetujuan opname gagal dan dibatalkan", e);
        }
    }

    /**
     * Penolakan Supervisor: status kembali "Draf Revisi" disertai catatan,
     * hak input Staff terbuka kembali (alur alternatif UC-10).
     */
    public void tolak(long opnameId, String catatan) {
        requireSupervisor();
        StockOpname doc = opnameRepo.findById(opnameId)
                .orElseThrow(() -> new BusinessException("Dokumen Stock Opname tidak ditemukan."));
        if (doc.getStatus() != StatusValidasi.MENUNGGU_VALIDASI) {
            throw new BusinessException("Hanya dokumen berstatus \"Menunggu Validasi\" yang dapat ditolak.");
        }
        if (catatan == null || catatan.isBlank()) {
            throw new BusinessException("Catatan penolakan wajib diisi.");
        }
        opnameRepo.updateStatusValidasi(opnameId, StatusValidasi.DRAF_REVISI,
                Session.getCurrentUser().getId(), LocalDateTime.now(), catatan.trim());
    }

    /** Riwayat dokumen tervalidasi dengan filter tanggal dan shift (R11.1 - R11.3). */
    public List<StockOpname> riwayat(LocalDate dari, LocalDate sampai, Long shiftId) {
        return opnameRepo.findRiwayat(dari, sampai, shiftId);
    }

    public StockOpname detail(long opnameId) {
        return opnameRepo.findById(opnameId)
                .orElseThrow(() -> new BusinessException("Dokumen Stock Opname tidak ditemukan."));
    }

    /** Mutasi penyesuaian yang dihasilkan sebuah dokumen tervalidasi (R11.4). */
    public List<MutasiStok> mutasiPenyesuaian(StockOpname doc) {
        com.hakotjeria.model.MutasiFilter f = new com.hakotjeria.model.MutasiFilter();
        f.setDariTanggal(doc.getTanggal());
        f.setSampaiTanggal(doc.getTanggal());
        f.setShiftId(doc.getShiftId());
        return mutasiRepo.findByFilter(f).stream()
                .filter(MutasiStok::isPenyesuaian)
                .toList();
    }

    public boolean isShiftTerkunci(LocalDate tanggal, long shiftId) {
        return opnameRepo.isShiftTerkunci(tanggal, shiftId);
    }

    // ===================== HELPER =====================

    private void pastikanBolehDisimpan(StockOpname doc) {
        if (doc.getStatus() == StatusValidasi.MENUNGGU_VALIDASI) {
            throw new BusinessException("Dokumen sedang menunggu validasi Supervisor; angka telah terkunci (R10.4).");
        }
        if (doc.getStatus() == StatusValidasi.TERVALIDASI) {
            throw new BusinessException("Dokumen telah tervalidasi dan bersifat Read-Only permanen (R11.5).");
        }
        if (doc.getDetail().isEmpty()) {
            throw new BusinessException("Tidak ada barang untuk diaudit. Tambahkan master data terlebih dahulu.");
        }
    }

    private void hitungUlangSelisih(StockOpname doc) {
        for (DetailStockOpname d : doc.getDetail()) {
            if (d.getStokFisik() == null) {
                d.setStokFisik(BigDecimal.ZERO);
            }
            d.hitungSelisih();
        }
    }

    /** Mengisi detail draf baru dari seluruh barang kedua kategori inventaris. */
    private void isiDetailBaru(StockOpname doc) {
        for (JenisInventaris jenis : JenisInventaris.values()) {
            for (StokItem item : mutasiRepo.findStokSemua(jenis)) {
                DetailStockOpname d = new DetailStockOpname();
                d.setJenisInventaris(jenis);
                d.setBarangId(item.getBarangId());
                d.setStokSistem(item.getStok());
                d.setStokFisik(null);
                d.setSelisih(BigDecimal.ZERO);
                d.setNamaBarang(item.getNama());
                d.setSatuanBarang(item.getSatuan());
                doc.getDetail().add(d);
            }
        }
    }

    /**
     * Menyegarkan Stok Sistem draf revisi terhadap mutasi terbaru dan
     * menambahkan barang baru yang belum tercantum pada draf.
     */
    private void segarkanStokSistem(StockOpname doc) {
        for (DetailStockOpname d : doc.getDetail()) {
            d.setStokSistem(mutasiRepo.hitungStok(d.getJenisInventaris(), d.getBarangId()));
            d.hitungSelisih();
        }
        for (JenisInventaris jenis : JenisInventaris.values()) {
            for (StokItem item : mutasiRepo.findStokSemua(jenis)) {
                boolean sudahAda = doc.getDetail().stream()
                        .anyMatch(d -> d.getJenisInventaris() == jenis && d.getBarangId() == item.getBarangId());
                if (!sudahAda) {
                    DetailStockOpname d = new DetailStockOpname();
                    d.setOpnameId(doc.getId());
                    d.setJenisInventaris(jenis);
                    d.setBarangId(item.getBarangId());
                    d.setStokSistem(item.getStok());
                    d.setStokFisik(null);
                    d.setSelisih(BigDecimal.ZERO);
                    d.setNamaBarang(item.getNama());
                    d.setSatuanBarang(item.getSatuan());
                    doc.getDetail().add(d);
                }
            }
        }
    }

    private String generateKode(StockOpname doc) {
        Shift shift = shiftRepo.findById(doc.getShiftId());
        int urutan = shift == null ? 0 : shift.getUrutan();
        return "SO-" + KODE_FORMAT.format(doc.getTanggal()) + "-" + urutan;
    }

    private void requireSupervisor() {
        if (!Session.isSupervisor()) {
            throw new BusinessException("Hanya Supervisor yang dapat memvalidasi Stock Opname (R10.3).");
        }
    }
}
