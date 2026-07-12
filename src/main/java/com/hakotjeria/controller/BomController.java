package com.hakotjeria.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.hakotjeria.model.BahanBaku;
import com.hakotjeria.model.Bom;
import com.hakotjeria.model.KomponenBom;
import com.hakotjeria.model.ProdukJadi;
import com.hakotjeria.service.BomService;
import com.hakotjeria.service.MasterDataService;
import com.hakotjeria.util.AlertUtil;
import com.hakotjeria.util.BusinessException;
import com.hakotjeria.util.Chips;
import com.hakotjeria.util.Formats;
import com.hakotjeria.util.Icons;
import com.hakotjeria.util.Session;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Controller Manajemen BOM (UC-04). Supervisor menyusun resep;
 * Staff hanya membaca tanpa tombol manipulasi (R04.6).
 */
public class BomController {

    @FXML
    private ComboBox<ProdukJadi> produkCombo;
    @FXML
    private Label sumberChipHolder;
    @FXML
    private HBox eksternalBanner;
    @FXML
    private VBox bomCard;
    @FXML
    private HBox tambahBox;
    @FXML
    private ComboBox<BahanBaku> bahanCombo;
    @FXML
    private TextField qtyKomponenField;
    @FXML
    private Label satuanKomponenLabel;
    @FXML
    private Button tambahKomponenButton;
    @FXML
    private TableView<KomponenBom> komponenTable;
    @FXML
    private TableColumn<KomponenBom, String> colKomponenNama;
    @FXML
    private TableColumn<KomponenBom, String> colKomponenQty;
    @FXML
    private TableColumn<KomponenBom, String> colKomponenSatuan;
    @FXML
    private TableColumn<KomponenBom, KomponenBom> colKomponenAksi;
    @FXML
    private Button hapusBomButton;
    @FXML
    private Button simpanBomButton;

    private final MasterDataService masterService = new MasterDataService();
    private final BomService bomService = new BomService();
    private final ObservableList<KomponenBom> komponenList = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        produkCombo.setItems(FXCollections.observableArrayList(masterService.daftarProdukJadi()));
        bahanCombo.setItems(FXCollections.observableArrayList(masterService.daftarBahanBaku()));

        colKomponenNama.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNamaBahan()));
        colKomponenQty.setCellValueFactory(c -> new SimpleStringProperty(Formats.qty(c.getValue().getQty())));
        colKomponenSatuan.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getSatuanBahan() == null ? "-" : c.getValue().getSatuanBahan().getLabel()));
        colKomponenAksi.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colKomponenAksi.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(KomponenBom item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || !Session.isSupervisor()) {
                    setGraphic(null);
                    return;
                }
                Button hapus = new Button();
                hapus.getStyleClass().add("btn-icon");
                hapus.setGraphic(Icons.of(Icons.TRASH, 14, Color.web("#D92D20")));
                hapus.setOnAction(e -> komponenList.remove(item));
                setGraphic(hapus);
            }
        });
        komponenTable.setItems(komponenList);
        komponenTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        komponenTable.setPlaceholder(new Label("Belum ada komponen. Pilih produk lalu tambahkan Bahan Baku."));

        // Satuan komponen otomatis mengikuti master Bahan Baku dan terkunci (R04.3).
        bahanCombo.valueProperty().addListener((obs, o, bahan) ->
                satuanKomponenLabel.setText(bahan == null ? "-" : bahan.getSatuan().getLabel()));

        produkCombo.valueProperty().addListener((obs, o, produk) -> muatBom(produk));

        boolean supervisor = Session.isSupervisor();
        tambahBox.setVisible(supervisor);
        tambahBox.setManaged(supervisor);
        simpanBomButton.setVisible(supervisor);
        simpanBomButton.setManaged(supervisor);
        hapusBomButton.setVisible(supervisor);
        hapusBomButton.setManaged(supervisor);

        setFormAktif(false);
    }

    private void muatBom(ProdukJadi produk) {
        komponenList.clear();
        sumberChipHolder.setGraphic(null);
        if (produk == null) {
            setFormAktif(false);
            tampilkanBannerEksternal(false);
            return;
        }
        sumberChipHolder.setGraphic(produk.isInternal()
                ? Chips.of("Internal (ber-BOM)", "chip-blue")
                : Chips.of("Eksternal (tanpa BOM)", "chip-gray"));

        // Alur alternatif UC-04: produk eksternal menonaktifkan formulir BOM.
        if (produk.isEksternal()) {
            tampilkanBannerEksternal(true);
            setFormAktif(false);
            return;
        }
        tampilkanBannerEksternal(false);
        setFormAktif(Session.isSupervisor());
        Optional<Bom> bom = bomService.getBom(produk.getId());
        bom.ifPresent(b -> komponenList.addAll(b.getKomponen()));
    }

    @FXML
    private void onTambahKomponen() {
        try {
            BahanBaku bahan = bahanCombo.getValue();
            if (bahan == null) {
                throw new BusinessException("Pilih Bahan Baku terlebih dahulu.");
            }
            BigDecimal qty = Formats.parseQty(qtyKomponenField.getText(), "Qty per unit");
            if (qty.signum() <= 0) {
                throw new BusinessException("Kuantitas komponen tidak boleh nol atau negatif (R04.5).");
            }
            boolean sudahAda = komponenList.stream()
                    .anyMatch(k -> k.getBahanBakuId() == bahan.getId());
            if (sudahAda) {
                throw new BusinessException("Bahan \"" + bahan.getNama() + "\" sudah ada pada daftar komponen.");
            }
            KomponenBom k = new KomponenBom();
            k.setBahanBakuId(bahan.getId());
            k.setQty(qty);
            k.setNamaBahan(bahan.getNama());
            k.setSatuanBahan(bahan.getSatuan());
            komponenList.add(k);
            qtyKomponenField.clear();
            bahanCombo.setValue(null);
        } catch (BusinessException e) {
            AlertUtil.error("Validasi Gagal", e.getMessage());
        }
    }

    @FXML
    private void onSimpanBom() {
        ProdukJadi produk = produkCombo.getValue();
        if (produk == null) {
            AlertUtil.warn("Pilih Produk", "Pilih Produk Jadi Internal terlebih dahulu.");
            return;
        }
        try {
            bomService.simpanBom(produk.getId(), new ArrayList<>(komponenList));
            AlertUtil.info("BOM Tersimpan", "Resep BOM untuk \"" + produk.getNama() + "\" berhasil disimpan.");
            muatBom(produk);
        } catch (BusinessException e) {
            AlertUtil.error("Gagal Menyimpan BOM", e.getMessage());
        }
    }

    @FXML
    private void onHapusBom() {
        ProdukJadi produk = produkCombo.getValue();
        if (produk == null) {
            return;
        }
        boolean yakin = AlertUtil.confirm("Hapus BOM",
                "Hapus seluruh resep BOM untuk \"" + produk.getNama() + "\"?");
        if (!yakin) {
            return;
        }
        try {
            bomService.hapusBom(produk.getId());
            AlertUtil.info("Berhasil", "BOM telah dihapus.");
            muatBom(produk);
        } catch (BusinessException e) {
            AlertUtil.error("Tidak Dapat Menghapus", e.getMessage());
        }
    }

    private void setFormAktif(boolean aktif) {
        tambahBox.setDisable(!aktif);
        simpanBomButton.setDisable(!aktif);
        hapusBomButton.setDisable(!aktif);
    }

    private void tampilkanBannerEksternal(boolean tampil) {
        eksternalBanner.setVisible(tampil);
        eksternalBanner.setManaged(tampil);
    }

    private List<KomponenBom> snapshotKomponen() {
        return new ArrayList<>(komponenList);
    }
}
