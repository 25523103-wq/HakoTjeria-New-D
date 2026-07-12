package com.hakotjeria.controller;

import java.math.BigDecimal;
import java.util.List;

import com.hakotjeria.model.JadwalTugas;
import com.hakotjeria.service.ProduksiService;
import com.hakotjeria.service.ProduksiService.KebutuhanBahan;
import com.hakotjeria.util.AlertUtil;
import com.hakotjeria.util.BusinessException;
import com.hakotjeria.util.Chips;
import com.hakotjeria.util.Formats;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 * Pop-Up Input Qty Aktual (UC-06, ER05, ER06):
 * nilai bawaan = Qty Target, pratinjau pemotongan BOM diperbarui dinamis
 * mengikuti angka yang diketik (SR14 < 500 ms).
 */
public class QtyAktualDialogController {

    @FXML
    private Label pertanyaanLabel;
    @FXML
    private TextField qtyAktualField;
    @FXML
    private Label satuanLabel;
    @FXML
    private Label targetLabel;
    @FXML
    private TableView<KebutuhanBahan> previewTable;
    @FXML
    private TableColumn<KebutuhanBahan, String> colPrevBahan;
    @FXML
    private TableColumn<KebutuhanBahan, String> colPrevKebutuhan;
    @FXML
    private TableColumn<KebutuhanBahan, String> colPrevStok;
    @FXML
    private TableColumn<KebutuhanBahan, KebutuhanBahan> colPrevStatus;
    @FXML
    private HBox kurangBanner;
    @FXML
    private Label kurangLabel;
    @FXML
    private Button konfirmasiButton;

    private final ProduksiService produksiService = new ProduksiService();
    private JadwalTugas tugas;
    private boolean berhasil;

    public void init(JadwalTugas tugas) {
        this.tugas = tugas;
        pertanyaanLabel.setText("Berapa " + tugas.getNamaProduk() + " yang berhasil dibuat?");
        satuanLabel.setText(tugas.getSatuanProduk().getLabel());
        targetLabel.setText("Qty Target: " + Formats.qtyWithSatuan(tugas.getQtyTarget(), tugas.getSatuanProduk()));
        qtyAktualField.setText(Formats.qty(tugas.getQtyTarget())); // nilai bawaan = Qty Target (R06.1)

        colPrevBahan.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getKomponen().getNamaBahan()));
        colPrevKebutuhan.setCellValueFactory(c -> new SimpleStringProperty(
                Formats.qtyWithSatuan(c.getValue().getKebutuhan(), c.getValue().getKomponen().getSatuanBahan())));
        colPrevStok.setCellValueFactory(c -> new SimpleStringProperty(
                Formats.qtyWithSatuan(c.getValue().getStokTersedia(), c.getValue().getKomponen().getSatuanBahan())));
        colPrevStatus.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colPrevStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(KebutuhanBahan item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(item.isCukup() ? Chips.of("Cukup", "chip-green") : Chips.of("Kurang", "chip-red"));
                }
            }
        });
        previewTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Pratinjau berubah dinamis mengikuti angka Qty Aktual (ER06).
        qtyAktualField.textProperty().addListener((obs, o, n) -> perbaruiPreview());
        perbaruiPreview();
    }

    public boolean isBerhasil() {
        return berhasil;
    }

    private void perbaruiPreview() {
        BigDecimal qty;
        try {
            qty = new BigDecimal(qtyAktualField.getText().trim().replace(",", "."));
        } catch (Exception e) {
            qty = BigDecimal.ZERO;
        }
        try {
            List<KebutuhanBahan> preview = produksiService.previewKebutuhan(tugas.getProdukId(), qty);
            previewTable.setItems(FXCollections.observableArrayList(preview));
            List<String> kurang = preview.stream()
                    .filter(k -> !k.isCukup())
                    .map(k -> k.getKomponen().getNamaBahan() + " (kurang "
                            + Formats.qtyWithSatuan(k.getKekurangan(), k.getKomponen().getSatuanBahan()) + ")")
                    .toList();
            boolean adaKurang = !kurang.isEmpty();
            kurangBanner.setVisible(adaKurang);
            kurangBanner.setManaged(adaKurang);
            if (adaKurang) {
                kurangLabel.setText("Stok tidak cukup: " + String.join(", ", kurang)
                        + ". Turunkan angka Qty Aktual.");
            }
        } catch (BusinessException e) {
            kurangBanner.setVisible(true);
            kurangBanner.setManaged(true);
            kurangLabel.setText(e.getMessage());
        }
    }

    @FXML
    private void onKonfirmasi() {
        try {
            BigDecimal qty = Formats.parseQty(qtyAktualField.getText(), "Qty Aktual");
            produksiService.prosesProduksi(tugas.getId(), qty);
            berhasil = true;
            AlertUtil.info("Produksi Tercatat",
                    "Produksi " + Formats.qty(qty) + " " + tugas.getNamaProduk()
                            + " berhasil dicatat. Bahan baku terpotong sesuai BOM dan stok produk bertambah.");
            close();
        } catch (BusinessException e) {
            // Alur alternatif UC-06: kembali ke pop-up untuk menurunkan angka.
            AlertUtil.error("Eksekusi Ditolak", e.getMessage());
            perbaruiPreview();
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
