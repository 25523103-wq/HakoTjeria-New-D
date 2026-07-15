@echo off
setlocal
rem ============================================================
rem  Buka H2 Console untuk basis data Hako Tjeria (folder .\data)
rem  Dapat dijalankan berdampingan dengan aplikasi berkat AUTO_SERVER.
rem ============================================================

rem Pindah ke folder skrip (root proyek) agar path relatif .\data benar.
cd /d "%~dp0"

rem Lokasi berkas H2 di repositori Maven lokal. Ubah bila versi berbeda.
set "H2_JAR=%USERPROFILE%\.m2\repository\com\h2database\h2\2.2.224\h2-2.2.224.jar"

if not exist "%H2_JAR%" (
    echo [!] Berkas H2 tidak ditemukan di:
    echo     %H2_JAR%
    echo     Pastikan dependensi Maven sudah terunduh, atau sesuaikan H2_JAR di skrip ini.
    pause
    exit /b 1
)

echo ============================================================
echo  H2 Console - Hako Tjeria
echo ------------------------------------------------------------
echo  Pada halaman login yang terbuka di browser, isikan:
echo.
echo    JDBC URL : jdbc:h2:file:./data/hakotjeria_db;AUTO_SERVER=TRUE
echo    User     : sa
echo    Password : (kosongkan)
echo.
echo  Boleh dibuka bersamaan dengan aplikasi yang sedang berjalan.
echo  Tutup jendela ini (Ctrl+C) untuk menghentikan console.
echo ============================================================

java -cp "%H2_JAR%" org.h2.tools.Console -web -browser

endlocal
