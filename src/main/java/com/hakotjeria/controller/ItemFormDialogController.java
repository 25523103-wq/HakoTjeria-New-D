package com.hakotjeria.controller;

import java.math.BigDecimal;

import com.hakotjeria.model.JenisInventaris;
import com.hakotjeria.model.Satuan;
import com.hakotjeria.model.StokItem;
import com.hakotjeria.model.SumberProduk;
import com.hakotjeria.service.MasterDataService;
import com.hakotjeria.service.MutasiStokService;
import com.hakotjeria.util.AlertUtil;
import com.hakotjeria.util.BusinessException;
import com.hakotjeria.util.Formats;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Dialog tambah/ubah master data (UC-03).
 * Satuan wajib dari Dropdown Satuan (R03.2); Kuantitas Awal hanya saat
 * pembuatan dan menjadi mutasi IN "Stok Awal" (R03.4). Saat mode ubah,
 * Stok Saat Ini dapat dikoreksi langsung; selisihnya dicatat sebagai
 * mutasi koreksi agar jejak audit tetap utuh.
 */
public class ItemFormDialogController {

    @FXML
    private Label judulLabel;
    @FXML
    private Label infoLabel;
    @FXML
    private TextField namaField;
    @FXML
    private ComboBox<Satuan> satuanCombo;
    @FXML
    private VBox sumberBox;
    @FXML
    private ComboBox<SumberProduk> sumberCombo;
    @FXML
    private TextField batasMinField;
    @FXML
    private VBox kuantitasAwalBox;
    @FXML
    private TextField kuantitasAwalField;
    @FXML
    private VBox stokBox;
    @FXML
    private TextField stokField;
    @FXML
    private Button simpanButton;

    private final MasterDataService masterService = new MasterDataService();
    private final MutasiStokService mutasiService = new MutasiStokService();
    private JenisInventaris jenis;
    private StokItem existing;
    private boolean tersimpan;

    public void init(JenisInventaris jenis, StokItem existing) {
        this.jenis = jenis;
        this.existing = existing;

        satuanCombo.setItems(FXCollections.observableArrayList(Satuan.values()));
        sumberCombo.setItems(FXCollections.observableArrayList(SumberProduk.values()));
        boolean produk = jenis == JenisInventaris.PRODUK_JADI;
        sumberBox.setVisible(produk);
        sumberBox.setManaged(produk);

        if (existing == null) {
            judulLabel.setText("Tambah " + jenis.getLabel() + " Baru");
            return;
        }
        judulLabel.setText("Ubah " + jenis.getLabel());
        namaField.setText(existing.getNama());
        satuanCombo.setValue(existing.getSatuan());
        sumberCombo.setValue(existing.getSumber());
        batasMinField.setText(Formats.qtyEditable(existing.getBatasMin()));
        // Kuantitas Awal hanya untuk pembuatan; saat ubah, koreksi lewat field Stok Saat Ini.
        kuantitasAwalBox.setVisible(false);
        kuantitasAwalBox.setManaged(false);
        stokBox.setVisible(true);
        stokBox.setManaged(true);
        stokField.setText(Formats.qtyEditable(existing.getStok()));

        if (masterService.punyaRiwayatMutasi(jenis, existing.getBarangId())) {
            satuanCombo.setDisable(true);
            infoLabel.setText("Barang ini telah memiliki riwayat mutasi: satuan terkunci. "
                    + "Perubahan Stok Saat Ini akan tercatat otomatis sebagai mutasi koreksi.");
            infoLabel.setVisible(true);
            infoLabel.setManaged(true);
        }
    }

    public boolean isTersimpan() {
        return tersimpan;
    }

    @FXML
    private void onSimpan() {
        try {
            String nama = namaField.getText();
            Satuan satuan = satuanCombo.getValue();
            BigDecimal batasMin = Formats.parseQty(
                    batasMinField.getText() == null || batasMinField.getText().isBlank()
                            ? "0" : batasMinField.getText(), "Batas Minimum");

            if (existing == null) {
                BigDecimal kuantitasAwal = Formats.parseQty(
                        kuantitasAwalField.getText() == null || kuantitasAwalField.getText().isBlank()
                                ? "0" : kuantitasAwalField.getText(), "Kuantitas Awal");
                if (jenis == JenisInventaris.BAHAN_BAKU) {
                    masterService.buatBahanBaku(nama, satuan, batasMin, kuantitasAwal);
                } else {
                    masterService.buatProdukJadi(nama, satuan, sumberCombo.getValue(), batasMin, kuantitasAwal);
                }
            } else {
                BigDecimal stokBaru = Formats.parseQty(stokField.getText(), "Stok Saat Ini");
                if (jenis == JenisInventaris.BAHAN_BAKU) {
                    masterService.perbaruiBahanBaku(existing.getBarangId(), nama, satuan, batasMin);
                } else {
                    masterService.perbaruiProdukJadi(existing.getBarangId(), nama, satuan,
                            sumberCombo.getValue(), batasMin);
                }
                mutasiService.sesuaikanStok(jenis, existing.getBarangId(), stokBaru);
            }
            tersimpan = true;
            close();
        } catch (BusinessException e) {
            AlertUtil.error("Validasi Gagal", e.getMessage());
        }
    }

    @FXML
    private void onBatal() {
        close();
    }

    private void close() {
        ((Stage) simpanButton.getScene().getWindow()).close();
    }
}
