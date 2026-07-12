# Hako Tjeria — Sistem Informasi Penjadwalan Kerja Harian & Stock Opname Bahan Baku

Aplikasi desktop untuk manajemen produksi dan mutasi *Dual-Inventory* pada Bakery Hako Tjeria,
dibangun sesuai dokumen **SRS** dan **SDD** (Kelompok 6 — The Milestone).

## Tech Stack

- **Java 17** (kompatibel Java 21+)
- **JavaFX 17** (FXML + Controller, Layered MVC)
- **Maven** (Standard Directory Layout)
- **H2 Embedded Database** — `jdbc:h2:~/hakotjeria_db`
- **Plain JDBC** (`java.sql.*`) — tanpa ORM/JPA/Hibernate
- **OpenPDF** untuk ekspor laporan PDF
- **CSS eksternal** (`app.css`)

Tanpa Spring, Hibernate/JPA, ORM, HikariCP, framework DI, maupun library ikon eksternal
(seluruh ikon digambar sebagai `SVGPath` internal).

## Cara Menjalankan

Butuh JDK 17+ dan Maven terpasang. Dari folder proyek:

```bash
mvn clean javafx:run
```

Alternatif (launcher non-modular, mis. dari IDE tanpa konfigurasi module-path):

```bash
mvn -q clean compile exec:java
```

Database, seluruh tabel, foreign key, dan data default (shift + akun) dibuat **otomatis**
saat aplikasi pertama dijalankan. File database tersimpan di home directory pengguna
(`~/hakotjeria_db.mv.db`).

Proyek dapat langsung dibuka di **IntelliJ IDEA**, **VS Code**, maupun **NetBeans**
(cukup buka `pom.xml` sebagai project Maven; seluruh dependency ter-resolve otomatis
dari Maven Central).

## Akun Default

| Peran      | Username     | Password        |
|------------|--------------|-----------------|
| Supervisor | `supervisor` | `supervisor123` |
| Staff      | `staff`      | `staff123`      |

Ganti/hapus akun default melalui menu **Manajemen Pengguna** (khusus Supervisor).

## Modul Backend Terisolasi (Admin / Tim Pengelola Sistem)

Sesuai **R13.6**, reset password dan buka kunci akun dilakukan lewat modul backend
terpisah dari antarmuka aplikasi (dijalankan dari terminal):

```bash
mvn -q compile exec:java -Dexec.mainClass=com.hakotjeria.tools.AdminTool -Dexec.args="list"
mvn -q compile exec:java -Dexec.mainClass=com.hakotjeria.tools.AdminTool -Dexec.args="reset-password supervisor SandiBaru1"
mvn -q compile exec:java -Dexec.mainClass=com.hakotjeria.tools.AdminTool -Dexec.args="unlock supervisor"
```

## Struktur Proyek (Layered MVC)

```
src/main/java/com/hakotjeria/
├── MainApp.java / Launcher.java     Entry point aplikasi
├── config/      DatabaseConfig      Koneksi H2 + inisialisasi skema & data default
├── model/       Entity + Enum       Sesuai class diagram SDD
├── repository/  JDBC murni          Hanya SQL/PreparedStatement (tanpa business rule)
├── service/     Business rule       Seluruh aturan bisnis SRS
├── controller/  Controller JavaFX    Hanya menangani UI (tanpa business rule)
├── util/        Helper              Session, Password, PDF, Format, Icons, dsb.
└── tools/       AdminTool           Modul backend terisolasi
src/main/resources/com/hakotjeria/
├── fxml/        Halaman FXML        Satu file per halaman + dialog
└── css/         app.css             Style global (btn-primary, card, table-view, dst.)
```

Pemisahan lapisan bersifat tegas: **Repository** hanya JDBC, **Service** memegang seluruh
aturan bisnis, **Controller** hanya menangani UI JavaFX.

## Cakupan Fungsional (UC-01 s/d UC-13)

Login multi-peran & lockout 5× (SR09) · Dashboard Supervisor/Staff · Manajemen Master Data
Dual-Inventory · Manajemen BOM · Jadwal Tugas Operasional per shift · Eksekusi Produksi
Internal (pemotongan BOM × Qty Aktual, transaksi atomik) · Eksekusi Pengambilan Eksternal ·
Mutasi Stok Manual · Riwayat Mutasi Stok (2 tab + filter + ekspor PDF) · Stock Opname
(input stok fisik, selisih otomatis, validasi, mutasi penyesuaian, penguncian shift) ·
Riwayat Stock Opname + ekspor PDF berkop · Manajemen Pengguna · Logout & Lupa Password.

Aturan bisnis penting yang ditegakkan: stok tidak boleh negatif (R06.3/R08.3), kalkulasi
memakai Qty Aktual bukan Qty Target (Kekangan 5), transaksi atomik untuk pemotongan bahan +
penambahan produk (R06.5), data riwayat & dokumen tervalidasi bersifat *Read-Only* permanen
(R09.5/R11.5/SR06), penguncian shift setelah opname disetujui (R10.6), stok akhir shift
otomatis menjadi stok awal shift berikutnya (R10.7), audit trail penginput/tanggal/shift (SR07).

---
© 2026 The Milestone — Kelompok 6.
"# HakoTjeria-New-D" 
