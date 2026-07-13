package com.hakotjeria.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.hakotjeria.util.PasswordUtil;

/**
 * Konfigurasi basis data H2 Embedded.
 * Bertanggung jawab membuka koneksi JDBC serta menginisialisasi skema
 * (tabel, foreign key, dan data default) saat aplikasi pertama dijalankan.
 */
public final class DatabaseConfig {

    /** Folder penyimpanan file basis data, relatif terhadap root proyek. */
    private static final String DB_DIR = "data";
    private static final String URL = "jdbc:h2:file:./data/hakotjeria_db";
    private static final String USERNAME = "sa";
    private static final String PASSWORD = "";

    private static final DatabaseConfig INSTANCE = new DatabaseConfig();

    private DatabaseConfig() {
        // Pastikan folder database ada agar berjalan pada clone baru sekalipun.
        try {
            Files.createDirectories(Path.of(DB_DIR));
        } catch (IOException e) {
            throw new IllegalStateException("Gagal membuat folder database '" + DB_DIR + "': " + e.getMessage(), e);
        }
    }

    public static DatabaseConfig getInstance() {
        return INSTANCE;
    }

    /** Membuka koneksi baru ke basis data embedded. */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }

    /**
     * Membuat seluruh tabel, relasi foreign key, dan data default
     * (shift kerja serta akun bawaan) apabila belum tersedia.
     */
    public void initSchema() {
        try (Connection con = getConnection(); Statement st = con.createStatement()) {

            st.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id                   IDENTITY PRIMARY KEY,
                    nama_lengkap         VARCHAR(120) NOT NULL,
                    username             VARCHAR(60)  NOT NULL UNIQUE,
                    password_hash        VARCHAR(200) NOT NULL,
                    role                 VARCHAR(20)  NOT NULL,
                    status_aktif         BOOLEAN      NOT NULL DEFAULT TRUE,
                    must_change_password BOOLEAN      NOT NULL DEFAULT FALSE,
                    failed_attempts      INT          NOT NULL DEFAULT 0,
                    locked_until         TIMESTAMP,
                    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS shift (
                    id          IDENTITY PRIMARY KEY,
                    nama_shift  VARCHAR(30) NOT NULL,
                    urutan      INT         NOT NULL,
                    jam_mulai   TIME        NOT NULL,
                    jam_selesai TIME        NOT NULL
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS bahan_baku (
                    id         IDENTITY PRIMARY KEY,
                    nama       VARCHAR(120)   NOT NULL UNIQUE,
                    satuan     VARCHAR(10)    NOT NULL,
                    batas_min  DECIMAL(19,3)  NOT NULL DEFAULT 0,
                    created_at TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS produk_jadi (
                    id         IDENTITY PRIMARY KEY,
                    nama       VARCHAR(120)   NOT NULL UNIQUE,
                    satuan     VARCHAR(10)    NOT NULL,
                    sumber     VARCHAR(20)    NOT NULL,
                    batas_min  DECIMAL(19,3)  NOT NULL DEFAULT 0,
                    created_at TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS bom (
                    id         IDENTITY PRIMARY KEY,
                    produk_id  BIGINT    NOT NULL UNIQUE,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_bom_produk FOREIGN KEY (produk_id) REFERENCES produk_jadi(id)
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS komponen_bom (
                    id            IDENTITY PRIMARY KEY,
                    bom_id        BIGINT        NOT NULL,
                    bahan_baku_id BIGINT        NOT NULL,
                    qty           DECIMAL(19,3) NOT NULL,
                    CONSTRAINT fk_komponen_bom FOREIGN KEY (bom_id) REFERENCES bom(id) ON DELETE CASCADE,
                    CONSTRAINT fk_komponen_bahan FOREIGN KEY (bahan_baku_id) REFERENCES bahan_baku(id),
                    CONSTRAINT uq_komponen UNIQUE (bom_id, bahan_baku_id)
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS jadwal_tugas (
                    id           IDENTITY PRIMARY KEY,
                    tanggal      DATE          NOT NULL,
                    shift_id     BIGINT        NOT NULL,
                    kategori     VARCHAR(30)   NOT NULL,
                    produk_id    BIGINT        NOT NULL,
                    qty_target   DECIMAL(19,3) NOT NULL,
                    qty_aktual   DECIMAL(19,3),
                    qty_diterima DECIMAL(19,3),
                    status       VARCHAR(30)   NOT NULL,
                    staff_id     BIGINT,
                    catatan      VARCHAR(500),
                    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_tugas_shift  FOREIGN KEY (shift_id)  REFERENCES shift(id),
                    CONSTRAINT fk_tugas_produk FOREIGN KEY (produk_id) REFERENCES produk_jadi(id),
                    CONSTRAINT fk_tugas_staff  FOREIGN KEY (staff_id)  REFERENCES users(id)
                )""");

            // Catatan: barang_id merujuk ke bahan_baku ATAU produk_jadi
            // sesuai kolom jenis_inventaris (arsitektur Dual-Inventory pada SDD),
            // sehingga validasi referensial dilakukan pada service layer.
            st.execute("""
                CREATE TABLE IF NOT EXISTS mutasi_stok (
                    id               IDENTITY PRIMARY KEY,
                    tanggal          DATE          NOT NULL,
                    shift_id         BIGINT        NOT NULL,
                    jenis_inventaris VARCHAR(20)   NOT NULL,
                    barang_id        BIGINT        NOT NULL,
                    jenis            VARCHAR(5)    NOT NULL,
                    qty              DECIMAL(19,3) NOT NULL,
                    keterangan       VARCHAR(500)  NOT NULL,
                    created_by       BIGINT,
                    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_mutasi_shift FOREIGN KEY (shift_id)   REFERENCES shift(id),
                    CONSTRAINT fk_mutasi_user  FOREIGN KEY (created_by) REFERENCES users(id)
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS stock_opname (
                    id               IDENTITY PRIMARY KEY,
                    kode             VARCHAR(40)  NOT NULL UNIQUE,
                    tanggal          DATE         NOT NULL,
                    shift_id         BIGINT       NOT NULL,
                    status           VARCHAR(30)  NOT NULL,
                    input_by         BIGINT,
                    input_at         TIMESTAMP,
                    valid_by         BIGINT,
                    valid_at         TIMESTAMP,
                    catatan_validasi VARCHAR(500),
                    CONSTRAINT fk_opname_shift    FOREIGN KEY (shift_id) REFERENCES shift(id),
                    CONSTRAINT fk_opname_inputer  FOREIGN KEY (input_by) REFERENCES users(id),
                    CONSTRAINT fk_opname_validator FOREIGN KEY (valid_by) REFERENCES users(id)
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS detail_stock_opname (
                    id               IDENTITY PRIMARY KEY,
                    opname_id        BIGINT        NOT NULL,
                    jenis_inventaris VARCHAR(20)   NOT NULL,
                    barang_id        BIGINT        NOT NULL,
                    stok_sistem      DECIMAL(19,3) NOT NULL,
                    stok_fisik       DECIMAL(19,3),
                    selisih          DECIMAL(19,3) NOT NULL,
                    CONSTRAINT fk_detail_opname FOREIGN KEY (opname_id) REFERENCES stock_opname(id) ON DELETE CASCADE
                )""");

            // Migrasi basis data lama: stok_fisik dahulu NOT NULL sehingga baris yang
            // belum diisi Staff otomatis tersimpan sebagai 0 (menutupi kolom yang
            // sebenarnya belum diaudit). Kolom dilonggarkan menjadi nullable agar
            // status "belum diisi" tetap tersimpan sebagai kosong, bukan 0.
            st.execute("ALTER TABLE detail_stock_opname ALTER COLUMN stok_fisik SET NULL");

            st.execute("CREATE INDEX IF NOT EXISTS idx_mutasi_tanggal ON mutasi_stok(tanggal, shift_id)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_mutasi_barang ON mutasi_stok(jenis_inventaris, barang_id)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_tugas_tanggal ON jadwal_tugas(tanggal, shift_id)");

            seedDefaults(con);
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal menginisialisasi basis data: " + e.getMessage(), e);
        }
    }

    /** Data default: dua shift kerja dan akun bawaan (supervisor & staff). */
    private void seedDefaults(Connection con) throws SQLException {
        if (isTableEmpty(con, "shift")) {
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO shift (nama_shift, urutan, jam_mulai, jam_selesai) VALUES (?,?,?,?)")) {
                ps.setString(1, "Shift 1");
                ps.setInt(2, 1);
                ps.setString(3, "06:00:00");
                ps.setString(4, "14:00:00");
                ps.addBatch();
                ps.setString(1, "Shift 2");
                ps.setInt(2, 2);
                ps.setString(3, "14:00:00");
                ps.setString(4, "22:00:00");
                ps.addBatch();
                ps.executeBatch();
            }
        }
        if (isTableEmpty(con, "users")) {
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO users (nama_lengkap, username, password_hash, role) VALUES (?,?,?,?)")) {
                ps.setString(1, "Budi Santoso");
                ps.setString(2, "supervisor");
                ps.setString(3, PasswordUtil.hash("supervisor123"));
                ps.setString(4, "SUPERVISOR");
                ps.addBatch();
                ps.setString(1, "Siti Aminah");
                ps.setString(2, "staff");
                ps.setString(3, PasswordUtil.hash("staff123"));
                ps.setString(4, "STAFF");
                ps.addBatch();
                ps.executeBatch();
            }
        }
    }

    private boolean isTableEmpty(Connection con, String table) throws SQLException {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getLong(1) == 0;
        }
    }
}
