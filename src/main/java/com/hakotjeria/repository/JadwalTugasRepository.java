package com.hakotjeria.repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.hakotjeria.config.DatabaseConfig;
import com.hakotjeria.model.JadwalTugas;
import com.hakotjeria.model.KategoriTugas;
import com.hakotjeria.model.Satuan;
import com.hakotjeria.model.StatusTugas;
import com.hakotjeria.model.SumberProduk;

/** Akses data jadwal tugas operasional. */
public class JadwalTugasRepository {

    private static final String BASE_SELECT = """
            SELECT t.id, t.tanggal, t.shift_id, t.kategori, t.produk_id, t.qty_target,
                   t.qty_aktual, t.qty_diterima, t.status, t.staff_id, t.catatan, t.created_at,
                   p.nama AS nama_produk, p.satuan AS satuan_produk, p.sumber AS sumber_produk,
                   s.nama_shift, u.nama_lengkap AS nama_staff
            FROM jadwal_tugas t
            JOIN produk_jadi p ON p.id = t.produk_id
            JOIN shift s ON s.id = t.shift_id
            LEFT JOIN users u ON u.id = t.staff_id
            """;

    public List<JadwalTugas> findByTanggalShift(LocalDate tanggal, Long shiftId) {
        StringBuilder sql = new StringBuilder(BASE_SELECT + " WHERE t.tanggal = ?");
        if (shiftId != null) {
            sql.append(" AND t.shift_id = ?");
        }
        sql.append(" ORDER BY t.kategori, t.id");
        List<JadwalTugas> result = new ArrayList<>();
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            ps.setDate(1, Date.valueOf(tanggal));
            if (shiftId != null) {
                ps.setLong(2, shiftId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RepositoryException("Gagal memuat jadwal tugas", e);
        }
    }

    public Optional<JadwalTugas> findById(long id) {
        String sql = BASE_SELECT + " WHERE t.id = ?";
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RepositoryException("Gagal mencari tugas", e);
        }
    }

    public long save(JadwalTugas t) {
        String sql = """
                INSERT INTO jadwal_tugas
                    (tanggal, shift_id, kategori, produk_id, qty_target, status, staff_id, catatan)
                VALUES (?,?,?,?,?,?,?,?)
                """;
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setDate(1, Date.valueOf(t.getTanggal()));
            ps.setLong(2, t.getShiftId());
            ps.setString(3, t.getKategori().name());
            ps.setLong(4, t.getProdukId());
            ps.setBigDecimal(5, t.getQtyTarget());
            ps.setString(6, t.getStatus().name());
            setNullableLong(ps, 7, t.getStaffId());
            ps.setString(8, t.getCatatan());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        } catch (SQLException e) {
            throw new RepositoryException("Gagal menyimpan tugas", e);
        }
    }

    public void update(JadwalTugas t) {
        String sql = """
                UPDATE jadwal_tugas
                SET tanggal = ?, shift_id = ?, kategori = ?, produk_id = ?, qty_target = ?,
                    status = ?, staff_id = ?, catatan = ?
                WHERE id = ?
                """;
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(t.getTanggal()));
            ps.setLong(2, t.getShiftId());
            ps.setString(3, t.getKategori().name());
            ps.setLong(4, t.getProdukId());
            ps.setBigDecimal(5, t.getQtyTarget());
            ps.setString(6, t.getStatus().name());
            setNullableLong(ps, 7, t.getStaffId());
            ps.setString(8, t.getCatatan());
            ps.setLong(9, t.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Gagal memperbarui tugas", e);
        }
    }

    public void delete(long id) {
        String sql = "DELETE FROM jadwal_tugas WHERE id = ?";
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Gagal menghapus tugas", e);
        }
    }

    /** Update status final produksi dalam transaksi milik pemanggil (R06.5). */
    public void updateSelesai(Connection con, long id, BigDecimal qtyAktual) throws SQLException {
        String sql = "UPDATE jadwal_tugas SET status = ?, qty_aktual = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, StatusTugas.SELESAI.name());
            ps.setBigDecimal(2, qtyAktual);
            ps.setLong(3, id);
            ps.executeUpdate();
        }
    }

    /** Update status pengambilan eksternal dalam transaksi milik pemanggil. */
    public void updateSudahDiambil(Connection con, long id, BigDecimal qtyDiterima) throws SQLException {
        String sql = "UPDATE jadwal_tugas SET status = ?, qty_diterima = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, StatusTugas.SUDAH_DIAMBIL.name());
            ps.setBigDecimal(2, qtyDiterima);
            ps.setLong(3, id);
            ps.executeUpdate();
        }
    }

    public void updateTidakTerpenuhi(long id, String alasan) {
        String sql = "UPDATE jadwal_tugas SET status = ?, catatan = ? WHERE id = ?";
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, StatusTugas.TIDAK_TERPENUHI.name());
            ps.setString(2, alasan);
            ps.setLong(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Gagal memperbarui status tugas", e);
        }
    }

    /** Produk masih terikat tugas aktif (status belum final) — R03.5, R04.4. */
    public boolean adaTugasAktifUntukProduk(long produkId) {
        String sql = "SELECT COUNT(*) FROM jadwal_tugas WHERE produk_id = ? AND status IN (?,?)";
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, produkId);
            ps.setString(2, StatusTugas.BELUM_DIKERJAKAN.name());
            ps.setString(3, StatusTugas.BELUM_DIAMBIL.name());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1) > 0;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Gagal memeriksa tugas aktif", e);
        }
    }

    public boolean adaTugasUntukProduk(long produkId) {
        String sql = "SELECT COUNT(*) FROM jadwal_tugas WHERE produk_id = ?";
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, produkId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1) > 0;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Gagal memeriksa relasi tugas", e);
        }
    }

    private void setNullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.BIGINT);
        } else {
            ps.setLong(index, value);
        }
    }

    private JadwalTugas map(ResultSet rs) throws SQLException {
        JadwalTugas t = new JadwalTugas();
        t.setId(rs.getLong("id"));
        t.setTanggal(rs.getDate("tanggal").toLocalDate());
        t.setShiftId(rs.getLong("shift_id"));
        t.setKategori(KategoriTugas.valueOf(rs.getString("kategori")));
        t.setProdukId(rs.getLong("produk_id"));
        t.setQtyTarget(rs.getBigDecimal("qty_target"));
        t.setQtyAktual(rs.getBigDecimal("qty_aktual"));
        t.setQtyDiterima(rs.getBigDecimal("qty_diterima"));
        t.setStatus(StatusTugas.valueOf(rs.getString("status")));
        long staffId = rs.getLong("staff_id");
        t.setStaffId(rs.wasNull() ? null : staffId);
        t.setCatatan(rs.getString("catatan"));
        Timestamp created = rs.getTimestamp("created_at");
        t.setCreatedAt(created == null ? null : created.toLocalDateTime());
        t.setNamaProduk(rs.getString("nama_produk"));
        t.setSatuanProduk(Satuan.valueOf(rs.getString("satuan_produk")));
        t.setSumberProduk(SumberProduk.valueOf(rs.getString("sumber_produk")));
        t.setNamaShift(rs.getString("nama_shift"));
        t.setNamaStaff(rs.getString("nama_staff"));
        return t;
    }
}
