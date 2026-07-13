package com.hakotjeria.controller;

import com.hakotjeria.model.JenisInventaris;
import com.hakotjeria.model.StokItem;
import com.hakotjeria.service.MasterDataService;
import com.hakotjeria.util.AlertUtil;
import com.hakotjeria.util.BusinessException;
import com.hakotjeria.util.Chips;
import com.hakotjeria.util.Formats;
import com.hakotjeria.util.FxUtil;
import com.hakotjeria.util.Icons;
import com.hakotjeria.util.Session;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Controller Manajemen Master Data Dual-Inventory (UC-03).
 * Supervisor memiliki hak CRUD; Staff hanya membaca (R03.6).
 */
public class MasterDataController {

    @FXML
    private TabPane tabPane;
    @FXML
    private Button newItemButton;
    @FXML
    private TextField searchBahanField;
    @FXML
    private TableView<StokItem> bahanTable;
    @FXML
    private TableColumn<StokItem, String> colBahanNama;
    @FXML
    private TableColumn<StokItem, String> colBahanSatuan;
    @FXML
    private TableColumn<StokItem, String> colBahanBatas;
    @FXML
    private TableColumn<StokItem, String> colBahanStok;
    @FXML
    private TableColumn<StokItem, StokItem> colBahanStatus;
    @FXML
    private TableColumn<StokItem, StokItem> colBahanAksi;
    @FXML
    private TextField searchProdukField;
    @FXML
    private TableView<StokItem> produkTable;
    @FXML
    private TableColumn<StokItem, String> colProdukNama;
    @FXML
    private TableColumn<StokItem, String> colProdukSumber;
    @FXML
    private TableColumn<StokItem, String> colProdukSatuan;
    @FXML
    private TableColumn<StokItem, String> colProdukBatas;
    @FXML
    private TableColumn<StokItem, String> colProdukStok;
    @FXML
    private TableColumn<StokItem, StokItem> colProdukStatus;
    @FXML
    private TableColumn<StokItem, StokItem> colProdukAksi;

    private final MasterDataService masterService = new MasterDataService();

    @FXML
    private void initialize() {
        boolean supervisor = Session.isSupervisor();
        newItemButton.setVisible(supervisor);
        newItemButton.setManaged(supervisor);

        setupColumns(colBahanNama, colBahanSatuan, colBahanBatas, colBahanStok, colBahanStatus, colBahanAksi);
        setupColumns(colProdukNama, colProdukSatuan, colProdukBatas, colProdukStok, colProdukStatus, colProdukAksi);
        colProdukSumber.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getSumber() == null ? "-" : c.getValue().getSumber().getLabel()));

        bahanTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        produkTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        bahanTable.setPlaceholder(new Label("Belum ada data Bahan Baku."));
        produkTable.setPlaceholder(new Label("Belum ada data Produk Jadi."));

        searchBahanField.textProperty().addListener((obs, o, n) -> muatBahan());
        searchProdukField.textProperty().addListener((obs, o, n) -> muatProduk());
        muatSemua();
    }

    private void setupColumns(TableColumn<StokItem, String> nama, TableColumn<StokItem, String> satuan,
                              TableColumn<StokItem, String> batas, TableColumn<StokItem, String> stok,
                              TableColumn<StokItem, StokItem> status, TableColumn<StokItem, StokItem> aksi) {
        nama.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNama()));
        satuan.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSatuan().getLabel()));
        batas.setCellValueFactory(c -> new SimpleStringProperty(Formats.qty(c.getValue().getBatasMin())));
        stok.setCellValueFactory(c -> new SimpleStringProperty(
                Formats.qtyWithSatuan(c.getValue().getStok(), c.getValue().getSatuan())));
        status.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        status.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(StokItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(item.isDiBawahBatasAman()
                            ? Chips.of("Low Stock", "chip-red")
                            : Chips.of("In Stock", "chip-green"));
                }
            }
        });
        aksi.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        aksi.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(StokItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || !Session.isSupervisor()) {
                    setGraphic(null);
                    return;
                }
                Button edit = iconButton(Icons.EDIT, "#475467");
                edit.setOnAction(e -> onEdit(item));
                Button hapus = iconButton(Icons.TRASH, "#D92D20");
                hapus.setOnAction(e -> onHapus(item));
                setGraphic(new HBox(4, edit, hapus));
            }
        });
    }

    private Button iconButton(String icon, String warna) {
        Button btn = new Button();
        btn.getStyleClass().add("btn-icon");
        btn.setGraphic(Icons.of(icon, 14, Color.web(warna)));
        return btn;
    }

    private void muatSemua() {
        muatBahan();
        muatProduk();
    }

    private void muatBahan() {
        bahanTable.setItems(FXCollections.observableArrayList(
                masterService.daftarStok(JenisInventaris.BAHAN_BAKU, searchBahanField.getText())));
    }

    private void muatProduk() {
        produkTable.setItems(FXCollections.observableArrayList(
                masterService.daftarStok(JenisInventaris.PRODUK_JADI, searchProdukField.getText())));
    }

    @FXML
    private void onTambahData() {
        JenisInventaris jenis = tabPane.getSelectionModel().getSelectedIndex() == 0
                ? JenisInventaris.BAHAN_BAKU : JenisInventaris.PRODUK_JADI;
        bukaFormDialog(jenis, null);
    }

    private void onEdit(StokItem item) {
        bukaFormDialog(item.getJenisInventaris(), item);
    }

    private void onHapus(StokItem item) {
        boolean yakin = AlertUtil.confirm("Hapus Data",
                "Hapus \"" + item.getNama() + "\" dari master data?");
        if (!yakin) {
            return;
        }
        try {
            if (item.getJenisInventaris() == JenisInventaris.BAHAN_BAKU) {
                masterService.hapusBahanBaku(item.getBarangId());
            } else {
                masterService.hapusProdukJadi(item.getBarangId());
            }
            muatSemua();
            AlertUtil.info("Berhasil", "Data \"" + item.getNama() + "\" telah dihapus.");
        } catch (BusinessException e) {
            AlertUtil.error("Tidak Dapat Menghapus", e.getMessage());
        }
    }

    private void bukaFormDialog(JenisInventaris jenis, StokItem existing) {
        FXMLLoader loader = FxUtil.loader("item_form_dialog.fxml");
        ItemFormDialogController controller = loader.getController();
        controller.init(jenis, existing);

        Stage dialog = new Stage();
        dialog.setTitle((existing == null ? "Tambah " : "Ubah ") + jenis.getLabel());
        dialog.initModality(Modality.APPLICATION_MODAL);
        Scene scene = new Scene(loader.getRoot());
        FxUtil.applyStylesheet(scene);
        dialog.setScene(scene);
        FxUtil.showAndWait(dialog);
        if (controller.isTersimpan()) {
            muatSemua();
        }
    }
}
