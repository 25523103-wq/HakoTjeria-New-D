package com.hakotjeria.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Dokumen Stock Opname satu shift (mencakup dua kategori inventaris). */
public class StockOpname {

    private long id;
    private String kode;
    private LocalDate tanggal;
    private long shiftId;
    private StatusValidasi status;
    private Long inputBy;
    private LocalDateTime inputAt;
    private Long validBy;
    private LocalDateTime validAt;
    private String catatanValidasi;

    // Atribut tampilan (join).
    private String namaShift;
    private String namaPenginput;
    private String namaValidator;
    private BigDecimal totalSelisih;

    private final List<DetailStockOpname> detail = new ArrayList<>();

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getKode() {
        return kode;
    }

    public void setKode(String kode) {
        this.kode = kode;
    }

    public LocalDate getTanggal() {
        return tanggal;
    }

    public void setTanggal(LocalDate tanggal) {
        this.tanggal = tanggal;
    }

    public long getShiftId() {
        return shiftId;
    }

    public void setShiftId(long shiftId) {
        this.shiftId = shiftId;
    }

    public StatusValidasi getStatus() {
        return status;
    }

    public void setStatus(StatusValidasi status) {
        this.status = status;
    }

    public Long getInputBy() {
        return inputBy;
    }

    public void setInputBy(Long inputBy) {
        this.inputBy = inputBy;
    }

    public LocalDateTime getInputAt() {
        return inputAt;
    }

    public void setInputAt(LocalDateTime inputAt) {
        this.inputAt = inputAt;
    }

    public Long getValidBy() {
        return validBy;
    }

    public void setValidBy(Long validBy) {
        this.validBy = validBy;
    }

    public LocalDateTime getValidAt() {
        return validAt;
    }

    public void setValidAt(LocalDateTime validAt) {
        this.validAt = validAt;
    }

    public String getCatatanValidasi() {
        return catatanValidasi;
    }

    public void setCatatanValidasi(String catatanValidasi) {
        this.catatanValidasi = catatanValidasi;
    }

    public String getNamaShift() {
        return namaShift;
    }

    public void setNamaShift(String namaShift) {
        this.namaShift = namaShift;
    }

    public String getNamaPenginput() {
        return namaPenginput;
    }

    public void setNamaPenginput(String namaPenginput) {
        this.namaPenginput = namaPenginput;
    }

    public String getNamaValidator() {
        return namaValidator;
    }

    public void setNamaValidator(String namaValidator) {
        this.namaValidator = namaValidator;
    }

    public BigDecimal getTotalSelisih() {
        return totalSelisih;
    }

    public void setTotalSelisih(BigDecimal totalSelisih) {
        this.totalSelisih = totalSelisih;
    }

    public List<DetailStockOpname> getDetail() {
        return detail;
    }
}
