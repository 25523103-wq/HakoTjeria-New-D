package com.hakotjeria.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import com.hakotjeria.config.DatabaseConfig;
import com.hakotjeria.model.Bom;
import com.hakotjeria.model.JadwalTugas;
import com.hakotjeria.model.JenisInventaris;
import com.hakotjeria.model.JenisMutasi;
import com.hakotjeria.model.KategoriTugas;
import com.hakotjeria.model.KomponenBom;
import com.hakotjeria.model.MutasiStok;
import com.hakotjeria.model.StatusTugas;
import com.hakotjeria.repository.BomRepository;
import com.hakotjeria.repository.JadwalTugasRepository;
import com.hakotjeria.repository.MutasiStokRepository;
import com.hakotjeria.repository.RepositoryException;
import com.hakotjeria.repository.StockOpnameRepository;
import com.hakotjeria.util.BusinessException;
import com.hakotjeria.util.Formats;
import com.hakotjeria.util.Session;

/**
 * Aturan bisnis Eksekusi Produksi Internal (UC-06) dan
 * Eksekusi Pengambilan Eksternal (UC-07).
 */
public class ProduksiService {

    public static final String KET_HASIL_PRODUKSI = "Hasil Prod. Internal";
    public static final String KET_AMBIL_EKSTERNAL = "Ambil dr Kedai Tjeria";

    private final JadwalTugasRepository tugasRepo = new JadwalTugasRepository();
    private final BomRepository bomRepo = new BomRepository();
    private final MutasiStokRepository mutasiRepo = new MutasiStokRepository();
    private final StockOpnameRepository opnameRepo = new StockOpnameRepository();

    /** Satu baris pratinjau kebutuhan bahan pada Pop-Up Qty Aktual (ER06). */
    public static class KebutuhanBahan {
        private final KomponenBom komponen;
        private final BigDecimal kebutuhan;
        private final BigDecimal stokTersedia;

        public KebutuhanBahan(KomponenBom komponen, BigDecimal kebutuhan, BigDecimal stokTersedia) {
            this.komponen = komponen;
            this.kebutuhan = kebutuhan;
            this.stokTersedia = stokTersedia;
        }

        public KomponenBom getKomponen() {
            return komponen;
        }

        public BigDecimal getKebutuhan() {
            return kebutuhan;
        }

        public BigDecimal getStokTersedia() {
            return stokTersedia;
        }

        public boolean isCukup() {
            return stokTersedia.compareTo(kebutuhan) >= 0;
        }

        public BigDecimal getKekurangan() {
            return kebutuhan.subtract(stokTersedia).max(BigDecimal.ZERO);
        }
    }

    /**
     * Pratinjau kalkulasi BOM: Kebutuhan = Kuantitas BOM x Qty Aktual (R06.2),
     * berubah dinamis mengikuti angka yang diketik pada Pop-Up.
     */
    public List<KebutuhanBahan> previewKebutuhan(long produkId, BigDecimal qtyAktual) {
        Bom bom = bomRepo.findByProdukId(produkId)
                .orElseThrow(() -> new BusinessException("Produk ini belum memiliki BOM."));
        BigDecimal pengali = qtyAktual == null || qtyAktual.signum() <= 0 ? BigDecimal.ZERO : qtyAktual;
        List<KebutuhanBahan> hasil = new ArrayList<>();
        for (KomponenBom k : bom.getKomponen()) {
            BigDecimal kebutuhan = k.getQty().multiply(pengali);
            BigDecimal stok = mutasiRepo.hitungStok(JenisInventaris.BAHAN_BAKU, k.getBahanBakuId());
            hasil.add(new KebutuhanBahan(k, kebutuhan, stok));
        }
        return hasil;
    }

    /**
     * Konfirmasi produksi internal: memotong Bahan Baku sesuai BOM x Qty Aktual
     * dan menambah Produk Jadi, dalam SATU transaksi atomik (R06.1 - R06.8).
     */
    public void prosesProduksi(long tugasId, BigDecimal qtyAktual) {
        JadwalTugas tugas = tugasRepo.findById(tugasId)
                .orElseThrow(() -> new BusinessException("Tugas tidak ditemukan."));
        if (tugas.getKategori() != KategoriTugas.PRODUKSI_INTERNAL) {
            throw new BusinessException("Tugas ini bukan kategori Produksi Internal.");
        }
        if (tugas.getStatus().isFinal()) {
            throw new BusinessException("Tugas sudah berstatus final dan tidak dapat dieksekusi ulang (R06.8).");
        }
        if (qtyAktual == null || qtyAktual.signum() <= 0) {
            throw new BusinessException("Qty Aktual harus berupa angka lebih besar dari nol (R06.4).");
        }
        pastikanPenanggungJawab(tugas);
        pastikanShiftTidakTerkunci(tugas);

        Bom bom = bomRepo.findByProdukId(tugas.getProdukId())
                .orElseThrow(() -> new BusinessException("Produk ini belum memiliki BOM."));
        if (bom.getKomponen().isEmpty()) {
            throw new BusinessException("BOM produk ini belum memiliki komponen.");
        }

        try (Connection con = DatabaseConfig.getInstance().getConnection()) {
            con.setAutoCommit(false);
            try {
                // Verifikasi kecukupan stok seluruh bahan di dalam transaksi (R06.3).
                List<String> kekurangan = new ArrayList<>();
                for (KomponenBom k : bom.getKomponen()) {
                    BigDecimal kebutuhan = k.getQty().multiply(qtyAktual);
                    BigDecimal stok = mutasiRepo.hitungStok(con, JenisInventaris.BAHAN_BAKU, k.getBahanBakuId());
                    if (stok.compareTo(kebutuhan) < 0) {
                        kekurangan.add("- " + k.getNamaBahan() + ": butuh "
                                + Formats.qtyWithSatuan(kebutuhan, k.getSatuanBahan())
                                + ", tersedia " + Formats.qtyWithSatuan(stok, k.getSatuanBahan())
                                + " (kurang " + Formats.qtyWithSatuan(kebutuhan.subtract(stok), k.getSatuanBahan()) + ")");
                    }
                }
                if (!kekurangan.isEmpty()) {
                    con.rollback();
                    StringJoiner pesan = new StringJoiner("\n");
                    pesan.add("Stok Bahan Baku tidak mencukupi. Turunkan Qty Aktual.");
                    kekurangan.forEach(pesan::add);
                    throw new BusinessException(pesan.toString());
                }

                String ketProduksi = "Prod. " + Formats.qty(qtyAktual) + " " + tugas.getNamaProduk();
                for (KomponenBom k : bom.getKomponen()) {
                    mutasiRepo.insert(con, buatMutasi(tugas, JenisInventaris.BAHAN_BAKU,
                            k.getBahanBakuId(), JenisMutasi.OUT, k.getQty().multiply(qtyAktual), ketProduksi));
                }
                mutasiRepo.insert(con, buatMutasi(tugas, JenisInventaris.PRODUK_JADI,
                        tugas.getProdukId(), JenisMutasi.IN, qtyAktual, KET_HASIL_PRODUKSI));
                tugasRepo.updateSelesai(con, tugasId, qtyAktual);
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } catch (BusinessException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RepositoryException("Transaksi produksi gagal dan dibatalkan", e);
        }
    }

    /**
     * Pencatatan penerimaan barang eksternal: hanya mutasi IN Produk Jadi,
     * seluruh kalkulasi BOM diabaikan (R07.1 - R07.5).
     */
    public void prosesPengambilan(long tugasId, BigDecimal qtyDiterima) {
        JadwalTugas tugas = tugasRepo.findById(tugasId)
                .orElseThrow(() -> new BusinessException("Tugas tidak ditemukan."));
        if (tugas.getKategori() != KategoriTugas.PENGAMBILAN_EKSTERNAL) {
            throw new BusinessException("Tugas ini bukan kategori Pengambilan Eksternal.");
        }
        if (tugas.getStatus() == StatusTugas.SUDAH_DIAMBIL) {
            throw new BusinessException("Status \"Sudah Diambil\" bersifat satu arah dan tugas telah terkunci (R07.4).");
        }
        if (tugas.getStatus().isFinal()) {
            throw new BusinessException("Tugas sudah berstatus final dan tidak dapat dieksekusi ulang.");
        }
        if (qtyDiterima == null || qtyDiterima.signum() <= 0) {
            throw new BusinessException("Qty Diterima harus berupa angka lebih besar dari nol.");
        }
        pastikanPenanggungJawab(tugas);
        pastikanShiftTidakTerkunci(tugas);

        try (Connection con = DatabaseConfig.getInstance().getConnection()) {
            con.setAutoCommit(false);
            try {
                mutasiRepo.insert(con, buatMutasi(tugas, JenisInventaris.PRODUK_JADI,
                        tugas.getProdukId(), JenisMutasi.IN, qtyDiterima, KET_AMBIL_EKSTERNAL));
                tugasRepo.updateSudahDiambil(con, tugasId, qtyDiterima);
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RepositoryException("Transaksi pengambilan gagal dan dibatalkan", e);
        }
    }

    private MutasiStok buatMutasi(JadwalTugas tugas, JenisInventaris jenisInventaris, long barangId,
                                  JenisMutasi jenis, BigDecimal qty, String keterangan) {
        MutasiStok m = new MutasiStok();
        m.setTanggal(tugas.getTanggal());
        m.setShiftId(tugas.getShiftId());
        m.setJenisInventaris(jenisInventaris);
        m.setBarangId(barangId);
        m.setJenis(jenis);
        m.setQty(qty);
        m.setKeterangan(keterangan);
        m.setCreatedBy(Session.getCurrentUser().getId());
        return m;
    }

    /**
     * Tugas yang memiliki Staff Penanggung Jawab hanya dapat dikonfirmasi
     * oleh staff yang ditunjuk; tugas tanpa penanggung jawab bebas diambil.
     */
    private void pastikanPenanggungJawab(JadwalTugas tugas) {
        if (tugas.getStaffId() != null
                && tugas.getStaffId().longValue() != Session.getCurrentUser().getId()) {
            throw new BusinessException("Tugas ini ditugaskan kepada "
                    + (tugas.getNamaStaff() == null ? "staff lain" : tugas.getNamaStaff())
                    + ". Hanya staff penanggung jawab yang dapat mengkonfirmasi status tugas.");
        }
    }

    private void pastikanShiftTidakTerkunci(JadwalTugas tugas) {
        if (opnameRepo.isShiftTerkunci(tugas.getTanggal(), tugas.getShiftId())) {
            throw new BusinessException("Shift ini telah dikunci karena Stock Opname-nya sudah disetujui (R10.6).");
        }
    }
}
