package com.hakotjeria.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import com.hakotjeria.model.Role;
import com.hakotjeria.model.User;
import com.hakotjeria.repository.ShiftRepository;
import com.hakotjeria.repository.UserRepository;
import com.hakotjeria.util.BusinessException;
import com.hakotjeria.util.PasswordUtil;
import com.hakotjeria.util.Session;

/**
 * Aturan bisnis autentikasi dan manajemen pengguna
 * (UC-01 Login, UC-12 Manajemen Pengguna, UC-13 Logout & Lupa Password).
 */
public class UserService {

    /** SR09: pembekuan sementara setelah lima kali gagal login berturut-turut. */
    private static final int MAX_PERCOBAAN_GAGAL = 5;
    private static final int DURASI_KUNCI_MENIT = 15;

    private final UserRepository userRepository = new UserRepository();
    private final ShiftRepository shiftRepository = new ShiftRepository();

    /**
     * Autentikasi pengguna (R01.1 - R01.4).
     * Mengisi Session dengan pengguna dan shift aktif bila berhasil.
     */
    public User login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new BusinessException("Username dan password wajib diisi.");
        }
        Optional<User> found = userRepository.findByUsername(username.trim());
        if (found.isEmpty()) {
            throw new BusinessException("Username atau password salah.");
        }
        User user = found.get();

        if (!user.isStatusAktif()) {
            throw new BusinessException("Akun ini telah dinonaktifkan. Hubungi Supervisor atau tim pengelola sistem.");
        }
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            long sisaMenit = Math.max(1, ChronoUnit.MINUTES.between(LocalDateTime.now(), user.getLockedUntil()) + 1);
            throw new BusinessException("Akun dibekukan sementara karena 5 kali percobaan login gagal. "
                    + "Coba lagi dalam " + sisaMenit + " menit atau hubungi tim pengelola sistem.");
        }

        if (!PasswordUtil.verify(password, user.getPasswordHash())) {
            int gagal = user.getFailedAttempts() + 1;
            LocalDateTime kunci = null;
            if (gagal >= MAX_PERCOBAAN_GAGAL) {
                kunci = LocalDateTime.now().plusMinutes(DURASI_KUNCI_MENIT);
                gagal = 0;
            }
            userRepository.updateLoginState(user.getId(), gagal, kunci);
            if (kunci != null) {
                throw new BusinessException("Akun dibekukan sementara selama " + DURASI_KUNCI_MENIT
                        + " menit karena 5 kali percobaan login gagal.");
            }
            throw new BusinessException("Username atau password salah.");
        }

        userRepository.updateLoginState(user.getId(), 0, null);
        Session.setCurrentUser(user);
        Session.setActiveShift(shiftRepository.getAktifSekarang(LocalTime.now()));
        return user;
    }

    /** Mengakhiri sesi aktif (R13.3, SR05). */
    public void logout() {
        Session.clear();
    }

    public List<User> daftarPengguna() {
        return userRepository.findAll();
    }

    public List<User> daftarStaffAktif() {
        return userRepository.findStaffAktif();
    }

    /** Pendaftaran akun baru oleh Supervisor (R12.2, R12.4). */
    public User buatPengguna(String namaLengkap, String username, String password, Role role) {
        requireSupervisor();
        if (namaLengkap == null || namaLengkap.isBlank()) {
            throw new BusinessException("Nama Lengkap wajib diisi.");
        }
        if (username == null || username.isBlank()) {
            throw new BusinessException("Username wajib diisi.");
        }
        if (password == null || password.length() < 6) {
            throw new BusinessException("Password minimal 6 karakter.");
        }
        if (role == null) {
            throw new BusinessException("Role wajib dipilih.");
        }
        if (userRepository.existsUsername(username.trim())) {
            throw new BusinessException("Username \"" + username.trim() + "\" sudah terdaftar. Gunakan username lain.");
        }
        User user = new User();
        user.setNamaLengkap(namaLengkap.trim());
        user.setUsername(username.trim());
        user.setPasswordHash(PasswordUtil.hash(password));
        user.setRole(role);
        long id = userRepository.save(user);
        user.setId(id);
        return user;
    }

    /** Pembaruan nama, role, dan status akun. */
    public void perbaruiPengguna(User user) {
        requireSupervisor();
        if (user.getNamaLengkap() == null || user.getNamaLengkap().isBlank()) {
            throw new BusinessException("Nama Lengkap wajib diisi.");
        }
        if (!user.isStatusAktif() && user.getId() == Session.getCurrentUser().getId()) {
            throw new BusinessException("Anda tidak dapat menonaktifkan akun yang sedang digunakan login (R12.5).");
        }
        userRepository.update(user);
    }

    /**
     * Penghapusan akun: memutus hak otentikasi tanpa menghapus identitas
     * pada riwayat mutasi (R12.3, R12.5, R12.6).
     */
    public void hapusPengguna(long userId) {
        requireSupervisor();
        if (userId == Session.getCurrentUser().getId()) {
            throw new BusinessException("Anda tidak dapat menghapus akun milik sendiri yang sedang digunakan (R12.5).");
        }
        userRepository.nonaktifkan(userId);
    }

    /** Reset password oleh Supervisor; pengguna wajib mengganti saat login berikutnya. */
    public void resetPassword(long userId, String passwordBaru) {
        requireSupervisor();
        if (passwordBaru == null || passwordBaru.length() < 6) {
            throw new BusinessException("Password baru minimal 6 karakter.");
        }
        userRepository.updatePassword(userId, PasswordUtil.hash(passwordBaru), true);
    }

    /** Penggantian password oleh pengguna sendiri (dipaksa setelah reset). */
    public void gantiPassword(long userId, String passwordLama, String passwordBaru) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Pengguna tidak ditemukan."));
        if (!PasswordUtil.verify(passwordLama, user.getPasswordHash())) {
            throw new BusinessException("Password lama tidak sesuai.");
        }
        if (passwordBaru == null || passwordBaru.length() < 6) {
            throw new BusinessException("Password baru minimal 6 karakter.");
        }
        if (passwordBaru.equals(passwordLama)) {
            throw new BusinessException("Password baru harus berbeda dari password lama.");
        }
        userRepository.updatePassword(userId, PasswordUtil.hash(passwordBaru), false);
    }

    private void requireSupervisor() {
        if (!Session.isSupervisor()) {
            throw new BusinessException("Hanya Supervisor yang berwenang mengelola akun pengguna (SR02, SR04).");
        }
    }
}
