package com.hakotjeria.controller;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

import com.hakotjeria.model.JenisInventaris;
import com.hakotjeria.model.JenisMutasi;
import com.hakotjeria.model.MutasiFilter;
import com.hakotjeria.model.MutasiStok;
import com.hakotjeria.model.RingkasanBahanBaku;
import com.hakotjeria.model.RingkasanMutasi;
import com.hakotjeria.model.Shift;
import com.hakotjeria.model.StokItem;
import com.hakotjeria.repository.ShiftRepository;
import com.hakotjeria.service.MutasiStokService;
import com.hakotjeria.util.AlertUtil;
import com.hakotjeria.util.BusinessException;
import com.hakotjeria.util.Chips;
import com.hakotjeria.util.Formats;
import com.hakotjeria.util.FxUtil;
import com.hakotjeria.util.PdfUtil;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

/**
 * Controller Riwayat Mutasi Stok (UC-09) dengan dua tabulasi Dual-Inventory,
 * penyaringan, ringkasan akumulasi, dan ekspor PDF.
 * Menyediakan pula akses pencatatan Mutasi Manual (UC-08).
 */
public class RiwayatMutasiController {

    @FXML
    private Button catatMutasiButton;
    @FXML
    private Button exportButton;
    @FXML
    private ComboBox<StokItem> barangCombo;
    @FXML
    private ComboBox<Object> jenisCombo;
    @FXML
    private ComboBox<Object> shiftCombo;
    @FXML
    private DatePicker dariPicker;
    @FXML
    private DatePicker sampaiPicker;
    @FXML
    private TabPane tabPane;
    @FXML
    private TableView<MutasiStok> bahanTable;
    @FXML
    private TableColumn<MutasiStok, String> colBbTanggal;
    @FXML
    private TableColumn<MutasiStok, String> colBbShift;
    @FXML
    private TableColumn<MutasiStok, String> colBbNama;
    @FXML
    private TableColumn<MutasiStok, MutasiStok> colBbJenis;
    @FXML
    private TableColumn<MutasiStok, String> colBbQty;
    @FXML
    private TableColumn<MutasiStok, String> colBbKet;
    @FXML
    private TableColumn<MutasiStok, String> colBbUser;
    @FXML
    private TableView<RingkasanBahanBaku> ringkasanBahanTable;
    @FXML
    private TableColumn<RingkasanBahanBaku, String> colRbNama;
    @FXML
    private TableColumn<RingkasanBahanBaku, String> colRbSatuan;
    @FXML
    private TableColumn<RingkasanBahanBaku, String> colRbStokAwal;
    @FXML
    private TableColumn<RingkasanBahanBaku, String> colRbIn;
    @FXML
    private TableColumn<RingkasanBahanBaku, String> colRbOut;
    @FXML
    private TableColumn<RingkasanBahanBaku, String> colRbStokAkhir;
    @FXML
    private TableView<MutasiStok> produkTable;
    @FXML
    private TableColumn<MutasiStok, String> colPjTanggal;
    @FXML
    private TableColumn<MutasiStok, String> colPjShift;
    @FXML
    private TableColumn<MutasiStok, String> colPjNama;
    @FXML
    private TableColumn<MutasiStok, MutasiStok> colPjJenis;
    @FXML
    private TableColumn<MutasiStok, String> colPjQty;
    @FXML
    private TableColumn<MutasiStok, String> colPjKet;
    @FXML
    private TableColumn<MutasiStok, String> colPjUser;
    @FXML
    private HBox ringkasanProdukBox;

    private static final String SEMUA = "Semua";

    private final MutasiStokService mutasiService = new MutasiStokService();
    private final ShiftRepository shiftRepo = new ShiftRepository();

    @FXML
    private void initialize() {
        setupTable(colBbTanggal, colBbShift, colBbNama, colBbJenis, colBbQty, colBbKet, colBbUser, bahanTable);
        setupTable(colPjTanggal, colPjShift, colPjNama, colPjJenis, colPjQty, colPjKet, colPjUser, produkTable);
        setupRingkasanBahanTable();

        jenisCombo.getItems().addAll(SEMUA, JenisMutasi.IN, JenisMutasi.OUT);
        jenisCombo.setValue(SEMUA);
        shiftCombo.getItems().add(SEMUA);
        shiftCombo.getItems().addAll(shiftRepo.findAll());
        shiftCombo.setValue(SEMUA);
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

        tabPane.getSelectionModel().selectedIndexProperty().addListener((obs, o, n) -> muatBarangCombo(aktifJenis()));
        muatBarangCombo(JenisInventaris.BAHAN_BAKU);
        muatData();
    }

    private JenisInventaris aktifJenis() {
        return tabPane.getSelectionModel().getSelectedIndex() == 0
                ? JenisInventaris.BAHAN_BAKU : JenisInventaris.PRODUK_JADI;
    }

    private void muatBarangCombo(JenisInventaris jenis) {
        StokItem terpilih = barangCombo.getValue();
        barangCombo.getItems().clear();
        StokItem semua = new StokItem();
        semua.setNama(SEMUA);
        semua.setBarangId(-1);
        barangCombo.getItems().add(semua);
        barangCombo.getItems().addAll(mutasiService.stokSemua(jenis));
        barangCombo.setValue(barangCombo.getItems().stream()
                .filter(i -> terpilih != null && i.getBarangId() == terpilih.getBarangId())
                .findFirst().orElse(semua));
    }

    private void setupTable(TableColumn<MutasiStok, String> tanggal, TableColumn<MutasiStok, String> shift,
                            TableColumn<MutasiStok, String> nama, TableColumn<MutasiStok, MutasiStok> jenis,
                            TableColumn<MutasiStok, String> qty, TableColumn<MutasiStok, String> ket,
                            TableColumn<MutasiStok, String> user, TableView<MutasiStok> table) {
        tanggal.setCellValueFactory(c -> new SimpleStringProperty(Formats.tanggal(c.getValue().getTanggal())));
        shift.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNamaShift()));
        nama.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNamaBarang()));
        jenis.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        jenis.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(MutasiStok item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || item == null ? null : Chips.jenisMutasi(item.getJenis()));
            }
        });
        qty.setCellValueFactory(c -> new SimpleStringProperty(
                Formats.qtyWithSatuan(c.getValue().getQty(), c.getValue().getSatuanBarang())));
        ket.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getKeterangan()));
        user.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getNamaPenginput() == null ? "-" : c.getValue().getNamaPenginput()));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Belum ada mutasi pada filter ini."));
    }

    private void setupRingkasanBahanTable() {
        colRbNama.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNama()));
        colRbSatuan.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSatuan().getLabel()));
        colRbStokAwal.setCellValueFactory(c -> new SimpleStringProperty(Formats.qty(c.getValue().getStokAwal())));
        colRbIn.setCellValueFactory(c -> new SimpleStringProperty(Formats.qty(c.getValue().getTotalIn())));
        colRbOut.setCellValueFactory(c -> new SimpleStringProperty(Formats.qty(c.getValue().getTotalOut())));
        colRbStokAkhir.setCellValueFactory(c -> new SimpleStringProperty(Formats.qty(c.getValue().getStokAkhir())));
        ringkasanBahanTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        ringkasanBahanTable.setPlaceholder(new Label("Belum ada mutasi bahan baku pada filter ini."));
    }

    private MutasiFilter buildFilter(JenisInventaris jenis) {
        MutasiFilter f = new MutasiFilter();
        f.setJenisInventaris(jenis);
        f.setDariTanggal(dariPicker.getValue());
        f.setSampaiTanggal(sampaiPicker.getValue());
        if (shiftCombo.getValue() instanceof Shift shift) {
            f.setShiftId(shift.getId());
        }
        if (jenisCombo.getValue() instanceof JenisMutasi jm) {
            f.setJenis(jm);
        }
        StokItem barang = barangCombo.getValue();
        if (barang != null && barang.getBarangId() >= 0) {
            f.setBarangId(barang.getBarangId());
        }
        return f;
    }

    private void muatData() {
        muatBahanTab();
        muatTab(JenisInventaris.PRODUK_JADI, produkTable, ringkasanProdukBox);
    }

    private void muatBahanTab() {
        MutasiFilter f = buildFilter(JenisInventaris.BAHAN_BAKU);
        List<MutasiStok> rows = mutasiService.riwayat(f);
        bahanTable.setItems(FXCollections.observableArrayList(rows));
        ringkasanBahanTable.setItems(FXCollections.observableArrayList(mutasiService.ringkasanPerBahanBaku(f)));
    }

    private void muatTab(JenisInventaris jenis, TableView<MutasiStok> table, HBox ringkasanBox) {
        MutasiFilter f = buildFilter(jenis);
        List<MutasiStok> rows = mutasiService.riwayat(f);
        table.setItems(FXCollections.observableArrayList(rows));
        RingkasanMutasi r = mutasiService.ringkasan(f);
        ringkasanBox.getChildren().setAll(
                ringkasanItem("Stok Awal", f.getBarangId() != null ? Formats.qty(r.getStokAwal()) : "—"),
                ringkasanItem("Total IN", Formats.qty(r.getTotalIn())),
                ringkasanItem("Total OUT", Formats.qty(r.getTotalOut())),
                ringkasanItem("Stok Akhir", f.getBarangId() != null ? Formats.qty(r.getStokAkhir()) : "—"));
    }

    private VBox ringkasanItem(String label, String value) {
        Label l = new Label(label);
        l.getStyleClass().add("metric-label");
        Label v = new Label(value);
        v.getStyleClass().add("label-strong");
        return new VBox(2, l, v);
    }

    @FXML
    private void onFilter() {
        muatData();
    }

    @FXML
    private void onReset() {
        dariPicker.setValue(null);
        sampaiPicker.setValue(null);
        shiftCombo.setValue(SEMUA);
        jenisCombo.setValue(SEMUA);
        muatBarangCombo(aktifJenis());
        muatData();
    }

    @FXML
    private void onCatatMutasi() {
        FXMLLoader loader = FxUtil.loader("mutasi_manual_dialog.fxml");
        MutasiManualDialogController controller = loader.getController();
        controller.init(aktifJenis());
        Stage dialog = new Stage();
        dialog.setTitle("Catat Mutasi Stok Manual - Hako Tjeria");
        dialog.initModality(Modality.APPLICATION_MODAL);
        Scene scene = new Scene(loader.getRoot());
        FxUtil.applyStylesheet(scene);
        dialog.setScene(scene);
        FxUtil.showAndWait(dialog);
        if (controller.isTersimpan()) {
            muatBarangCombo(aktifJenis());
            muatData();
        }
    }

    /** Ekspor riwayat mutasi terfilter tab aktif ke PDF (R09.7). */
    @FXML
    private void onExportPdf() {
        JenisInventaris jenis = aktifJenis();
        MutasiFilter f = buildFilter(jenis);
        List<MutasiStok> rows = jenis == JenisInventaris.BAHAN_BAKU
                ? bahanTable.getItems() : produkTable.getItems();
        if (rows.isEmpty()) {
            AlertUtil.warn("Tidak Ada Data", "Tidak ada baris mutasi untuk diekspor pada filter ini.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Simpan Laporan PDF");
        chooser.setInitialFileName("Riwayat_Mutasi_" + jenis.name() + ".pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Dokumen PDF", "*.pdf"));
        File target = chooser.showSaveDialog(exportButton.getScene().getWindow());
        if (target == null) {
            return;
        }
        try {
            if (jenis == JenisInventaris.BAHAN_BAKU) {
                List<RingkasanBahanBaku> ringkasanPerBarang = mutasiService.ringkasanPerBahanBaku(f);
                PdfUtil.exportMutasi(target, jenis.getLabel(), deskripsiFilter(f), rows, null, false,
                        ringkasanPerBarang);
            } else {
                RingkasanMutasi ringkasan = mutasiService.ringkasan(f);
                PdfUtil.exportMutasi(target, jenis.getLabel(), deskripsiFilter(f), rows, ringkasan,
                        f.getBarangId() != null);
            }
            AlertUtil.info("Export Berhasil", "Laporan PDF tersimpan di:\n" + target.getAbsolutePath());
        } catch (BusinessException e) {
            AlertUtil.error("Export Gagal", e.getMessage());
        }
    }

    private String deskripsiFilter(MutasiFilter f) {
        StringBuilder sb = new StringBuilder();
        if (f.getDariTanggal() != null || f.getSampaiTanggal() != null) {
            sb.append("Periode ").append(Formats.tanggal(f.getDariTanggal()))
                    .append(" s/d ").append(Formats.tanggal(f.getSampaiTanggal())).append("  ");
        }
        if (f.getShiftId() != null) {
            Shift s = shiftRepo.findById(f.getShiftId());
            sb.append("Shift: ").append(s == null ? f.getShiftId() : s.getNamaShift()).append("  ");
        }
        if (f.getJenis() != null) {
            sb.append("Jenis: ").append(f.getJenis().name()).append("  ");
        }
        return sb.toString().trim();
    }
}
