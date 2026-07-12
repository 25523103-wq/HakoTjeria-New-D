package com.hakotjeria.util;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.hakotjeria.model.MutasiStok;
import com.hakotjeria.model.RingkasanMutasi;
import com.hakotjeria.model.DetailStockOpname;
import com.hakotjeria.model.StockOpname;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Generator dokumen PDF (OpenPDF) untuk Riwayat Mutasi Stok (R09.7)
 * dan Riwayat Stock Opname (R11.6).
 */
public final class PdfUtil {

    private static final Color BIRU_TUA = new Color(27, 43, 101);
    private static final Color ABU_HEADER = new Color(238, 241, 248);
    private static final Font FONT_JUDUL = new Font(Font.HELVETICA, 16, Font.BOLD, BIRU_TUA);
    private static final Font FONT_SUB = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);
    private static final Font FONT_HEADER = new Font(Font.HELVETICA, 9, Font.BOLD, BIRU_TUA);
    private static final Font FONT_SEL = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.BLACK);
    private static final Font FONT_LABEL = new Font(Font.HELVETICA, 10, Font.BOLD, Color.BLACK);

    private PdfUtil() {
    }

    /** Ekspor riwayat mutasi terfilter ke PDF (R09.7). */
    public static void exportMutasi(File target, String judulTabel, String deskripsiFilter,
                                    List<MutasiStok> rows, RingkasanMutasi ringkasan,
                                    boolean tampilkanStokAwalAkhir) {
        Document doc = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(doc, new FileOutputStream(target));
            doc.open();
            tulisKop(doc, "Laporan Riwayat Mutasi Stok - " + judulTabel);
            if (deskripsiFilter != null && !deskripsiFilter.isBlank()) {
                doc.add(new Paragraph("Filter: " + deskripsiFilter, FONT_SUB));
            }
            doc.add(new Paragraph(" ", FONT_SUB));

            PdfPTable table = new PdfPTable(new float[]{1.2f, 0.8f, 2.2f, 0.8f, 1.2f, 2.6f, 1.6f});
            table.setWidthPercentage(100);
            header(table, "Tanggal", "Shift", "Nama Barang", "Jenis", "Qty", "Keterangan", "Penginput");
            for (MutasiStok m : rows) {
                sel(table, Formats.tanggal(m.getTanggal()));
                sel(table, m.getNamaShift());
                sel(table, m.getNamaBarang());
                sel(table, m.getJenis().name());
                sel(table, Formats.qtyWithSatuan(m.getQty(), m.getSatuanBarang()));
                sel(table, m.getKeterangan());
                sel(table, m.getNamaPenginput() == null ? "-" : m.getNamaPenginput());
            }
            doc.add(table);

            if (ringkasan != null) {
                doc.add(new Paragraph(" ", FONT_SUB));
                Paragraph p = new Paragraph("Ringkasan Periode Terfilter", FONT_LABEL);
                doc.add(p);
                String stokAwal = tampilkanStokAwalAkhir ? Formats.qty(ringkasan.getStokAwal()) : "-";
                String stokAkhir = tampilkanStokAwalAkhir ? Formats.qty(ringkasan.getStokAkhir()) : "-";
                doc.add(new Paragraph(
                        "Stok Awal: " + stokAwal
                                + "    |    Total IN: " + Formats.qty(ringkasan.getTotalIn())
                                + "    |    Total OUT: " + Formats.qty(ringkasan.getTotalOut())
                                + "    |    Stok Akhir: " + stokAkhir,
                        FONT_SEL));
            }
            tulisFooter(doc);
        } catch (DocumentException | IOException e) {
            throw new BusinessException("Gagal membuat dokumen PDF: " + e.getMessage());
        } finally {
            if (doc.isOpen()) {
                doc.close();
            }
        }
    }

    /** Ekspor detail Riwayat Stock Opname ke PDF dengan kop laporan (R11.6). */
    public static void exportStockOpname(File target, StockOpname opname, List<MutasiStok> penyesuaian) {
        Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(doc, new FileOutputStream(target));
            doc.open();
            tulisKop(doc, "Laporan Stock Opname " + opname.getKode());

            PdfPTable info = new PdfPTable(new float[]{1.4f, 2.6f, 1.4f, 2.6f});
            info.setWidthPercentage(100);
            infoSel(info, "Tanggal", Formats.tanggal(opname.getTanggal()));
            infoSel(info, "Shift", opname.getNamaShift());
            infoSel(info, "Staff Penginput", nullDash(opname.getNamaPenginput()));
            infoSel(info, "Supervisor Validator", nullDash(opname.getNamaValidator()));
            infoSel(info, "Waktu Input", Formats.tanggalWaktu(opname.getInputAt()));
            infoSel(info, "Waktu Validasi", Formats.tanggalWaktu(opname.getValidAt()));
            infoSel(info, "Status Validasi", opname.getStatus().getLabel());
            infoSel(info, "Total Selisih", Formats.qty(opname.getTotalSelisih() == null
                    ? BigDecimal.ZERO : opname.getTotalSelisih()));
            doc.add(info);
            doc.add(new Paragraph(" ", FONT_SUB));

            doc.add(new Paragraph("Rincian Selisih", FONT_LABEL));
            PdfPTable table = new PdfPTable(new float[]{1.4f, 2.6f, 1.2f, 1.4f, 1.4f, 1.4f});
            table.setWidthPercentage(100);
            header(table, "Kategori", "Nama Barang", "Satuan", "Stok Sistem", "Stok Fisik", "Selisih");
            for (DetailStockOpname d : opname.getDetail()) {
                sel(table, d.getJenisInventaris().getLabel());
                sel(table, d.getNamaBarang());
                sel(table, d.getSatuanBarang() == null ? "-" : d.getSatuanBarang().getLabel());
                sel(table, Formats.qty(d.getStokSistem()));
                sel(table, Formats.qty(d.getStokFisik()));
                sel(table, Formats.qty(d.getSelisih()));
            }
            doc.add(table);

            if (penyesuaian != null && !penyesuaian.isEmpty()) {
                doc.add(new Paragraph(" ", FONT_SUB));
                doc.add(new Paragraph("Mutasi Penyesuaian yang Dihasilkan", FONT_LABEL));
                PdfPTable adj = new PdfPTable(new float[]{1.4f, 2.6f, 1f, 1.6f, 2.4f});
                adj.setWidthPercentage(100);
                header(adj, "Kategori", "Nama Barang", "Jenis", "Qty", "Keterangan");
                for (MutasiStok m : penyesuaian) {
                    sel(adj, m.getJenisInventaris().getLabel());
                    sel(adj, m.getNamaBarang());
                    sel(adj, m.getJenis().name());
                    sel(adj, Formats.qtyWithSatuan(m.getQty(), m.getSatuanBarang()));
                    sel(adj, m.getKeterangan());
                }
                doc.add(adj);
            }
            tulisFooter(doc);
        } catch (DocumentException | IOException e) {
            throw new BusinessException("Gagal membuat dokumen PDF: " + e.getMessage());
        } finally {
            if (doc.isOpen()) {
                doc.close();
            }
        }
    }

    // ===================== HELPER =====================

    /** Kop laporan resmi (R11.6). */
    private static void tulisKop(Document doc, String judul) throws DocumentException {
        Paragraph nama = new Paragraph("HAKO TJERIA", FONT_JUDUL);
        nama.setAlignment(Element.ALIGN_CENTER);
        doc.add(nama);
        Paragraph sub = new Paragraph("Industrial Bakery Management - Yogyakarta", FONT_SUB);
        sub.setAlignment(Element.ALIGN_CENTER);
        doc.add(sub);
        Paragraph garis = new Paragraph("__________________________________________________________________________",
                new Font(Font.HELVETICA, 10, Font.BOLD, BIRU_TUA));
        garis.setAlignment(Element.ALIGN_CENTER);
        doc.add(garis);
        Paragraph j = new Paragraph(judul, new Font(Font.HELVETICA, 13, Font.BOLD, Color.BLACK));
        j.setAlignment(Element.ALIGN_CENTER);
        j.setSpacingBefore(8);
        j.setSpacingAfter(10);
        doc.add(j);
    }

    private static void tulisFooter(Document doc) throws DocumentException {
        Paragraph footer = new Paragraph("Dicetak melalui Sistem Informasi Hako Tjeria pada "
                + Formats.tanggalWaktu(LocalDateTime.now()), FONT_SUB);
        footer.setSpacingBefore(14);
        doc.add(footer);
    }

    private static void header(PdfPTable table, String... labels) {
        for (String label : labels) {
            PdfPCell cell = new PdfPCell(new Phrase(label, FONT_HEADER));
            cell.setBackgroundColor(ABU_HEADER);
            cell.setPadding(5);
            table.addCell(cell);
        }
    }

    private static void sel(PdfPTable table, String value) {
        PdfPCell cell = new PdfPCell(new Phrase(value == null ? "-" : value, FONT_SEL));
        cell.setPadding(4);
        table.addCell(cell);
    }

    private static void infoSel(PdfPTable table, String label, String value) {
        PdfPCell l = new PdfPCell(new Phrase(label, FONT_HEADER));
        l.setPadding(4);
        l.setBackgroundColor(ABU_HEADER);
        table.addCell(l);
        PdfPCell v = new PdfPCell(new Phrase(value == null ? "-" : value, FONT_SEL));
        v.setPadding(4);
        table.addCell(v);
    }

    private static String nullDash(String s) {
        return s == null ? "-" : s;
    }
}
