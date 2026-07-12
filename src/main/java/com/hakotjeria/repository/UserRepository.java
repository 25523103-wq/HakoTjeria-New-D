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
import com.hakotjeria.model.Role;
import com.hakotjeria.model.User;

/** Akses data tabel users. */
public class UserRepository {

    private static final String BASE_SELECT = """
            SELECT id, nama_lengkap, username, password_hash, role, status_aktif,
                   must_change_password, failed_attempts, locked_until, created_at
            FROM users
            """;

    public Optional<User> findByUsername(String username) {
        String sql = BASE_SELECT + " WHERE LOWER(username) = LOWER(?)";
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RepositoryException("Gagal mencari pengguna", e);
        }
    }

    public Optional<User> findById(long id) {
        String sql = BASE_SELECT + " WHERE id = ?";
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RepositoryException("Gagal mencari pengguna", e);
        }
    }

    public List<User> findAll() {
        String sql = BASE_SELECT + " ORDER BY nama_lengkap";
        List<User> result = new ArrayList<>();
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(map(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RepositoryException("Gagal memuat daftar pengguna", e);
        }
    }

    public List<User> findStaffAktif() {
        String sql = BASE_SELECT + " WHERE role = 'STAFF' AND status_aktif = TRUE ORDER BY nama_lengkap";
        List<User> result = new ArrayList<>();
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(map(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RepositoryException("Gagal memuat daftar staff", e);
        }
    }

    public boolean existsUsername(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE LOWER(username) = LOWER(?)";
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1) > 0;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Gagal memeriksa keunikan username", e);
        }
    }

    public long save(User user) {
        String sql = "INSERT INTO users (nama_lengkap, username, password_hash, role, status_aktif) VALUES (?,?,?,?,?)";
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getNamaLengkap());
            ps.setString(2, user.getUsername());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getRole().name());
            ps.setBoolean(5, user.isStatusAktif());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        } catch (SQLException e) {
            throw new RepositoryException("Gagal menyimpan pengguna", e);
        }
    }

    public void update(User user) {
        String sql = "UPDATE users SET nama_lengkap = ?, role = ?, status_aktif = ? WHERE id = ?";
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, user.getNamaLengkap());
            ps.setString(2, user.getRole().name());
            ps.setBoolean(3, user.isStatusAktif());
            ps.setLong(4, user.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Gagal memperbarui pengguna", e);
        }
    }

    /** Memutus hak otentikasi tanpa menghapus identitas historis (R12.6). */
    public void nonaktifkan(long id) {
        String sql = "UPDATE users SET status_aktif = FALSE WHERE id = ?";
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Gagal menonaktifkan pengguna", e);
        }
    }

    public void updatePassword(long id, String passwordHash, boolean mustChange) {
        String sql = """
                UPDATE users SET password_hash = ?, must_change_password = ?,
                       failed_attempts = 0, locked_until = NULL
                WHERE id = ?
                """;
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setBoolean(2, mustChange);
            ps.setLong(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Gagal memperbarui password", e);
        }
    }

    public void updateLoginState(long id, int failedAttempts, java.time.LocalDateTime lockedUntil) {
        String sql = "UPDATE users SET failed_attempts = ?, locked_until = ? WHERE id = ?";
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, failedAttempts);
            ps.setTimestamp(2, lockedUntil == null ? null : Timestamp.valueOf(lockedUntil));
            ps.setLong(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Gagal memperbarui status login", e);
        }
    }

    /** Membuka kunci akun yang dibekukan (dipakai modul backend terisolasi / R13.6). */
    public void unlock(String username) {
        String sql = "UPDATE users SET failed_attempts = 0, locked_until = NULL, status_aktif = TRUE " +
                "WHERE LOWER(username) = LOWER(?)";
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Gagal membuka kunci akun", e);
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setNamaLengkap(rs.getString("nama_lengkap"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(Role.valueOf(rs.getString("role")));
        u.setStatusAktif(rs.getBoolean("status_aktif"));
        u.setMustChangePassword(rs.getBoolean("must_change_password"));
        u.setFailedAttempts(rs.getInt("failed_attempts"));
        Timestamp locked = rs.getTimestamp("locked_until");
        u.setLockedUntil(locked == null ? null : locked.toLocalDateTime());
        Timestamp created = rs.getTimestamp("created_at");
        u.setCreatedAt(created == null ? null : created.toLocalDateTime());
        return u;
    }
}
