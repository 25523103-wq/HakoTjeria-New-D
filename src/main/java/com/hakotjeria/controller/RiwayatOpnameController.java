package com.hakotjeria.controller;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

import com.hakotjeria.model.DetailStockOpname;
import com.hakotjeria.model.MutasiStok;
import com.hakotjeria.model.Shift;
import com.hakotjeria.model.StockOpname;
import com.hakotjeria.repository.ShiftRepository;
import com.hakotjeria.service.StockOpnameService;
import com.hakotjeria.util.AlertUtil;
import com.hakotjeria.util.BusinessException;
import com.hakotjeria.util.Chips;
import com.hakotjeria.util.Formats;
import com.hakotjeria.util.PdfUtil;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Controller Riwayat Stock Opname (UC-11): daftar dokumen tervalidasi,
 * detail selisih, dan ekspor PDF berkop (R11.1 - R11.6). Data Read-Only.
 */
public class RiwayatOpnameController {

    @FXML
    private Button exportButton;
    @FXML
    private TextField cariField;
    @FXML
    private ComboBox<Object> shiftCombo;
    @FXML
    private DatePicker dariPicker;
    @FXML
    private DatePicker sampaiPicker;
    @FXML
    private TableView<StockOpname> opnameTable;
    @FXML
    private TableColumn<StockOpname, String> colKode;
    @FXML
    private TableColumn<StockOpname, String> colTanggal;
    @FXML
    private TableColumn<StockOpname, String> colShift;
    @FXML
    private TableColumn<StockOpname, String> colPetugas;
    @FXML
    private TableColumn<StockOpname, String> colTotalSelisih;
    @FXML
    private TableColumn<StockOpname, StockOpname> colStatus;
    @FXML
    private VBox detailCard;
    @FXML
    private VBox detailHeaderBox;
    @FXML
    private TableView<DetailStockOpname> detailTable;
    @FXML
    private TableColumn<DetailStockOpname, String> colDetNama;
    @FXML
    private TableColumn<DetailStockOpname, String> colDetSistem;
    @FXML
    private TableColumn<DetailStockOpname, String> colDetFisik;
    @FXML
    private TableColumn<DetailStockOpname, DetailStockOpname> colDetSelisih;

    private static final String SEMUA = "Semua";

    private final StockOpnameService opnameService = new StockOpnameService();
    private final ShiftRepository shiftRepo = new ShiftRepository();
    private StockOpname terpilih;

    @FXML
    private void initialize() {
        shiftCombo.getItems().add(SEMUA);
        shiftCombo.getItems().addAll(shiftRepo.findAll());
        shiftCombo.setValue(SEMUA);

        colKode.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getKode()));
        colTanggal.setCellValueFactory(c -> new SimpleStringProperty(Formats.tanggal(c.getValue().getTanggal())));
        colShift.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNamaShift()));
        colPetugas.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getNamaPenginput() == null ? "-" : c.getValue().getNamaPenginput()));
        colTotalSelisih.setCellValueFactory(c -> new SimpleStringProperty(
                Formats.qty(c.getValue().getTotalSelisih())));
        colStatus.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(StockOpname item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || item == null ? null : Chips.statusValidasi(item.getStatus()));
            }
        });
        opnameTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        opnameTable.setPlaceholder(new Label("Belum ada dokumen Stock Opname tervalidasi."));
        opnameTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> tampilkanDetail(n));

        colDetNama.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNamaBarang()));
        colDetSistem.setCellValueFactory(c -> new SimpleStringProperty(Formats.qty(c.getValue().getStokSistem())));
        colDetFisik.setCellValueFactory(c -> new SimpleStringProperty(Formats.qty(c.getValue().getStokFisik())));
        colDetSelisih.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colDetSelisih.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(DetailStockOpname item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                BigDecimalChip(item);
            }

            private void BigDecimalChip(DetailStockOpname item) {
                java.math.BigDecimal s = item.getSelisih();
                String label = (s.signum() > 0 ? "+" : "") + Formats.qty(s);
                String warna = s.signum() == 0 ? "chip-green" : (s.signum() > 0 ? "chip-blue" : "chip-red");
                setGraphic(Chips.of(label, warna));
            }
        });
        detailTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        detailTable.setPlaceholder(new Label("Pilih dokumen untuk melihat rincian."));

        muatData();
    }

    private Long shiftTerpilih() {
        return shiftCombo.getValue() instanceof Shift shift ? shift.getId() : null;
    }

    private void muatData() {
        List<StockOpname> semua = opnameService.riwayat(dariPicker.getValue(), sampaiPicker.getValue(), shiftTerpilih());
        String kunci = cariField.getText() == null ? "" : cariField.getText().trim().toLowerCase();
        List<StockOpname> hasil = semua.stream()
                .filter(o -> kunci.isEmpty()
                        || (o.getKode() != null && o.getKode().toLowerCase().contains(kunci))
                        || (o.getNamaPenginput() != null && o.getNamaPenginput().toLowerCase().contains(kunci)))
                .toList();
        opnameTable.setItems(FXCollections.observableArrayList(hasil));
        detailHeaderBox.getChildren().clear();
        detailTable.setItems(FXCollections.observableArrayList());
        terpilih = null;
    }

    private void tampilkanDetail(StockOpname header) {
        detailHeaderBox.getChildren().clear();
        if (header == null) {
            return;
        }
        terpilih = opnameService.detail(header.getId());
        detailHeaderBox.getChildren().addAll(
                barisInfo("Kode", terpilih.getKode()),
                barisInfo("Tanggal / Shift", Formats.tanggal(terpilih.getTanggal()) + " · " + terpilih.getNamaShift()),
                barisInfo("Staff Penginput", nz(terpilih.getNamaPenginput())),
                barisInfo("Supervisor Validator", nz(terpilih.getNamaValidator())),
                barisInfo("Waktu Validasi", Formats.tanggalWaktu(terpilih.getValidAt())));
        detailTable.setItems(FXCollections.observableArrayList(terpilih.getDetail()));
    }

    private HBox barisInfo(String label, String value) {
        Label l = new Label(label + ":");
        l.getStyleClass().add("metric-label");
        l.setMinWidth(150);
        Label v = new Label(value);
        v.getStyleClass().add("label-strong");
        v.setWrapText(true);
        return new HBox(6, l, v);
    }

    private String nz(String s) {
        return s == null ? "-" : s;
    }

    @FXML
    private void onFilter() {
        muatData();
    }

    @FXML
    private void onReset() {
        cariField.clear();
        shiftCombo.setValue(SEMUA);
        dariPicker.setValue(null);
        sampaiPicker.setValue(null);
        muatData();
    }

    /** Ekspor dokumen terpilih ke PDF berkop dengan tabel selisih (R11.6). */
    @FXML
    private void onExportPdf() {
        if (terpilih == null) {
            AlertUtil.warn("Pilih Dokumen", "Pilih salah satu dokumen Stock Opname terlebih dahulu.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Simpan Laporan PDF");
        chooser.setInitialFileName(terpilih.getKode() + ".pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Dokumen PDF", "*.pdf"));
        File target = chooser.showSaveDialog(exportButton.getScene().getWindow());
        if (target == null) {
            return;
        }
        try {
            List<MutasiStok> penyesuaian = opnameService.mutasiPenyesuaian(terpilih);
            PdfUtil.exportStockOpname(target, terpilih, penyesuaian);
            AlertUtil.info("Export Berhasil", "Laporan PDF tersimpan di:\n" + target.getAbsolutePath());
        } catch (BusinessException e) {
            AlertUtil.error("Export Gagal", e.getMessage());
        }
    }
}
