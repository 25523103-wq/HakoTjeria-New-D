package com.hakotjeria.service;

import java.time.LocalDate;
import java.util.List;

import com.hakotjeria.model.JadwalTugas;
import com.hakotjeria.model.KategoriTugas;
import com.hakotjeria.model.ProdukJadi;
import com.hakotjeria.model.StatusTugas;
import com.hakotjeria.repository.BomRepository;
import com.hakotjeria.repository.JadwalTugasRepository;
import com.hakotjeria.repository.MasterDataRepository;
import com.hakotjeria.repository.StockOpnameRepository;
import com.hakotjeria.util.BusinessException;
import com.hakotjeria.util.Session;

/** Aturan bisnis Manajemen Jadwal Tugas Operasional (UC-05). */
public class JadwalTugasService {

    private final JadwalTugasRepository tugasRepo = new JadwalTugasRepository();
    private final MasterDataRepository masterRepo = new MasterDataRepository();
    private final BomRepository bomRepo = new BomRepository();
    private final StockOpnameRepository opnameRepo = new StockOpnameRepository();

    public List<JadwalTugas> daftarTugas(LocalDate tanggal, Long shiftId) {
        return tugasRepo.findByTanggalShift(tanggal, shiftId);
    }

    public JadwalTugas getTugas(long id) {
        return tugasRepo.findById(id)
                .orElseThrow(() -> new BusinessException("Tugas tidak ditemukan."));
    }

    /** Pembuatan tugas baru dengan status bawaan sesuai kategori (R05.1 - R05.4). */
    public void buatTugas(JadwalTugas tugas) {
        requireSupervisor();
        validasi(tugas);
        pastikanShiftTidakTerkunci(tugas.getTanggal(), tugas.getShiftId());
        tugas.setStatus(statusBawaan(tugas.getKategori()));
        tugasRepo.save(tugas);
    }

    /** Perubahan tugas; tugas berstatus final tidak dapat diubah (R05.5). */
    public void ubahTugas(JadwalTugas tugas) {
        requireSupervisor();
        JadwalTugas existing = getTugas(tugas.getId());
        if (existing.getStatus().isFinal()) {
            throw new BusinessException("Tugas berstatus \"" + existing.getStatus().getLabel()
                    + "\" bersifat final dan tidak dapat diubah (R05.5).");
        }
        pastikanShiftTidakTerkunci(existing.getTanggal(), existing.getShiftId());
        validasi(tugas);
        pastikanShiftTidakTerkunci(tugas.getTanggal(), tugas.getShiftId());
        // Status bawaan mengikuti kategori terbaru bila kategori berubah.
        tugas.setStatus(statusBawaan(tugas.getKategori()));
        tugasRepo.update(tugas);
    }

    /** Penghapusan tugas; tugas final tidak dapat dihapus (R05.5). */
    public void hapusTugas(long id) {
        requireSupervisor();
        JadwalTugas existing = getTugas(id);
        if (existing.getStatus().isFinal()) {
            throw new BusinessException("Tugas berstatus \"" + existing.getStatus().getLabel()
                    + "\" bersifat final dan tidak dapat dihapus (R05.5).");
        }
        pastikanShiftTidakTerkunci(existing.getTanggal(), existing.getShiftId());
        tugasRepo.delete(id);
    }

    /**
     * Menandai tugas "Tidak Terpenuhi" disertai alasan; tidak ada mutasi stok
     * yang dicatat (alur alternatif UC-06 dan UC-07).
     */
    public void setTidakTerpenuhi(long tugasId, String alasan) {
        JadwalTugas tugas = getTugas(tugasId);
        if (tugas.getStatus().isFinal()) {
            throw new BusinessException("Tugas sudah berstatus final dan tidak dapat diubah (R06.8).");
        }
        if (alasan == null || alasan.isBlank()) {
            throw new BusinessException("Alasan kegagalan tugas wajib diisi.");
        }
        pastikanPenanggungJawab(tugas);
        pastikanShiftTidakTerkunci(tugas.getTanggal(), tugas.getShiftId());
        tugasRepo.updateTidakTerpenuhi(tugasId, alasan.trim());
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

    // ===================== HELPER =====================

    private StatusTugas statusBawaan(KategoriTugas kategori) {
        return kategori == KategoriTugas.PRODUKSI_INTERNAL
                ? StatusTugas.BELUM_DIKERJAKAN
                : StatusTugas.BELUM_DIAMBIL;
    }

    /**
     * Validasi keselarasan Kategori Tugas dengan Sumber Produk (R05.3):
     * Produksi Internal untuk produk Internal ber-BOM,
     * Pengambilan Eksternal untuk produk Eksternal tanpa BOM.
     */
    private void validasi(JadwalTugas tugas) {
        if (tugas.getTanggal() == null) {
            throw new BusinessException("Tanggal tugas wajib diisi (R05.2).");
        }
        if (tugas.getShiftId() <= 0) {
            throw new BusinessException("Shift tugas wajib dipilih (R05.2).");
        }
        if (tugas.getKategori() == null) {
            throw new BusinessException("Kategori Tugas wajib dipilih.");
        }
        if (tugas.getQtyTarget() == null || tugas.getQtyTarget().signum() <= 0) {
            throw new BusinessException("Qty Target harus lebih besar dari nol.");
        }
        ProdukJadi produk = masterRepo.findProdukJadiById(tugas.getProdukId())
                .orElseThrow(() -> new BusinessException("Produk Jadi target wajib dipilih."));
        if (tugas.getKategori() == KategoriTugas.PRODUKSI_INTERNAL) {
            if (!produk.isInternal()) {
                throw new BusinessException("Kategori Produksi Internal hanya untuk produk bersumber Internal (R05.3).");
            }
            if (!bomRepo.hasBomWithKomponen(produk.getId())) {
                throw new BusinessException("Produk \"" + produk.getNama() + "\" belum memiliki BOM. "
                        + "Susun resep BOM terlebih dahulu (R05.3).");
            }
        } else {
            if (!produk.isEksternal()) {
                throw new BusinessException("Kategori Pengambilan Eksternal hanya untuk produk bersumber Eksternal (R05.3).");
            }
        }
    }

    /** Shift terkunci permanen setelah opname disetujui (R10.6, Kekangan 10). */
    private void pastikanShiftTidakTerkunci(LocalDate tanggal, long shiftId) {
        if (opnameRepo.isShiftTerkunci(tanggal, shiftId)) {
            throw new BusinessException("Shift ini telah dikunci karena Stock Opname-nya sudah disetujui. "
                    + "Tugas dan mutasi tidak dapat diubah lagi (R10.6).");
        }
    }

    private void requireSupervisor() {
        if (!Session.isSupervisor()) {
            throw new BusinessException("Hanya Supervisor yang dapat mengelola jadwal tugas (R05.1).");
        }
    }
}
