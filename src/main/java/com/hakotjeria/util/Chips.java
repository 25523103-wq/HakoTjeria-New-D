package com.hakotjeria.util;

import com.hakotjeria.model.JenisMutasi;
import com.hakotjeria.model.StatusTugas;
import com.hakotjeria.model.StatusValidasi;

import javafx.scene.control.Label;

/** Pabrik label chip status dengan kode warna konsisten (ER04). */
public final class Chips {

    private Chips() {
    }

    public static Label of(String text, String colorClass) {
        Label label = new Label(text);
        label.getStyleClass().addAll("status-label", colorClass);
        return label;
    }

    /**
     * Kode warna status tugas (ER04):
     * kuning = Belum Dikerjakan, hijau = Selesai / Sudah Diambil,
     * merah = Tidak Terpenuhi, abu-abu = Belum Diambil.
     */
    public static Label statusTugas(StatusTugas status) {
        return switch (status) {
            case BELUM_DIKERJAKAN -> of(status.getLabel(), "chip-yellow");
            case SELESAI, SUDAH_DIAMBIL -> of(status.getLabel(), "chip-green");
            case TIDAK_TERPENUHI -> of(status.getLabel(), "chip-red");
            case BELUM_DIAMBIL -> of(status.getLabel(), "chip-gray");
        };
    }

    public static Label statusValidasi(StatusValidasi status) {
        return switch (status) {
            case TERVALIDASI -> of(status.getLabel(), "chip-green");
            case MENUNGGU_VALIDASI -> of(status.getLabel(), "chip-yellow");
            case DRAF_REVISI -> of(status.getLabel(), "chip-gray");
        };
    }

    public static Label jenisMutasi(JenisMutasi jenis) {
        return jenis == JenisMutasi.IN ? of("IN", "chip-green") : of("OUT", "chip-red");
    }
}
