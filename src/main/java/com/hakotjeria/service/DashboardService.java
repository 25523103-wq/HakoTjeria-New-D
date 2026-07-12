package com.hakotjeria.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.hakotjeria.model.JadwalTugas;
import com.hakotjeria.model.JenisInventaris;
import com.hakotjeria.model.KategoriTugas;
import com.hakotjeria.model.MutasiStok;
import com.hakotjeria.model.StatusValidasi;
import com.hakotjeria.model.StockOpname;
import com.hakotjeria.model.StokItem;
import com.hakotjeria.repository.JadwalTugasRepository;
import com.hakotjeria.repository.MutasiStokRepository;
import com.hakotjeria.repository.StockOpnameRepository;

/** Agregasi data ringkasan operasional untuk Dashboard (UC-02). */
public class DashboardService {

    private final JadwalTugasRepository tugasRepo = new JadwalTugasRepository();
    private final MutasiStokRepository mutasiRepo = new MutasiStokRepository();
    private final StockOpnameRepository opnameRepo = new StockOpnameRepository();

    public List<JadwalTugas> tugasHariIni() {
        return tugasRepo.findByTanggalShift(LocalDate.now(), null);
    }

    public long hitungTugasSelesai(List<JadwalTugas> tugas) {
        return tugas.stream().filter(t -> t.getStatus().isFinal()).count();
    }

    public List<StokItem> stokSemua(JenisInventaris jenis) {
        return mutasiRepo.findStokSemua(jenis);
    }

    /** Barang dengan stok di bawah batas aman minimum (R02.5). */
    public List<StokItem> stokDiBawahBatasAman(JenisInventaris jenis) {
        return stokSemua(jenis).stream()
                .filter(StokItem::isDiBawahBatasAman)
                .toList();
    }

    /** Status Stock Opname hari ini untuk satu shift ("Belum Dibuat" bila kosong). */
    public String statusOpnameHariIni(long shiftId) {
        Optional<StockOpname> doc = opnameRepo.findByTanggalShift(LocalDate.now(), shiftId);
        return doc.map(o -> o.getStatus().getLabel()).orElse("Belum Dibuat");
    }

    public Optional<StatusValidasi> statusValidasiOpnameHariIni(long shiftId) {
        return opnameRepo.findByTanggalShift(LocalDate.now(), shiftId).map(StockOpname::getStatus);
    }

    /** Aktivitas mutasi terbaru (R02.3). */
    public List<MutasiStok> mutasiTerbaru(int limit) {
        return mutasiRepo.findTerbaru(limit);
    }

    /** Data grafik perbandingan Qty Target vs Qty Aktual tugas produksi hari ini (R02.3). */
    public List<JadwalTugas> tugasProduksiHariIni() {
        return tugasHariIni().stream()
                .filter(t -> t.getKategori() == KategoriTugas.PRODUKSI_INTERNAL)
                .toList();
    }
}
