package com.hakotjeria.tools;

import com.hakotjeria.config.DatabaseConfig;
import com.hakotjeria.model.User;
import com.hakotjeria.repository.UserRepository;
import com.hakotjeria.util.PasswordUtil;

/**
 * Modul backend terisolasi untuk Admin / Tim Pengelola Sistem (R13.6).
 * Dijalankan dari terminal, terpisah dari antarmuka aplikasi, untuk:
 * - reset password pengguna (termasuk Supervisor yang lupa kata sandi)
 * - membuka akun yang dibekukan
 *
 * Contoh:
 *   mvn -q compile exec:java -Dexec.mainClass=com.hakotjeria.tools.AdminTool -Dexec.args="list"
 *   mvn -q compile exec:java -Dexec.mainClass=com.hakotjeria.tools.AdminTool -Dexec.args="reset-password supervisor RahasiaBaru1"
 *   mvn -q compile exec:java -Dexec.mainClass=com.hakotjeria.tools.AdminTool -Dexec.args="unlock supervisor"
 */
public final class AdminTool {

    private AdminTool() {
    }

    public static void main(String[] args) {
        DatabaseConfig.getInstance().initSchema();
        UserRepository repo = new UserRepository();

        if (args.length == 0) {
            printUsage();
            return;
        }
        switch (args[0]) {
            case "list" -> {
                System.out.printf("%-4s %-25s %-15s %-12s %-8s%n", "ID", "Nama", "Username", "Role", "Aktif");
                for (User u : repo.findAll()) {
                    System.out.printf("%-4d %-25s %-15s %-12s %-8s%n",
                            u.getId(), u.getNamaLengkap(), u.getUsername(), u.getRole(), u.isStatusAktif());
                }
            }
            case "reset-password" -> {
                if (args.length < 3) {
                    printUsage();
                    return;
                }
                User user = repo.findByUsername(args[1]).orElse(null);
                if (user == null) {
                    System.out.println("Pengguna tidak ditemukan: " + args[1]);
                    return;
                }
                if (args[2].length() < 6) {
                    System.out.println("Password baru minimal 6 karakter.");
                    return;
                }
                // Setelah reset, sistem memaksa penggantian sandi pada login pertama (UC-13).
                repo.updatePassword(user.getId(), PasswordUtil.hash(args[2]), true);
                repo.unlock(args[1]);
                System.out.println("Password " + args[1] + " berhasil direset. "
                        + "Pengguna wajib mengganti sandi saat login berikutnya.");
            }
            case "unlock" -> {
                if (args.length < 2) {
                    printUsage();
                    return;
                }
                repo.unlock(args[1]);
                System.out.println("Akun " + args[1] + " telah dibuka dan diaktifkan kembali.");
            }
            default -> printUsage();
        }
    }

    private static void printUsage() {
        System.out.println("""
                AdminTool - Modul backend terisolasi Hako Tjeria
                Perintah:
                  list                                  Menampilkan seluruh akun
                  reset-password <username> <baru>      Reset password + buka kunci akun
                  unlock <username>                     Membuka akun yang dibekukan
                """);
    }
}
