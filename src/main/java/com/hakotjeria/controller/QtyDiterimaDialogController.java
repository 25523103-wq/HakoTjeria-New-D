package com.hakotjeria.controller;

import java.math.BigDecimal;

import com.hakotjeria.model.JadwalTugas;
import com.hakotjeria.service.ProduksiService;
import com.hakotjeria.util.AlertUtil;
import com.hakotjeria.util.BusinessException;
import com.hakotjeria.util.Formats;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Pop-Up Input Qty Diterima (UC-07, ER07):
 * nilai bawaan = Qty Target, hanya mencatat mutasi IN Produk Jadi.
 */
public class QtyDiterimaDialogController {

    @FXML
    private Label infoLabel;
    @FXML
    private TextField qtyDiterimaField;
    @FXML
    private Label satuanLabel;
    @FXML
    private Label targetLabel;
    @FXML
    private Button konfirmasiButton;

    private final ProduksiService produksiService = new ProduksiService();
    private JadwalTugas tugas;
    private boolean berhasil;

    public void init(JadwalTugas tugas) {
        this.tugas = tugas;
        satuanLabel.setText(tugas.getSatuanProduk().getLabel());
        targetLabel.setText("Qty Target: " + Formats.qtyWithSatuan(tugas.getQtyTarget(), tugas.getSatuanProduk()));
        qtyDiterimaField.setText(Formats.qty(tugas.getQtyTarget())); // nilai bawaan = Qty Target (R07.2)
    }

    public boolean isBerhasil() {
        return berhasil;
    }

    @FXML
    private void onKonfirmasi() {
        try {
            BigDecimal qty = Formats.parseQty(qtyDiterimaField.getText(), "Qty Diterima");
            produksiService.prosesPengambilan(tugas.getId(), qty);
            berhasil = true;
            AlertUtil.info("Penerimaan Tercatat",
                    "Penerimaan " + Formats.qty(qty) + " " + tugas.getNamaProduk()
                            + " dari Kedai Tjeria berhasil dicatat sebagai mutasi IN Produk Jadi.");
            close();
        } catch (BusinessException e) {
            AlertUtil.error("Gagal", e.getMessage());
        }
    }

    @FXML
    private void onBatal() {
        close();
    }

    private void close() {
        ((Stage) konfirmasiButton.getScene().getWindow()).close();
    }
}
