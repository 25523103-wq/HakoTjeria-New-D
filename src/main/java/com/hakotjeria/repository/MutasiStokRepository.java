package com.hakotjeria.repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.hakotjeria.config.DatabaseConfig;
import com.hakotjeria.model.JenisInventaris;
import com.hakotjeria.model.JenisMutasi;
import com.hakotjeria.model.MutasiFilter;
import com.hakotjeria.model.MutasiStok;
import com.hakotjeria.model.RingkasanMutasi;
import com.hakotjeria.model.Satuan;
import com.hakotjeria.model.StokItem;
import com.hakotjeria.model.SumberProduk;

/**
 * Akses data mutasi stok. Baris mutasi bersifat append-only:
 * repository ini sengaja tidak menyediakan UPDATE/DELETE (R09.5, SR06).
 */
public class MutasiStokRepository {

    /** Insert dalam transaksi milik pemanggil. */
    public void insert(Connection con, MutasiStok m) throws SQLException {
        String sql = """
                INSERT INTO mutasi_stok
                    (tanggal, shift_id, jenis_inventaris, barang_id, jenis, qty, keterangan, created_by)
                VALUES (?,?,?,?,?,?,?,?)
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(m.getTanggal()));
            ps.setLong(2, m.getShiftId());
            ps.setString(3, m.getJenisInventaris().name());
            ps.setLong(4, m.getBarangId());
            ps.setString(5, m.getJenis().name());
            ps.setBigDecimal(6, m.getQty());
            ps.setString(7, m.getKeterangan());
            if (m.getCreatedBy() == null) {
                ps.setNull(8, java.sql.Types.BIGINT);
            } else {
                ps.setLong(8, m.getCreatedBy());
            }
            ps.executeUpdate();
        }
    }

    /** Insert dengan koneksi mandiri (satu mutasi tunggal). */
    public void insert(MutasiStok m) {
        try (Connection con = DatabaseConfig.getInstance().getConnection()) {
            insert(con, m);
        } catch (SQLException e) {
            throw new RepositoryException("Gagal mencatat mutasi stok", e);
        }
    }

    /** Stok terkini sebuah barang = total IN - total OUT. */
    public BigDecimal hitungStok(JenisInventaris jenis, long barangId) {
        try (Connection con = DatabaseConfig.getInstance().getConnection()) {
            return hitungStok(con, jenis, barangId);
        } catch (SQLException e) {
            throw new RepositoryException("Gagal menghitung stok", e);
        }
    }

    /** Versi transaksional untuk verifikasi kecukupan stok di dalam transaksi. */
    public BigDecimal hitungStok(Connection con, JenisInventaris jenis, long barangId) throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(CASE WHEN jenis = 'IN' THEN qty ELSE -qty END), 0)
                FROM mutasi_stok
                WHERE jenis_inventaris = ? AND barang_id = ?
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, jenis.name());
            ps.setLong(2, barangId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1);
            }
        }
    }

    /** Apakah barang sudah memiliki riwayat mutasi (menentukan kunci edit/hapus master data). */
    public boolean adaMutasi(JenisInventaris jenis, long barangId) {
        String sql = "SELECT COUNT(*) FROM mutasi_stok WHERE jenis_inventaris = ? AND barang_id = ?";
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, jenis.name());
            ps.setLong(2, barangId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1) > 0;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Gagal memeriksa riwayat mutasi", e);
        }
    }

    /** Riwayat mutasi terfilter, terurut dari kejadian teranyar (R09.3, R09.4). */
    public List<MutasiStok> findByFilter(MutasiFilter f) {
        StringBuilder sql = new StringBuilder("""
                SELECT m.id, m.tanggal, m.shift_id, m.jenis_inventaris, m.barang_id, m.jenis,
                       m.qty, m.keterangan, m.created_by, m.created_at,
                       s.nama_shift, u.nama_lengkap AS nama_penginput,
                       CASE WHEN m.jenis_inventaris = 'BAHAN_BAKU' THEN bb.nama ELSE pj.nama END AS nama_barang,
                       CASE WHEN m.jenis_inventaris = 'BAHAN_BAKU' THEN bb.satuan ELSE pj.satuan END AS satuan_barang
                FROM mutasi_stok m
                JOIN shift s ON s.id = m.shift_id
                LEFT JOIN users u ON u.id = m.created_by
                LEFT JOIN bahan_baku bb ON m.jenis_inventaris = 'BAHAN_BAKU' AND bb.id = m.barang_id
                LEFT JOIN produk_jadi pj ON m.jenis_inventaris = 'PRODUK_JADI' AND pj.id = m.barang_id
                WHERE 1=1
                """);
        List<Object> params = new ArrayList<>();
        appendConditions(sql, params, f);
        sql.append(" ORDER BY m.tanggal DESC, m.created_at DESC, m.id DESC");

        List<MutasiStok> result = new ArrayList<>();
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RepositoryException("Gagal memuat riwayat mutasi", e);
        }
    }

    /**
     * Ringkasan akumulasi periode terfilter (R09.6).
     * Stok Awal = akumulasi seluruh mutasi sebelum tanggal awal filter.
     */
    public RingkasanMutasi ringkasan(MutasiFilter f) {
        BigDecimal stokAwal = BigDecimal.ZERO;
        if (f.getDariTanggal() != null) {
            StringBuilder awalSql = new StringBuilder("""
                    SELECT COALESCE(SUM(CASE WHEN m.jenis = 'IN' THEN m.qty ELSE -m.qty END), 0)
                    FROM mutasi_stok m WHERE m.tanggal < ?
                    """);
            List<Object> awalParams = new ArrayList<>();
            awalParams.add(Date.valueOf(f.getDariTanggal()));
            if (f.getJenisInventaris() != null) {
                awalSql.append(" AND m.jenis_inventaris = ?");
                awalParams.add(f.getJenisInventaris().name());
            }
            if (f.getBarangId() != null) {
                awalSql.append(" AND m.barang_id = ?");
                awalParams.add(f.getBarangId());
            }
            stokAwal = queryScalar(awalSql.toString(), awalParams);
        }

        StringBuilder inSql = new StringBuilder(
                "SELECT COALESCE(SUM(m.qty), 0) FROM mutasi_stok m WHERE m.jenis = 'IN'");
        StringBuilder outSql = new StringBuilder(
                "SELECT COALESCE(SUM(m.qty), 0) FROM mutasi_stok m WHERE m.jenis = 'OUT'");
        List<Object> inParams = new ArrayList<>();
        List<Object> outParams = new ArrayList<>();
        MutasiFilter tanpaJenis = copyTanpaJenisMutasi(f);
        appendConditions(inSql, inParams, tanpaJenis);
        appendConditions(outSql, outParams, tanpaJenis);

        BigDecimal totalIn = queryScalar(inSql.toString(), inParams);
        BigDecimal totalOut = queryScalar(outSql.toString(), outParams);
        return new RingkasanMutasi(stokAwal, totalIn, totalOut);
    }

    /** Aktivitas mutasi terbaru untuk Dashboard (R02.3). */
    public List<MutasiStok> findTerbaru(int limit) {
        MutasiFilter f = new MutasiFilter();
        List<MutasiStok> all = findByFilter(f);
        return all.size() <= limit ? all : all.subList(0, limit);
    }

    /** Daftar stok terkini seluruh barang pada satu kategori inventaris. */
    public List<StokItem> findStokSemua(JenisInventaris jenis) {
        boolean bahan = jenis == JenisInventaris.BAHAN_BAKU;
        String table = bahan ? "bahan_baku" : "produk_jadi";
        String sumberCol = bahan ? "" : ", b.sumber";
        String sql = """
                SELECT b.id, b.nama, b.satuan, b.batas_min%s,
                       COALESCE(SUM(CASE WHEN m.jenis = 'IN' THEN m.qty
                                         WHEN m.jenis = 'OUT' THEN -m.qty END), 0) AS stok
                FROM %s b
                LEFT JOIN mutasi_stok m ON m.jenis_inventaris = ? AND m.barang_id = b.id
                GROUP BY b.id, b.nama, b.satuan, b.batas_min%s
                ORDER BY b.nama
                """.formatted(sumberCol, table, sumberCol);
        List<StokItem> result = new ArrayList<>();
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, jenis.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StokItem item = new StokItem();
                    item.setJenisInventaris(jenis);
                    item.setBarangId(rs.getLong("id"));
                    item.setNama(rs.getString("nama"));
                    item.setSatuan(Satuan.valueOf(rs.getString("satuan")));
                    item.setBatasMin(rs.getBigDecimal("batas_min"));
                    item.setStok(rs.getBigDecimal("stok"));
                    if (!bahan) {
                        item.setSumber(SumberProduk.valueOf(rs.getString("sumber")));
                    }
                    result.add(item);
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RepositoryException("Gagal memuat stok " + jenis.getLabel(), e);
        }
    }

    // ===================== HELPER =====================

    private void appendConditions(StringBuilder sql, List<Object> params, MutasiFilter f) {
        if (f.getJenisInventaris() != null) {
            sql.append(" AND m.jenis_inventaris = ?");
            params.add(f.getJenisInventaris().name());
        }
        if (f.getDariTanggal() != null) {
            sql.append(" AND m.tanggal >= ?");
            params.add(Date.valueOf(f.getDariTanggal()));
        }
        if (f.getSampaiTanggal() != null) {
            sql.append(" AND m.tanggal <= ?");
            params.add(Date.valueOf(f.getSampaiTanggal()));
        }
        if (f.getShiftId() != null) {
            sql.append(" AND m.shift_id = ?");
            params.add(f.getShiftId());
        }
        if (f.getJenis() != null) {
            sql.append(" AND m.jenis = ?");
            params.add(f.getJenis().name());
        }
        if (f.getBarangId() != null) {
            sql.append(" AND m.barang_id = ?");
            params.add(f.getBarangId());
        }
    }

    private MutasiFilter copyTanpaJenisMutasi(MutasiFilter f) {
        MutasiFilter c = new MutasiFilter();
        c.setJenisInventaris(f.getJenisInventaris());
        c.setDariTanggal(f.getDariTanggal());
        c.setSampaiTanggal(f.getSampaiTanggal());
        c.setShiftId(f.getShiftId());
        c.setBarangId(f.getBarangId());
        return c;
    }

    private BigDecimal queryScalar(String sql, List<Object> params) {
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                BigDecimal value = rs.getBigDecimal(1);
                return value == null ? BigDecimal.ZERO : value;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Gagal menghitung ringkasan mutasi", e);
        }
    }

    private void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    private MutasiStok map(ResultSet rs) throws SQLException {
        MutasiStok m = new MutasiStok();
        m.setId(rs.getLong("id"));
        m.setTanggal(rs.getDate("tanggal").toLocalDate());
        m.setShiftId(rs.getLong("shift_id"));
        m.setJenisInventaris(JenisInventaris.valueOf(rs.getString("jenis_inventaris")));
        m.setBarangId(rs.getLong("barang_id"));
        m.setJenis(JenisMutasi.valueOf(rs.getString("jenis")));
        m.setQty(rs.getBigDecimal("qty"));
        m.setKeterangan(rs.getString("keterangan"));
        long createdBy = rs.getLong("created_by");
        m.setCreatedBy(rs.wasNull() ? null : createdBy);
        Timestamp created = rs.getTimestamp("created_at");
        m.setCreatedAt(created == null ? null : created.toLocalDateTime());
        m.setNamaShift(rs.getString("nama_shift"));
        m.setNamaPenginput(rs.getString("nama_penginput"));
        m.setNamaBarang(rs.getString("nama_barang"));
        String satuan = rs.getString("satuan_barang");
        m.setSatuanBarang(satuan == null ? null : Satuan.valueOf(satuan));
        return m;
    }
}
