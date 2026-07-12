package com.hakotjeria.util;

/** Pelanggaran aturan bisnis; pesan ditampilkan langsung ke pengguna. */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
