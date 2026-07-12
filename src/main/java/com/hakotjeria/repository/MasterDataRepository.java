package com.hakotjeria.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.hakotjeria.config.DatabaseConfig;
import com.hakotjeria.model.BahanBaku;
import com.hakotjeria.model.ProdukJadi;
import com.hakotjeria.model.Satuan;
import com.hakotjeria.model.SumberProduk;

/** Akses data master Bahan Baku dan Produk Jadi (Dual-Inventory). */
public class MasterDataRepository {

    // ===================== BAHAN BAKU =====================

    public List<BahanBaku> findBahanBaku(String cari) {
        String sql = "SELECT id, nama, satuan, batas_min, created_at FROM bahan_baku " +
                "WHERE (? IS NULL OR LOWER(nama) LIKE ?) ORDER BY nama";
        List<BahanBaku> result = new ArrayList<>();
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            applySearch(ps, cari);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapBahan(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RepositoryException("Gagal memuat data Bahan Baku", e);
        }
    }

    public Optional<BahanBaku> findBahanBakuById(long id) {
        String sql = "SELECT id, nama, satuan, batas_min, created_at FROM bahan_baku WHERE id = ?";
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapBahan(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RepositoryException("Gagal mencari Bahan Baku", e);
        }
    }

    public boolean existsNamaBahan(String nama, Long excludeId) {
        return existsNama("bahan_baku", nama, excludeId);
    }

    public long saveBahanBaku(BahanBaku b) {
        String sql = "INSERT INTO bahan_baku (nama, satuan, batas_min) VALUES (?,?,?)";
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, b.getNama());
            ps.setString(2, b.getSatuan().name());
            ps.setBigDecimal(3, b.getBatasMin());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        } catch (SQLException e) {
            throw new RepositoryException("Gagal menyimpan Bahan Baku", e);
        }
    }

    public void updateBahanBaku(BahanBaku b) {
        String sql = "UPDATE bahan_baku SET nama = ?, satuan = ?, batas_min = ? WHERE id = ?";
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, b.getNama());
            ps.setString(2, b.getSatuan().name());
            ps.setBigDecimal(3, b.getBatasMin());
            ps.setLong(4, b.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Gagal memperbarui Bahan Baku", e);
        }
    }

    public void deleteBahanBaku(long id) {
        delete("bahan_baku", id);
    }

    /** Bahan dipakai sebagai komponen BOM (kendala referensial). */
    public boolean bahanDipakaiBom(long bahanBakuId) {
        String sql = "SELECT COUNT(*) FROM komponen_bom WHERE bahan_baku_id = ?";
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, bahanBakuId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1) > 0;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Gagal memeriksa relasi BOM", e);
        }
    }

    // ===================== PRODUK JADI =====================

    public List<ProdukJadi> findProdukJadi(String cari) {
        String sql = "SELECT id, nama, satuan, sumber, batas_min, created_at FROM produk_jadi " +
                "WHERE (? IS NULL OR LOWER(nama) LIKE ?) ORDER BY nama";
        List<ProdukJadi> result = new ArrayList<>();
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            applySearch(ps, cari);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapProduk(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RepositoryException("Gagal memuat data Produk Jadi", e);
        }
    }

    public Optional<ProdukJadi> findProdukJadiById(long id) {
        String sql = "SELECT id, nama, satuan, sumber, batas_min, created_at FROM produk_jadi WHERE id = ?";
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapProduk(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RepositoryException("Gagal mencari Produk Jadi", e);
        }
    }

    public boolean existsNamaProduk(String nama, Long excludeId) {
        return existsNama("produk_jadi", nama, excludeId);
    }

    public long saveProdukJadi(ProdukJadi p) {
        String sql = "INSERT INTO produk_jadi (nama, satuan, sumber, batas_min) VALUES (?,?,?,?)";
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getNama());
            ps.setString(2, p.getSatuan().name());
            ps.setString(3, p.getSumber().name());
            ps.setBigDecimal(4, p.getBatasMin());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        } catch (SQLException e) {
            throw new RepositoryException("Gagal menyimpan Produk Jadi", e);
        }
    }

    public void updateProdukJadi(ProdukJadi p) {
        String sql = "UPDATE produk_jadi SET nama = ?, satuan = ?, sumber = ?, batas_min = ? WHERE id = ?";
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, p.getNama());
            ps.setString(2, p.getSatuan().name());
            ps.setString(3, p.getSumber().name());
            ps.setBigDecimal(4, p.getBatasMin());
            ps.setLong(5, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Gagal memperbarui Produk Jadi", e);
        }
    }

    public void deleteProdukJadi(long id) {
        delete("produk_jadi", id);
    }

    // ===================== HELPER =====================

    private void applySearch(PreparedStatement ps, String cari) throws SQLException {
        if (cari == null || cari.isBlank()) {
            ps.setNull(1, java.sql.Types.VARCHAR);
            ps.setNull(2, java.sql.Types.VARCHAR);
        } else {
            String like = "%" + cari.trim().toLowerCase() + "%";
            ps.setString(1, like);
            ps.setString(2, like);
        }
    }

    private boolean existsNama(String table, String nama, Long excludeId) {
        String sql = "SELECT COUNT(*) FROM " + table + " WHERE LOWER(nama) = LOWER(?) AND (? IS NULL OR id <> ?)";
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nama);
            if (excludeId == null) {
                ps.setNull(2, java.sql.Types.BIGINT);
                ps.setNull(3, java.sql.Types.BIGINT);
            } else {
                ps.setLong(2, excludeId);
                ps.setLong(3, excludeId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1) > 0;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Gagal memeriksa keunikan nama", e);
        }
    }

    private void delete(String table, long id) {
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM " + table + " WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Gagal menghapus data", e);
        }
    }

    private BahanBaku mapBahan(ResultSet rs) throws SQLException {
        BahanBaku b = new BahanBaku();
        b.setId(rs.getLong("id"));
        b.setNama(rs.getString("nama"));
        b.setSatuan(Satuan.valueOf(rs.getString("satuan")));
        b.setBatasMin(rs.getBigDecimal("batas_min"));
        Timestamp created = rs.getTimestamp("created_at");
        b.setCreatedAt(created == null ? null : created.toLocalDateTime());
        return b;
    }

    private ProdukJadi mapProduk(ResultSet rs) throws SQLException {
        ProdukJadi p = new ProdukJadi();
        p.setId(rs.getLong("id"));
        p.setNama(rs.getString("nama"));
        p.setSatuan(Satuan.valueOf(rs.getString("satuan")));
        p.setSumber(SumberProduk.valueOf(rs.getString("sumber")));
        p.setBatasMin(rs.getBigDecimal("batas_min"));
        Timestamp created = rs.getTimestamp("created_at");
        p.setCreatedAt(created == null ? null : created.toLocalDateTime());
        return p;
    }
}
