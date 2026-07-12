package com.hakotjeria.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.hakotjeria.model.Satuan;

/** Format angka dan tanggal yang konsisten di seluruh aplikasi. */
public final class Formats {

    public static final DateTimeFormatter TANGGAL = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    public static final DateTimeFormatter TANGGAL_WAKTU = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private static final DecimalFormat QTY;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("id-ID"));
        QTY = new DecimalFormat("#,##0.###", symbols);
    }

    private Formats() {
    }

    public static String qty(BigDecimal value) {
        if (value == null) {
            return "-";
        }
        return QTY.format(value.setScale(3, RoundingMode.HALF_UP).stripTrailingZeros());
    }

    public static String qtyWithSatuan(BigDecimal value, Satuan satuan) {
        if (value == null) {
            return "-";
        }
        return qty(value) + (satuan != null ? " " + satuan.getLabel() : "");
    }

    public static String tanggal(LocalDate d) {
        return d == null ? "-" : TANGGAL.format(d);
    }

    public static String tanggalWaktu(LocalDateTime dt) {
        return dt == null ? "-" : TANGGAL_WAKTU.format(dt);
    }

    /** Parse angka desimal dari input teks; melempar BusinessException bila non-numerik. */
    public static BigDecimal parseQty(String text, String namaField) {
        if (text == null || text.isBlank()) {
            throw new BusinessException(namaField + " wajib diisi.");
        }
        try {
            return new BigDecimal(text.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            throw new BusinessException(namaField + " harus berupa angka yang valid.");
        }
    }
}
