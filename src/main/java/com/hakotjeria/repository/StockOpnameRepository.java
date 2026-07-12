package com.hakotjeria.repository;

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
import java.util.List;
import java.util.Optional;

import com.hakotjeria.config.DatabaseConfig;
import com.hakotjeria.model.DetailStockOpname;
import com.hakotjeria.model.JenisInventaris;
import com.hakotjeria.model.Satuan;
import com.hakotjeria.model.StatusValidasi;
import com.hakotjeria.model.StockOpname;

/** Akses data dokumen Stock Opname beserta detailnya. */
public class StockOpnameRepository {

    private static final String BASE_SELECT = """
            SELECT o.id, o.kode, o.tanggal, o.shift_id, o.status, o.input_by, o.input_at,
                   o.valid_by, o.valid_at, o.catatan_validasi,
                   s.nama_shift, ui.nama_lengkap AS nama_penginput, uv.nama_lengkap AS nama_validator,
                   (SELECT COALESCE(SUM(d.selisih), 0) FROM detail_stock_opname d
                     WHERE d.opname_id = o.id) AS total_selisih
            FROM stock_opname o
            JOIN shift s ON s.id = o.shift_id
            LEFT JOIN users ui ON ui.id = o.input_by
            LEFT JOIN users uv ON uv.id = o.valid_by
            """;

    public Optional<StockOpname> findById(long id) {
        String sql = BASE_SELECT + " WHERE o.id = ?";
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                StockOpname o = map(rs);
                o.getDetail().addAll(findDetail(con, o.getId()));
                return Optional.of(o);
            }
        } catch (SQLException e) {
            throw new RepositoryException("Gagal memuat dokumen Stock Opname", e);
        }
    }

    /** Dokumen opname (bila ada) untuk satu tanggal + shift. */
    public Optional<StockOpname> findByTanggalShift(LocalDate tanggal, long shiftId) {
        String sql = BASE_SELECT + " WHERE o.tanggal = ? AND o.shift_id = ? ORDER BY o.id DESC";
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(tanggal));
            ps.setLong(2, shiftId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                StockOpname o = map(rs);
                o.getDetail().addAll(findDetail(con, o.getId()));
                return Optional.of(o);
            }
        } catch (SQLException e) {
            throw new RepositoryException("Gagal memuat dokumen Stock Opname", e);
        }
    }

    public List<StockOpname> findByStatus(StatusValidasi status) {
        String sql = BASE_SELECT + " WHERE o.status = ? ORDER BY o.tanggal DESC, o.id DESC";
        List<StockOpname> result = new ArrayList<>();
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RepositoryException("Gagal memuat daftar Stock Opname", e);
        }
    }

    /** Riwayat tervalidasi dengan filter tanggal dan shift (R11.1, R11.3). */
    public List<StockOpname> findRiwayat(LocalDate dari, LocalDate sampai, Long shiftId) {
        StringBuilder sql = new StringBuilder(BASE_SELECT + " WHERE o.status = 'TERVALIDASI'");
        List<Object> params = new ArrayList<>();
        if (dari != null) {
            sql.append(" AND o.tanggal >= ?");
            params.add(Date.valueOf(dari));
        }
        if (sampai != null) {
            sql.append(" AND o.tanggal <= ?");
            params.add(Date.valueOf(sampai));
        }
        if (shiftId != null) {
            sql.append(" AND o.shift_id = ?");
            params.add(shiftId);
        }
        sql.append(" ORDER BY o.tanggal DESC, o.id DESC");
        List<StockOpname> result = new ArrayList<>();
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RepositoryException("Gagal memuat riwayat Stock Opname", e);
        }
    }

    /** Shift terkunci permanen bila dokumen opname-nya telah tervalidasi (R10.6). */
    public boolean isShiftTerkunci(LocalDate tanggal, long shiftId) {
        String sql = "SELECT COUNT(*) FROM stock_opname WHERE tanggal = ? AND shift_id = ? AND status = 'TERVALIDASI'";
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(tanggal));
            ps.setLong(2, shiftId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1) > 0;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Gagal memeriksa kunci shift", e);
        }
    }

    public long insertHeader(StockOpname o) {
        String sql = """
                INSERT INTO stock_opname (kode, tanggal, shift_id, status, input_by, input_at)
                VALUES (?,?,?,?,?,?)
                """;
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, o.getKode());
            ps.setDate(2, Date.valueOf(o.getTanggal()));
            ps.setLong(3, o.getShiftId());
            ps.setString(4, o.getStatus().name());
            if (o.getInputBy() == null) {
                ps.setNull(5, java.sql.Types.BIGINT);
            } else {
                ps.setLong(5, o.getInputBy());
            }
            ps.setTimestamp(6, o.getInputAt() == null ? null : Timestamp.valueOf(o.getInputAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        } catch (SQLException e) {
            throw new RepositoryException("Gagal menyimpan dokumen Stock Opname", e);
        }
    }

    /** Menulis ulang seluruh detail dokumen (saat draf disimpan/diajukan). */
    public void replaceDetail(long opnameId, List<DetailStockOpname> detail) {
        try (Connection con = DatabaseConfig.getInstance().getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement del = con.prepareStatement(
                        "DELETE FROM detail_stock_opname WHERE opname_id = ?")) {
                    del.setLong(1, opnameId);
                    del.executeUpdate();
                }
                try (PreparedStatement ins = con.prepareStatement("""
                        INSERT INTO detail_stock_opname
                            (opname_id, jenis_inventaris, barang_id, stok_sistem, stok_fisik, selisih)
                        VALUES (?,?,?,?,?,?)
                        """)) {
                    for (DetailStockOpname d : detail) {
                        ins.setLong(1, opnameId);
                        ins.setString(2, d.getJenisInventaris().name());
                        ins.setLong(3, d.getBarangId());
                        ins.setBigDecimal(4, d.getStokSistem());
                        ins.setBigDecimal(5, d.getStokFisik());
                        ins.setBigDecimal(6, d.getSelisih());
                        ins.addBatch();
                    }
                    ins.executeBatch();
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RepositoryException("Gagal menyimpan detail Stock Opname", e);
        }
    }

    public void updateStatusPengajuan(long opnameId, StatusValidasi status, Long inputBy, LocalDateTime inputAt) {
        String sql = "UPDATE stock_opname SET status = ?, input_by = ?, input_at = ? WHERE id = ?";
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status.name());
            if (inputBy == null) {
                ps.setNull(2, java.sql.Types.BIGINT);
            } else {
                ps.setLong(2, inputBy);
            }
            ps.setTimestamp(3, inputAt == null ? null : Timestamp.valueOf(inputAt));
            ps.setLong(4, opnameId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Gagal memperbarui status Stock Opname", e);
        }
    }

    /** Update status validasi dalam transaksi milik pemanggil (persetujuan). */
    public void updateStatusValidasi(Connection con, long opnameId, StatusValidasi status,
                                     Long validBy, LocalDateTime validAt, String catatan) throws SQLException {
        String sql = "UPDATE stock_opname SET status = ?, valid_by = ?, valid_at = ?, catatan_validasi = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status.name());
            if (validBy == null) {
                ps.setNull(2, java.sql.Types.BIGINT);
            } else {
                ps.setLong(2, validBy);
            }
            ps.setTimestamp(3, validAt == null ? null : Timestamp.valueOf(validAt));
            ps.setString(4, catatan);
            ps.setLong(5, opnameId);
            ps.executeUpdate();
        }
    }

    /** Versi non-transaksional (penolakan). */
    public void updateStatusValidasi(long opnameId, StatusValidasi status,
                                     Long validBy, LocalDateTime validAt, String catatan) {
        try (Connection con = DatabaseConfig.getInstance().getConnection()) {
            updateStatusValidasi(con, opnameId, status, validBy, validAt, catatan);
        } catch (SQLException e) {
            throw new RepositoryException("Gagal memperbarui status validasi", e);
        }
    }

    public List<DetailStockOpname> findDetail(long opnameId) {
        try (Connection con = DatabaseConfig.getInstance().getConnection()) {
            return findDetail(con, opnameId);
        } catch (SQLException e) {
            throw new RepositoryException("Gagal memuat detail Stock Opname", e);
        }
    }

    private List<DetailStockOpname> findDetail(Connection con, long opnameId) throws SQLException {
        String sql = """
                SELECT d.id, d.opname_id, d.jenis_inventaris, d.barang_id,
                       d.stok_sistem, d.stok_fisik, d.selisih,
                       CASE WHEN d.jenis_inventaris = 'BAHAN_BAKU' THEN bb.nama ELSE pj.nama END AS nama_barang,
                       CASE WHEN d.jenis_inventaris = 'BAHAN_BAKU' THEN bb.satuan ELSE pj.satuan END AS satuan_barang
                FROM detail_stock_opname d
                LEFT JOIN bahan_baku bb ON d.jenis_inventaris = 'BAHAN_BAKU' AND bb.id = d.barang_id
                LEFT JOIN produk_jadi pj ON d.jenis_inventaris = 'PRODUK_JADI' AND pj.id = d.barang_id
                WHERE d.opname_id = ?
                ORDER BY d.jenis_inventaris, nama_barang
                """;
        List<DetailStockOpname> result = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, opnameId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DetailStockOpname d = new DetailStockOpname();
                    d.setId(rs.getLong("id"));
                    d.setOpnameId(rs.getLong("opname_id"));
                    d.setJenisInventaris(JenisInventaris.valueOf(rs.getString("jenis_inventaris")));
                    d.setBarangId(rs.getLong("barang_id"));
                    d.setStokSistem(rs.getBigDecimal("stok_sistem"));
                    d.setStokFisik(rs.getBigDecimal("stok_fisik"));
                    d.setSelisih(rs.getBigDecimal("selisih"));
                    d.setNamaBarang(rs.getString("nama_barang"));
                    String satuan = rs.getString("satuan_barang");
                    d.setSatuanBarang(satuan == null ? null : Satuan.valueOf(satuan));
                    result.add(d);
                }
            }
        }
        return result;
    }

    private StockOpname map(ResultSet rs) throws SQLException {
        StockOpname o = new StockOpname();
        o.setId(rs.getLong("id"));
        o.setKode(rs.getString("kode"));
        o.setTanggal(rs.getDate("tanggal").toLocalDate());
        o.setShiftId(rs.getLong("shift_id"));
        o.setStatus(StatusValidasi.valueOf(rs.getString("status")));
        long inputBy = rs.getLong("input_by");
        o.setInputBy(rs.wasNull() ? null : inputBy);
        Timestamp inputAt = rs.getTimestamp("input_at");
        o.setInputAt(inputAt == null ? null : inputAt.toLocalDateTime());
        long validBy = rs.getLong("valid_by");
        o.setValidBy(rs.wasNull() ? null : validBy);
        Timestamp validAt = rs.getTimestamp("valid_at");
        o.setValidAt(validAt == null ? null : validAt.toLocalDateTime());
        o.setCatatanValidasi(rs.getString("catatan_validasi"));
        o.setNamaShift(rs.getString("nama_shift"));
        o.setNamaPenginput(rs.getString("nama_penginput"));
        o.setNamaValidator(rs.getString("nama_validator"));
        o.setTotalSelisih(rs.getBigDecimal("total_selisih"));
        return o;
    }
}
