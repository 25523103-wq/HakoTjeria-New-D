package com.hakotjeria.controller;

import java.math.BigDecimal;

import com.hakotjeria.model.JenisInventaris;
import com.hakotjeria.model.JenisMutasi;
import com.hakotjeria.model.StokItem;
import com.hakotjeria.service.MutasiStokService;
import com.hakotjeria.util.AlertUtil;
import com.hakotjeria.util.BusinessException;
import com.hakotjeria.util.Formats;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;

/**
 * Dialog Pencatatan Mutasi Stok Manual (UC-08).
 * Jenis IN/OUT dan Keterangan wajib; validasi kecukupan stok OUT
 * dilakukan pada service.
 */
public class MutasiManualDialogController {

    @FXML
    private ComboBox<JenisInventaris> kategoriCombo;
    @FXML
    private ComboBox<StokItem> barangCombo;
    @FXML
    private ComboBox<JenisMutasi> jenisCombo;
    @FXML
    private TextField qtyField;
    @FXML
    private TextArea keteranganArea;
    @FXML
    private Button simpanButton;

    private final MutasiStokService mutasiService = new MutasiStokService();
    private boolean tersimpan;

    public void init(JenisInventaris jenisDefault) {
        kategoriCombo.setItems(FXCollections.observableArrayList(JenisInventaris.values()));
        jenisCombo.setItems(FXCollections.observableArrayList(JenisMutasi.values()));
        barangCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(StokItem item) {
                return item == null ? "" : item.getNama();
            }

            @Override
            public StokItem fromString(String string) {
                return null;
            }
        });
        kategoriCombo.valueProperty().addListener((obs, o, n) -> muatBarang(n));
        kategoriCombo.setValue(jenisDefault == null ? JenisInventaris.BAHAN_BAKU : jenisDefault);
    }

    private void muatBarang(JenisInventaris jenis) {
        barangCombo.setValue(null);
        if (jenis == null) {
            barangCombo.setItems(FXCollections.observableArrayList());
            return;
        }
        barangCombo.setItems(FXCollections.observableArrayList(mutasiService.stokSemua(jenis)));
    }

    public boolean isTersimpan() {
        return tersimpan;
    }

    @FXML
    private void onSimpan() {
        try {
            StokItem barang = barangCombo.getValue();
            BigDecimal qty = Formats.parseQty(qtyField.getText(), "Jumlah (Qty)");
            mutasiService.catatMutasiManual(
                    kategoriCombo.getValue(),
                    barang == null ? null : barang.getBarangId(),
                    jenisCombo.getValue(),
                    qty,
                    keteranganArea.getText());
            tersimpan = true;
            AlertUtil.info("Mutasi Tersimpan", "Mutasi manual berhasil dicatat dan stok diperbarui.");
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
