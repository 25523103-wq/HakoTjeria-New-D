package com.hakotjeria.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.hakotjeria.config.DatabaseConfig;
import com.hakotjeria.model.Shift;

/** Akses data tabel shift. */
public class ShiftRepository {

    public List<Shift> findAll() {
        String sql = "SELECT id, nama_shift, urutan, jam_mulai, jam_selesai FROM shift ORDER BY urutan";
        List<Shift> result = new ArrayList<>();
        try (Connection con = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(map(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RepositoryException("Gagal memuat data shift", e);
        }
    }

    public Shift findById(long id) {
        return findAll().stream()
                .filter(s -> s.getId() == id)
                .findFirst()
                .orElse(null);
    }

    /**
     * Menentukan shift aktif berdasarkan jam sekarang.
     * Di luar rentang kedua shift, waktu sebelum Shift 1 memakai Shift 1
     * dan waktu setelah Shift 2 memakai Shift 2 (shift terakhir).
     */
    public Shift getAktifSekarang(LocalTime sekarang) {
        List<Shift> all = findAll();
        if (all.isEmpty()) {
            throw new RepositoryException("Data shift belum tersedia", null);
        }
        return all.stream()
                .filter(s -> s.mencakup(sekarang))
                .findFirst()
                .orElseGet(() -> {
                    Shift pertama = all.get(0);
                    if (sekarang.isBefore(pertama.getJamMulai())) {
                        return pertama;
                    }
                    return all.stream()
                            .max(Comparator.comparingInt(Shift::getUrutan))
                            .orElse(pertama);
                });
    }

    private Shift map(ResultSet rs) throws SQLException {
        Shift s = new Shift();
        s.setId(rs.getLong("id"));
        s.setNamaShift(rs.getString("nama_shift"));
        s.setUrutan(rs.getInt("urutan"));
        s.setJamMulai(rs.getTime("jam_mulai").toLocalTime());
        s.setJamSelesai(rs.getTime("jam_selesai").toLocalTime());
        return s;
    }
}
