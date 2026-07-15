package com.hakotjeria.tools;

import java.sql.Connection;

import org.h2.tools.Server;

import com.hakotjeria.config.DatabaseConfig;

/**
 * Membuka H2 Console (antarmuka web) yang langsung tersambung ke basis data
 * aplikasi di folder ./data. Dari sini Admin dapat menelaah dan mengubah data
 * secara independen: stock opname, riwayat opname, master data, BOM, mutasi
 * stok, jadwal tugas, dan pengguna.
 *
 * Karena koneksi memakai mode AUTO_SERVER, tool ini boleh dijalankan bersamaan
 * dengan aplikasi utama tanpa bentrok kunci file.
 *
 * Jalankan lewat Maven:
 *   mvn -q compile exec:java -Dexec.mainClass=com.hakotjeria.tools.H2ConsoleTool
 * atau lewat berkas buka-h2-console.bat (klik ganda) di root proyek.
 *
 * Console menutup ketika tombol "Disconnect" ditekan atau terminal diakhiri (Ctrl+C).
 */
public final class H2ConsoleTool {

    private H2ConsoleTool() {
    }

    public static void main(String[] args) throws Exception {
        // Pastikan skema & data default tersedia, meski dijalankan pada clone baru.
        DatabaseConfig.getInstance().initSchema();

        System.out.println("Membuka H2 Console untuk basis data data/hakotjeria_db ...");
        System.out.println("  URL      : " + DatabaseConfig.jdbcUrl());
        System.out.println("  Pengguna : " + DatabaseConfig.username() + "   (kata sandi kosong)");
        System.out.println("Browser akan terbuka dan langsung tersambung.");
        System.out.println("Tekan tombol \"Disconnect\" di halaman atau Ctrl+C di sini untuk menutup.");

        // startWebServer membuka browser yang langsung tersambung ke koneksi ini
        // dan memblokir hingga console ditutup, sehingga JVM tetap hidup.
        try (Connection con = DatabaseConfig.getInstance().getConnection()) {
            Server.startWebServer(con);
        }
    }
}
