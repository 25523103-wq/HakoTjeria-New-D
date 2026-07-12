package com.hakotjeria.model;

import java.time.LocalTime;

/** Entitas shift kerja harian (Shift 1 dan Shift 2). */
public class Shift {

    private long id;
    private String namaShift;
    private int urutan;
    private LocalTime jamMulai;
    private LocalTime jamSelesai;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNamaShift() {
        return namaShift;
    }

    public void setNamaShift(String namaShift) {
        this.namaShift = namaShift;
    }

    public int getUrutan() {
        return urutan;
    }

    public void setUrutan(int urutan) {
        this.urutan = urutan;
    }

    public LocalTime getJamMulai() {
        return jamMulai;
    }

    public void setJamMulai(LocalTime jamMulai) {
        this.jamMulai = jamMulai;
    }

    public LocalTime getJamSelesai() {
        return jamSelesai;
    }

    public void setJamSelesai(LocalTime jamSelesai) {
        this.jamSelesai = jamSelesai;
    }

    /** Menentukan apakah waktu tertentu berada dalam rentang shift ini. */
    public boolean mencakup(LocalTime waktu) {
        return !waktu.isBefore(jamMulai) && waktu.isBefore(jamSelesai);
    }

    @Override
    public String toString() {
        return namaShift;
    }
}
