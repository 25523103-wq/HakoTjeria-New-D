package com.hakotjeria.tools;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.hakotjeria.config.DatabaseConfig;
import com.hakotjeria.model.JenisInventaris;
import com.hakotjeria.model.JenisMutasi;
import com.hakotjeria.model.KategoriTugas;
import com.hakotjeria.model.MutasiStok;
import com.hakotjeria.model.Satuan;
import com.hakotjeria.model.StatusTugas;
import com.hakotjeria.model.StatusValidasi;
import com.hakotjeria.model.SumberProduk;
import com.hakotjeria.service.ProduksiService;
import com.hakotjeria.util.PasswordUtil;

/**
 * Seeder data dummy: mensimulasikan pemakaian aplikasi selama 1 bulan (30 hari)
 * (produksi kue internal, pengambilan eksternal, penjualan, pembelian bahan,
 * waste, hingga Stock Opname tervalidasi) secara kronologis sehingga
 * stok tidak pernah negatif dan seluruh angka konsisten dengan aturan bisnis.
 *
 * Menjalankan:
 *   mvn -q compile exec:java -Dexec.mainClass=com.hakotjeria.tools.DummyDataSeeder
 *   mvn -q compile exec:java -Dexec.mainClass=com.hakotjeria.tools.DummyDataSeeder -Dexec.args="--force"
 *
 * --force menghapus seluruh data transaksi lama (jadwal, mutasi, opname)
 * sebelum mengisi ulang; data master dipakai ulang bila sudah ada.
 * Aplikasi harus dalam keadaan TIDAK berjalan (H2 embedded, single-process).
 */
public final class DummyDataSeeder {

    private static final Random RND = new Random(42);

    /** Stok berjalan per barang, kunci "BB:<id>" / "PJ:<id>", agar tak pernah negatif. */
    private static final Map<String, BigDecimal> STOK = new HashMap<>();

    private DummyDataSeeder() {
    }

    public static void main(String[] args) throws Exception {
        boolean force = args.length > 0 && "--force".equals(args[0]);
        DatabaseConfig.getInstance().initSchema();

        try (Connection con = DatabaseConfig.getInstance().getConnection()) {
            long jumlahMutasi = count(con, "mutasi_stok");
            if (jumlahMutasi > 0 && !force) {
                System.out.println("Tabel mutasi_stok sudah berisi " + jumlahMutasi
                        + " baris. Jalankan dengan argumen --force untuk menghapus data"
                        + " transaksi lama dan mengisi ulang data dummy.");
                return;
            }
            if (force) {
                con.createStatement().execute("DELETE FROM detail_stock_opname");
                con.createStatement().execute("DELETE FROM stock_opname");
                con.createStatement().execute("DELETE FROM mutasi_stok");
                con.createStatement().execute("DELETE FROM jadwal_tugas");
                System.out.println("Data transaksi lama dihapus.");
            }

            seed(con);
        }
        System.out.println("Selesai. Data dummy 1 bulan (30 hari) berhasil dibuat.");
    }

    // ===================== ORKESTRASI =====================

    private static void seed(Connection con) throws SQLException {
        LocalDate hariIni = LocalDate.now();
        final int totalHari = 30;
        LocalDate mulai = hariIni.minusDays(totalHari);

        // ---------- Pengguna ----------
        long supervisor = ensureUser(con, "Budi Santoso", "supervisor", "SUPERVISOR", "supervisor123");
        long siti = ensureUser(con, "Siti Aminah", "staff", "STAFF", "staff123");
        long andi = ensureUser(con, "Andi Wijaya", "andi", "STAFF", "staff123");
        long rina = ensureUser(con, "Rina Puspita", "rina", "STAFF", "staff123");
        long[] staff = {siti, andi, rina};

        long shift1 = shiftId(con, 1);
        long shift2 = shiftId(con, 2);

        // ---------- Bahan Baku ----------
        long tepung = ensureBahan(con, "Tepung Terigu", Satuan.GRAM, "5000");
        long gula = ensureBahan(con, "Gula Pasir", Satuan.GRAM, "5000");
        long telur = ensureBahan(con, "Telur", Satuan.PCS, "60");
        long butter = ensureBahan(con, "Butter", Satuan.GRAM, "3000");
        long creamCheese = ensureBahan(con, "Cream Cheese", Satuan.GRAM, "2000");
        long whippingCream = ensureBahan(con, "Whipping Cream", Satuan.ML, "2000");
        long susu = ensureBahan(con, "Susu Cair", Satuan.ML, "3000");
        long coklatBubuk = ensureBahan(con, "Coklat Bubuk", Satuan.GRAM, "1500");
        long darkChocolate = ensureBahan(con, "Dark Chocolate", Satuan.GRAM, "2000");
        long strawberry = ensureBahan(con, "Strawberry Segar", Satuan.GRAM, "1500");
        long biskuit = ensureBahan(con, "Biskuit Marie", Satuan.GRAM, "1500");
        long vanilla = ensureBahan(con, "Vanilla Extract", Satuan.ML, "200");
        long bakingPowder = ensureBahan(con, "Baking Powder", Satuan.GRAM, "300");
        long keju = ensureBahan(con, "Keju Cheddar", Satuan.GRAM, "1500");

        // ---------- Produk Jadi ----------
        long cheesecake = ensureProduk(con, "Cheesecake", Satuan.PCS, SumberProduk.INTERNAL, "3");
        long strawberryCake = ensureProduk(con, "Strawberry Cake", Satuan.PCS, SumberProduk.INTERNAL, "3");
        long chocolateCake = ensureProduk(con, "Chocolate Cake", Satuan.PCS, SumberProduk.INTERNAL, "3");
        long redVelvet = ensureProduk(con, "Red Velvet Cake", Satuan.PCS, SumberProduk.INTERNAL, "2");
        long brownies = ensureProduk(con, "Brownies Coklat", Satuan.PCS, SumberProduk.INTERNAL, "5");
        long croissant = ensureProduk(con, "Croissant", Satuan.PCS, SumberProduk.EKSTERNAL, "10");
        long donat = ensureProduk(con, "Donat Coklat", Satuan.PCS, SumberProduk.EKSTERNAL, "12");
        long rotiSobek = ensureProduk(con, "Roti Sobek", Satuan.PCS, SumberProduk.EKSTERNAL, "8");

        // ---------- BOM (kebutuhan bahan per 1 pcs) ----------
        Map<Long, Map<Long, BigDecimal>> bomProduk = new LinkedHashMap<>();
        bomProduk.put(cheesecake, komposisi(
                creamCheese, "250", biskuit, "100", butter, "80", gula, "90",
                telur, "2", whippingCream, "100", vanilla, "5"));
        bomProduk.put(strawberryCake, komposisi(
                tepung, "200", gula, "150", telur, "3", butter, "100",
                whippingCream, "150", strawberry, "200", bakingPowder, "8", vanilla, "5"));
        bomProduk.put(chocolateCake, komposisi(
                tepung, "180", gula, "160", telur, "3", butter, "120",
                coklatBubuk, "60", darkChocolate, "100", susu, "120", bakingPowder, "8"));
        bomProduk.put(redVelvet, komposisi(
                tepung, "200", gula, "150", telur, "2", butter, "100", coklatBubuk, "15",
                susu, "150", creamCheese, "150", bakingPowder, "8", vanilla, "5", keju, "50"));
        bomProduk.put(brownies, komposisi(
                tepung, "120", gula, "140", telur, "2", butter, "130",
                darkChocolate, "150", coklatBubuk, "40"));
        for (Map.Entry<Long, Map<Long, BigDecimal>> e : bomProduk.entrySet()) {
            ensureBom(con, e.getKey(), e.getValue());
        }

        long[] internal = {cheesecake, strawberryCake, chocolateCake, redVelvet, brownies};
        long[] eksternal = {croissant, donat, rotiSobek};
        Map<Long, String> namaProduk = namaBarang(con, "produk_jadi");

        // ---------- Hari-0: penerimaan stok awal seluruh bahan ----------
        LocalDateTime awal = mulai.atTime(6, 45);
        String ketAwal = "Penerimaan awal bahan baku dari supplier";
        belanja(con, mulai, shift1, supervisor, awal, ketAwal, Map.ofEntries(
                Map.entry(tepung, new BigDecimal("15000")), Map.entry(gula, new BigDecimal("15000")),
                Map.entry(telur, new BigDecimal("240")), Map.entry(butter, new BigDecimal("10000")),
                Map.entry(creamCheese, new BigDecimal("8000")), Map.entry(whippingCream, new BigDecimal("6000")),
                Map.entry(susu, new BigDecimal("8000")), Map.entry(coklatBubuk, new BigDecimal("4000")),
                Map.entry(darkChocolate, new BigDecimal("6000")), Map.entry(strawberry, new BigDecimal("4000")),
                Map.entry(biskuit, new BigDecimal("4000")), Map.entry(vanilla, new BigDecimal("500")),
                Map.entry(bakingPowder, new BigDecimal("800")), Map.entry(keju, new BigDecimal("3000"))));

        // ---------- Simulasi harian selama 1 bulan ----------
        for (int hari = 0; hari < totalHari; hari++) {
            LocalDate tgl = mulai.plusDays(hari);

            // Restok berkala tiap 4 hari (selain hari pertama).
            if (hari > 0 && hari % 4 == 0) {
                belanja(con, tgl, shift1, staff[hari % staff.length], tgl.atTime(7, 15),
                        "Pembelian mingguan bahan baku dari supplier", Map.ofEntries(
                                Map.entry(tepung, new BigDecimal("8000")), Map.entry(gula, new BigDecimal("8000")),
                                Map.entry(telur, new BigDecimal("120")), Map.entry(butter, new BigDecimal("5000")),
                                Map.entry(creamCheese, new BigDecimal("4000")),
                                Map.entry(whippingCream, new BigDecimal("3000")),
                                Map.entry(susu, new BigDecimal("4000")),
                                Map.entry(strawberry, new BigDecimal("2500")),
                                Map.entry(darkChocolate, new BigDecimal("3000"))));
            }

            // --- Shift 1: dua tugas produksi internal + satu pengambilan eksternal ---
            long p1 = internal[hari % internal.length];
            long p2 = internal[(hari + 2) % internal.length];
            produksi(con, tgl, shift1, p1, namaProduk.get(p1), bomProduk.get(p1),
                    4 + RND.nextInt(3), staff[hari % staff.length], tgl.atTime(8, 30));
            produksi(con, tgl, shift1, p2, namaProduk.get(p2), bomProduk.get(p2),
                    3 + RND.nextInt(3), staff[(hari + 1) % staff.length], tgl.atTime(10, 15));

            long amb = eksternal[hari % eksternal.length];
            pengambilan(con, tgl, shift1, amb, 15 + RND.nextInt(11),
                    staff[(hari + 2) % staff.length], tgl.atTime(9, 20));

            // --- Shift 2: satu tugas produksi (sesekali gagal/tidak terpenuhi) ---
            long p3 = internal[(hari + 4) % internal.length];
            if (hari == 4 || hari == 19) {
                // Contoh alur alternatif: tugas final berstatus Tidak Terpenuhi.
                String alasan = hari == 4 ? "Oven bermasalah, produksi dibatalkan"
                        : "Stok bahan baku tidak mencukupi, produksi ditunda";
                insertTugas(con, tgl, shift2, KategoriTugas.PRODUKSI_INTERNAL, p3,
                        new BigDecimal(4), null, null, StatusTugas.TIDAK_TERPENUHI,
                        staff[hari % staff.length], alasan,
                        tgl.minusDays(1).atTime(16, 30));
            } else {
                produksi(con, tgl, shift2, p3, namaProduk.get(p3), bomProduk.get(p3),
                        3 + RND.nextInt(3), staff[(hari + 1) % staff.length],
                        tgl.atTime(15, 30));
            }
            if (hari % 3 == 1) {
                pengambilan(con, tgl, shift2, eksternal[(hari + 1) % eksternal.length],
                        10 + RND.nextInt(9), staff[hari % staff.length], tgl.atTime(16, 40));
            }

            // --- Waste / defect sesekali ---
            if (hari % 4 == 2) {
                long rusak = internal[hari % internal.length];
                keluarkan(con, tgl, shift2, JenisInventaris.PRODUK_JADI, rusak, BigDecimal.ONE,
                        "Waste - tidak lolos QC", staff[hari % staff.length], tgl.atTime(17, 10));
            }
            if (hari % 5 == 3) {
                keluarkan(con, tgl, shift1, JenisInventaris.BAHAN_BAKU, strawberry,
                        new BigDecimal("150"), "Bahan rusak / kedaluwarsa dibuang",
                        staff[(hari + 1) % staff.length], tgl.atTime(7, 50));
            }

            // --- Penjualan harian tiap sore (mutasi OUT manual produk jadi) ---
            long kasir = staff[(hari + 2) % staff.length];
            for (long produk : gabung(internal, eksternal)) {
                BigDecimal stok = stok(JenisInventaris.PRODUK_JADI, produk);
                if (stok.signum() <= 0) {
                    continue;
                }
                int laku = Math.max(1, stok.multiply(new BigDecimal("0.6"))
                        .add(new BigDecimal(RND.nextInt(3))).intValue());
                BigDecimal qty = new BigDecimal(Math.min(laku, stok.intValue()));
                if (qty.signum() > 0) {
                    keluarkan(con, tgl, shift2, JenisInventaris.PRODUK_JADI, produk, qty,
                            "Penjualan harian", kasir, tgl.atTime(20, 10 + RND.nextInt(40)));
                }
            }

            // --- Stock Opname tervalidasi tiap akhir minggu (hari ke-7, 14, 21, 28) ---
            if (hari % 7 == 6) {
                stockOpname(con, tgl, shift2, 2, staff[hari % staff.length], supervisor, true);
            }
        }

        // ---------- Hari ini: hari kerja penuh, opname MENUNGGU VALIDASI ----------
        produksi(con, hariIni, shift1, cheesecake, namaProduk.get(cheesecake),
                bomProduk.get(cheesecake), 5, siti, hariIni.atTime(8, 30));
        produksi(con, hariIni, shift1, strawberryCake, namaProduk.get(strawberryCake),
                bomProduk.get(strawberryCake), 4, andi, hariIni.atTime(10, 15));
        pengambilan(con, hariIni, shift1, croissant, 20, rina, hariIni.atTime(9, 20));
        produksi(con, hariIni, shift2, chocolateCake, namaProduk.get(chocolateCake),
                bomProduk.get(chocolateCake), 4, siti, hariIni.atTime(15, 30));
        for (long produk : gabung(internal, eksternal)) {
            BigDecimal stok = stok(JenisInventaris.PRODUK_JADI, produk);
            if (stok.signum() > 0) {
                BigDecimal laku = stok.multiply(new BigDecimal("0.5"))
                        .setScale(0, java.math.RoundingMode.DOWN).max(BigDecimal.ONE).min(stok);
                keluarkan(con, hariIni, shift2, JenisInventaris.PRODUK_JADI, produk, laku,
                        "Penjualan harian", rina, hariIni.atTime(20, 10 + RND.nextInt(40)));
            }
        }
        // Staff sudah mengirim dokumen opname; Supervisor belum memvalidasi.
        stockOpname(con, hariIni, shift2, 2, andi, supervisor, false);

        // ---------- Besok: tugas terjadwal yang belum dikerjakan ----------
        LocalDate besok = hariIni.plusDays(1);
        LocalDateTime sorePerencanaan = hariIni.atTime(16, 30);
        insertTugas(con, besok, shift1, KategoriTugas.PRODUKSI_INTERNAL, redVelvet,
                new BigDecimal(4), null, null, StatusTugas.BELUM_DIKERJAKAN, siti, null, sorePerencanaan);
        insertTugas(con, besok, shift1, KategoriTugas.PRODUKSI_INTERNAL, brownies,
                new BigDecimal(6), null, null, StatusTugas.BELUM_DIKERJAKAN, andi, null, sorePerencanaan);
        insertTugas(con, besok, shift1, KategoriTugas.PENGAMBILAN_EKSTERNAL, donat,
                new BigDecimal(18), null, null, StatusTugas.BELUM_DIAMBIL, rina, null, sorePerencanaan);
    }

    // ===================== SIMULASI TRANSAKSI =====================

    /** Tugas produksi internal Selesai: OUT bahan sesuai BOM + IN produk jadi. */
    private static void produksi(Connection con, LocalDate tgl, long shiftId, long produkId,
                                 String namaProduk, Map<Long, BigDecimal> bom, int qtyTarget,
                                 long staffId, LocalDateTime jam) throws SQLException {
        int qtyAktual = Math.max(1, qtyTarget + (RND.nextInt(3) - 1)); // target -1 .. +1
        BigDecimal qty = new BigDecimal(qtyAktual);

        // Pastikan bahan cukup; bila kurang, catat pembelian tambahan pagi harinya.
        Map<Long, BigDecimal> kekurangan = new LinkedHashMap<>();
        for (Map.Entry<Long, BigDecimal> k : bom.entrySet()) {
            BigDecimal butuh = k.getValue().multiply(qty);
            BigDecimal stok = stok(JenisInventaris.BAHAN_BAKU, k.getKey());
            if (stok.compareTo(butuh) < 0) {
                kekurangan.put(k.getKey(), butuh.subtract(stok).multiply(new BigDecimal(3)));
            }
        }
        if (!kekurangan.isEmpty()) {
            belanja(con, tgl, shiftId, staffId, tgl.atTime(7, 30),
                    "Pembelian tambahan bahan baku", kekurangan);
        }

        insertTugas(con, tgl, shiftId, KategoriTugas.PRODUKSI_INTERNAL, produkId,
                new BigDecimal(qtyTarget), qty, null, StatusTugas.SELESAI, staffId, null,
                tgl.minusDays(1).atTime(16, 30));

        String ket = "Prod. " + qtyAktual + " " + namaProduk;
        for (Map.Entry<Long, BigDecimal> k : bom.entrySet()) {
            insertMutasi(con, tgl, shiftId, JenisInventaris.BAHAN_BAKU, k.getKey(),
                    JenisMutasi.OUT, k.getValue().multiply(qty), ket, staffId, jam);
        }
        insertMutasi(con, tgl, shiftId, JenisInventaris.PRODUK_JADI, produkId,
                JenisMutasi.IN, qty, ProduksiService.KET_HASIL_PRODUKSI, staffId,
                jam.plusMinutes(1));
    }

    /** Tugas pengambilan eksternal Sudah Diambil: IN produk jadi dari Kedai Tjeria. */
    private static void pengambilan(Connection con, LocalDate tgl, long shiftId, long produkId,
                                    int qtyTarget, long staffId, LocalDateTime jam) throws SQLException {
        int diterima = Math.max(1, qtyTarget - RND.nextInt(3));
        insertTugas(con, tgl, shiftId, KategoriTugas.PENGAMBILAN_EKSTERNAL, produkId,
                new BigDecimal(qtyTarget), null, new BigDecimal(diterima),
                StatusTugas.SUDAH_DIAMBIL, staffId, null, tgl.minusDays(1).atTime(16, 30));
        insertMutasi(con, tgl, shiftId, JenisInventaris.PRODUK_JADI, produkId,
                JenisMutasi.IN, new BigDecimal(diterima), ProduksiService.KET_AMBIL_EKSTERNAL,
                staffId, jam);
    }

    /** Mutasi IN pembelian untuk beberapa bahan sekaligus. */
    private static void belanja(Connection con, LocalDate tgl, long shiftId, long userId,
                                LocalDateTime jam, String ket, Map<Long, BigDecimal> isi) throws SQLException {
        for (Map.Entry<Long, BigDecimal> e : isi.entrySet()) {
            insertMutasi(con, tgl, shiftId, JenisInventaris.BAHAN_BAKU, e.getKey(),
                    JenisMutasi.IN, e.getValue(), ket, userId, jam);
        }
    }

    /** Mutasi OUT manual (penjualan / waste), dipangkas agar stok tidak negatif. */
    private static void keluarkan(Connection con, LocalDate tgl, long shiftId, JenisInventaris jenis,
                                  long barangId, BigDecimal qty, String ket, long userId,
                                  LocalDateTime jam) throws SQLException {
        BigDecimal stok = stok(jenis, barangId);
        BigDecimal dipakai = qty.min(stok);
        if (dipakai.signum() <= 0) {
            return;
        }
        insertMutasi(con, tgl, shiftId, jenis, barangId, JenisMutasi.OUT, dipakai, ket, userId, jam);
    }

    /**
     * Dokumen Stock Opname berisi detail seluruh barang, sebagian kecil berbeda
     * dengan stok sistem. Bila tervalidasi, mutasi penyesuaian ikut dibuat;
     * bila belum, dokumen berhenti di status Menunggu Validasi (tanpa mutasi,
     * sesuai alur UC-10: penyesuaian baru terjadi saat Supervisor menyetujui).
     */
    private static void stockOpname(Connection con, LocalDate tgl, long shiftId, int urutanShift,
                                    long staffId, long supervisorId, boolean tervalidasi) throws SQLException {
        String kode = "SO-" + tgl.toString().replace("-", "") + "-" + urutanShift;
        LocalDateTime inputAt = tgl.atTime(21, 25);
        LocalDateTime validAt = tgl.atTime(21, 50);

        long opnameId;
        try (PreparedStatement ps = con.prepareStatement("""
                INSERT INTO stock_opname (kode, tanggal, shift_id, status, input_by, input_at, valid_by, valid_at)
                VALUES (?,?,?,?,?,?,?,?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, kode);
            ps.setDate(2, Date.valueOf(tgl));
            ps.setLong(3, shiftId);
            ps.setString(4, (tervalidasi ? StatusValidasi.TERVALIDASI : StatusValidasi.MENUNGGU_VALIDASI).name());
            ps.setLong(5, staffId);
            ps.setTimestamp(6, Timestamp.valueOf(inputAt));
            if (tervalidasi) {
                ps.setLong(7, supervisorId);
                ps.setTimestamp(8, Timestamp.valueOf(validAt));
            } else {
                ps.setNull(7, java.sql.Types.BIGINT);
                ps.setNull(8, java.sql.Types.TIMESTAMP);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                opnameId = keys.getLong(1);
            }
        }

        try (PreparedStatement det = con.prepareStatement("""
                INSERT INTO detail_stock_opname
                    (opname_id, jenis_inventaris, barang_id, stok_sistem, stok_fisik, selisih)
                VALUES (?,?,?,?,?,?)
                """)) {
            int nomor = 0;
            for (Map.Entry<String, BigDecimal> e : new LinkedHashMap<>(STOK).entrySet()) {
                JenisInventaris jenis = e.getKey().startsWith("BB:")
                        ? JenisInventaris.BAHAN_BAKU : JenisInventaris.PRODUK_JADI;
                long barangId = Long.parseLong(e.getKey().substring(3));
                BigDecimal sistem = e.getValue();

                // Sekitar 1 dari 6 barang diberi selisih fisik kecil (audit realistis).
                BigDecimal selisih = BigDecimal.ZERO;
                if (nomor++ % 6 == 2 && sistem.signum() > 0) {
                    BigDecimal basis = jenis == JenisInventaris.BAHAN_BAKU
                            ? new BigDecimal(10 + RND.nextInt(90)) : BigDecimal.ONE;
                    selisih = basis.min(sistem).negate(); // umumnya fisik lebih sedikit
                }
                BigDecimal fisik = sistem.add(selisih);

                det.setLong(1, opnameId);
                det.setString(2, jenis.name());
                det.setLong(3, barangId);
                det.setBigDecimal(4, sistem);
                det.setBigDecimal(5, fisik);
                det.setBigDecimal(6, selisih);
                det.addBatch();

                if (tervalidasi && selisih.signum() != 0) {
                    insertMutasi(con, tgl, shiftId, jenis, barangId,
                            selisih.signum() > 0 ? JenisMutasi.IN : JenisMutasi.OUT,
                            selisih.abs(), MutasiStok.KET_PENYESUAIAN, supervisorId,
                            validAt.plusMinutes(2));
                }
            }
            det.executeBatch();
        }
    }

    // ===================== INSERT DASAR =====================

    private static void insertMutasi(Connection con, LocalDate tgl, long shiftId,
                                     JenisInventaris jenisInv, long barangId, JenisMutasi jenis,
                                     BigDecimal qty, String ket, long userId,
                                     LocalDateTime createdAt) throws SQLException {
        String kunci = kunci(jenisInv, barangId);
        BigDecimal sesudah = jenis == JenisMutasi.IN
                ? stok(jenisInv, barangId).add(qty)
                : stok(jenisInv, barangId).subtract(qty);
        if (sesudah.signum() < 0) {
            throw new IllegalStateException("Seeder menghasilkan stok negatif untuk " + kunci
                    + " (" + ket + "). Periksa urutan transaksi.");
        }
        try (PreparedStatement ps = con.prepareStatement("""
                INSERT INTO mutasi_stok
                    (tanggal, shift_id, jenis_inventaris, barang_id, jenis, qty, keterangan, created_by, created_at)
                VALUES (?,?,?,?,?,?,?,?,?)
                """)) {
            ps.setDate(1, Date.valueOf(tgl));
            ps.setLong(2, shiftId);
            ps.setString(3, jenisInv.name());
            ps.setLong(4, barangId);
            ps.setString(5, jenis.name());
            ps.setBigDecimal(6, qty);
            ps.setString(7, ket);
            ps.setLong(8, userId);
            ps.setTimestamp(9, Timestamp.valueOf(createdAt));
            ps.executeUpdate();
        }
        STOK.put(kunci, sesudah);
    }

    private static void insertTugas(Connection con, LocalDate tgl, long shiftId, KategoriTugas kategori,
                                    long produkId, BigDecimal target, BigDecimal aktual,
                                    BigDecimal diterima, StatusTugas status, long staffId,
                                    String catatan, LocalDateTime createdAt) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("""
                INSERT INTO jadwal_tugas
                    (tanggal, shift_id, kategori, produk_id, qty_target, qty_aktual, qty_diterima,
                     status, staff_id, catatan, created_at)
                VALUES (?,?,?,?,?,?,?,?,?,?,?)
                """)) {
            ps.setDate(1, Date.valueOf(tgl));
            ps.setLong(2, shiftId);
            ps.setString(3, kategori.name());
            ps.setLong(4, produkId);
            ps.setBigDecimal(5, target);
            ps.setBigDecimal(6, aktual);
            ps.setBigDecimal(7, diterima);
            ps.setString(8, status.name());
            ps.setLong(9, staffId);
            ps.setString(10, catatan);
            ps.setTimestamp(11, Timestamp.valueOf(createdAt));
            ps.executeUpdate();
        }
    }

    // ===================== MASTER DATA =====================

    private static long ensureUser(Connection con, String nama, String username,
                                   String role, String password) throws SQLException {
        Long id = cariId(con, "SELECT id FROM users WHERE username = ?", username);
        if (id != null) {
            return id;
        }
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO users (nama_lengkap, username, password_hash, role) VALUES (?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nama);
            ps.setString(2, username);
            ps.setString(3, PasswordUtil.hash(password));
            ps.setString(4, role);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    private static long ensureBahan(Connection con, String nama, Satuan satuan,
                                    String batasMin) throws SQLException {
        Long id = cariId(con, "SELECT id FROM bahan_baku WHERE nama = ?", nama);
        if (id != null) {
            return id;
        }
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO bahan_baku (nama, satuan, batas_min) VALUES (?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nama);
            ps.setString(2, satuan.name());
            ps.setBigDecimal(3, new BigDecimal(batasMin));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    private static long ensureProduk(Connection con, String nama, Satuan satuan,
                                     SumberProduk sumber, String batasMin) throws SQLException {
        Long id = cariId(con, "SELECT id FROM produk_jadi WHERE nama = ?", nama);
        if (id != null) {
            return id;
        }
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO produk_jadi (nama, satuan, sumber, batas_min) VALUES (?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nama);
            ps.setString(2, satuan.name());
            ps.setString(3, sumber.name());
            ps.setBigDecimal(4, new BigDecimal(batasMin));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    private static void ensureBom(Connection con, long produkId,
                                  Map<Long, BigDecimal> komponen) throws SQLException {
        Long bomId = cariId(con, "SELECT id FROM bom WHERE produk_id = ?", produkId);
        if (bomId == null) {
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO bom (produk_id) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, produkId);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    bomId = keys.getLong(1);
                }
            }
        }
        try (PreparedStatement ps = con.prepareStatement("""
                MERGE INTO komponen_bom (bom_id, bahan_baku_id, qty)
                KEY (bom_id, bahan_baku_id) VALUES (?,?,?)
                """)) {
            for (Map.Entry<Long, BigDecimal> e : komponen.entrySet()) {
                ps.setLong(1, bomId);
                ps.setLong(2, e.getKey());
                ps.setBigDecimal(3, e.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // ===================== UTIL =====================

    private static Map<Long, BigDecimal> komposisi(Object... pasangan) {
        Map<Long, BigDecimal> hasil = new LinkedHashMap<>();
        for (int i = 0; i < pasangan.length; i += 2) {
            hasil.put((Long) pasangan[i], new BigDecimal((String) pasangan[i + 1]));
        }
        return hasil;
    }

    private static List<Long> gabung(long[] a, long[] b) {
        List<Long> hasil = new ArrayList<>();
        for (long x : a) {
            hasil.add(x);
        }
        for (long x : b) {
            hasil.add(x);
        }
        return hasil;
    }

    private static String kunci(JenisInventaris jenis, long barangId) {
        return (jenis == JenisInventaris.BAHAN_BAKU ? "BB:" : "PJ:") + barangId;
    }

    private static BigDecimal stok(JenisInventaris jenis, long barangId) {
        return STOK.getOrDefault(kunci(jenis, barangId), BigDecimal.ZERO);
    }

    private static Map<Long, String> namaBarang(Connection con, String tabel) throws SQLException {
        Map<Long, String> hasil = new HashMap<>();
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, nama FROM " + tabel)) {
            while (rs.next()) {
                hasil.put(rs.getLong(1), rs.getString(2));
            }
        }
        return hasil;
    }

    private static long shiftId(Connection con, int urutan) throws SQLException {
        Long id = cariId(con, "SELECT id FROM shift WHERE urutan = ?", urutan);
        if (id == null) {
            throw new IllegalStateException("Shift dengan urutan " + urutan + " tidak ditemukan.");
        }
        return id;
    }

    private static Long cariId(Connection con, String sql, Object param) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        }
    }

    private static long count(Connection con, String tabel) throws SQLException {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + tabel)) {
            rs.next();
            return rs.getLong(1);
        }
    }
}
