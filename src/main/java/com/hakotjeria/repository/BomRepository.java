package com.hakotjeria.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import com.hakotjeria.config.DatabaseConfig;
import com.hakotjeria.model.Bom;
import com.hakotjeria.model.KomponenBom;
import com.hakotjeria.model.Satuan;

/** Akses data BOM (resep digital) beserta komponennya. */
public class BomRepository {

    public Optional<Bom> findByProdukId(long produkId) {
        String sqlHeader = "SELECT id, produk_id, created_at FROM bom WHERE produk_id = ?";
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sqlHeader)) {
            ps.setLong(1, produkId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Bom bom = new Bom();
                bom.setId(rs.getLong("id"));
                bom.setProdukId(rs.getLong("produk_id"));
                Timestamp created = rs.getTimestamp("created_at");
                bom.setCreatedAt(created == null ? null : created.toLocalDateTime());
                bom.getKomponen().addAll(findKomponen(con, bom.getId()));
                return Optional.of(bom);
            }
        } catch (SQLException e) {
            throw new RepositoryException("Gagal memuat BOM", e);
        }
    }

    private List<KomponenBom> findKomponen(Connection con, long bomId) throws SQLException {
        String sql = """
                SELECT k.id, k.bom_id, k.bahan_baku_id, k.qty, b.nama AS nama_bahan, b.satuan AS satuan_bahan
                FROM komponen_bom k
                JOIN bahan_baku b ON b.id = k.bahan_baku_id
                WHERE k.bom_id = ?
                ORDER BY b.nama
                """;
        List<KomponenBom> result = new java.util.ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, bomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    KomponenBom k = new KomponenBom();
                    k.setId(rs.getLong("id"));
                    k.setBomId(rs.getLong("bom_id"));
                    k.setBahanBakuId(rs.getLong("bahan_baku_id"));
                    k.setQty(rs.getBigDecimal("qty"));
                    k.setNamaBahan(rs.getString("nama_bahan"));
                    k.setSatuanBahan(Satuan.valueOf(rs.getString("satuan_bahan")));
                    result.add(k);
                }
            }
        }
        return result;
    }

    /**
     * Menyimpan (membuat/menimpa) resep BOM sebuah produk internal
     * dalam satu transaksi: header dibuat bila belum ada, komponen ditulis ulang.
     */
    public void save(long produkId, List<KomponenBom> komponen) {
        try (Connection con = DatabaseConfig.getInstance().getConnection()) {
            con.setAutoCommit(false);
            try {
                long bomId = findOrCreateHeader(con, produkId);
                try (PreparedStatement del = con.prepareStatement("DELETE FROM komponen_bom WHERE bom_id = ?")) {
                    del.setLong(1, bomId);
                    del.executeUpdate();
                }
                try (PreparedStatement ins = con.prepareStatement(
                        "INSERT INTO komponen_bom (bom_id, bahan_baku_id, qty) VALUES (?,?,?)")) {
                    for (KomponenBom k : komponen) {
                        ins.setLong(1, bomId);
                        ins.setLong(2, k.getBahanBakuId());
                        ins.setBigDecimal(3, k.getQty());
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
            throw new RepositoryException("Gagal menyimpan BOM", e);
        }
    }

    public void deleteByProdukId(long produkId) {
        String sql = "DELETE FROM bom WHERE produk_id = ?";
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, produkId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Gagal menghapus BOM", e);
        }
    }

    public boolean hasBomWithKomponen(long produkId) {
        String sql = """
                SELECT COUNT(*) FROM komponen_bom k
                JOIN bom b ON b.id = k.bom_id
                WHERE b.produk_id = ?
                """;
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, produkId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1) > 0;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Gagal memeriksa BOM", e);
        }
    }

    private long findOrCreateHeader(Connection con, long produkId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT id FROM bom WHERE produk_id = ?")) {
            ps.setLong(1, produkId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO bom (produk_id) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, produkId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }
}
