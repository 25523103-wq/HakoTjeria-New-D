package com.hakotjeria.controller;

import java.time.LocalDate;
import java.util.List;

import com.hakotjeria.model.JadwalTugas;
import com.hakotjeria.model.KategoriTugas;
import com.hakotjeria.model.Shift;
import com.hakotjeria.model.StatusTugas;
import com.hakotjeria.repository.ShiftRepository;
import com.hakotjeria.service.JadwalTugasService;
import com.hakotjeria.service.StockOpnameService;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Controller Jadwal Tugas Operasional (UC-05) beserta aksi eksekusi
 * Produksi Internal (UC-06) dan Pengambilan Eksternal (UC-07).
 */
public class JadwalTugasController {

    @FXML
    private Button tambahTugasButton;
    @FXML
    private DatePicker tanggalPicker;
    @FXML
    private ComboBox<Object> shiftCombo;
    @FXML
    private Label lockChipHolder;
    @FXML
    private Label infoShiftAktifLabel;
    @FXML
    private StackPane internalIcon;
    @FXML
    private Label internalCountChip;
    @FXML
    private TableView<JadwalTugas> internalTable;
    @FXML
    private TableColumn<JadwalTugas, String> colIntProduk;
    @FXML
    private TableColumn<JadwalTugas, String> colIntTarget;
    @FXML
    private TableColumn<JadwalTugas, String> colIntAktual;
    @FXML
    private TableColumn<JadwalTugas, String> colIntPj;
    @FXML
    private TableColumn<JadwalTugas, JadwalTugas> colIntStatus;
    @FXML
    private TableColumn<JadwalTugas, JadwalTugas> colIntAksi;
    @FXML
    private StackPane eksternalIcon;
    @FXML
    private Label eksternalCountChip;
    @FXML
    private TableView<JadwalTugas> eksternalTable;
    @FXML
    private TableColumn<JadwalTugas, String> colEksProduk;
    @FXML
    private TableColumn<JadwalTugas, String> colEksTarget;
    @FXML
    private TableColumn<JadwalTugas, String> colEksDiterima;
    @FXML
    private TableColumn<JadwalTugas, String> colEksPj;
    @FXML
    private TableColumn<JadwalTugas, JadwalTugas> colEksStatus;
    @FXML
    private TableColumn<JadwalTugas, JadwalTugas> colEksAksi;

    private static final String SEMUA_SHIFT = "Semua Shift";

    private final JadwalTugasService tugasService = new JadwalTugasService();
    private final StockOpnameService opnameService = new StockOpnameService();
    private final ShiftRepository shiftRepo = new ShiftRepository();

    @FXML
    private void initialize() {
        internalIcon.getChildren().add(Icons.of(Icons.BREAD, 18, Color.web("#9DB2F0")));
        eksternalIcon.getChildren().add(Icons.of(Icons.SWAP, 18, Color.web("#B54708")));

        boolean supervisor = Session.isSupervisor();
        tambahTugasButton.setVisible(supervisor);
        tambahTugasButton.setManaged(supervisor);

        tanggalPicker.setValue(LocalDate.now());
        List<Shift> shifts = shiftRepo.findAll();
        shiftCombo.getItems().add(SEMUA_SHIFT);
        shiftCombo.getItems().addAll(shifts);
        shiftCombo.setValue(SEMUA_SHIFT);
        Shift aktif = Session.getActiveShift();
        infoShiftAktifLabel.setText("Shift aktif sekarang: " + aktif.getNamaShift()
                + " (" + aktif.getJamMulai() + " - " + aktif.getJamSelesai() + ")");

        setupInternalTable();
        setupEksternalTable();
        tanggalPicker.valueProperty().addListener((obs, o, n) -> muatData());
        shiftCombo.valueProperty().addListener((obs, o, n) -> muatData());
        muatData();
    }

    private Long shiftTerpilih() {
        Object value = shiftCombo.getValue();
        return value instanceof Shift shift ? shift.getId() : null;
    }

    private void muatData() {
        LocalDate tanggal = tanggalPicker.getValue() == null ? LocalDate.now() : tanggalPicker.getValue();
        List<JadwalTugas> semua = tugasService.daftarTugas(tanggal, shiftTerpilih());
        List<JadwalTugas> internal = semua.stream()
                .filter(t -> t.getKategori() == KategoriTugas.PRODUKSI_INTERNAL).toList();
        List<JadwalTugas> eksternal = semua.stream()
                .filter(t -> t.getKategori() == KategoriTugas.PENGAMBILAN_EKSTERNAL).toList();
        internalTable.setItems(FXCollections.observableArrayList(internal));
        eksternalTable.setItems(FXCollections.observableArrayList(eksternal));
        internalCountChip.setGraphic(Chips.of(internal.size() + " tugas", "chip-blue"));
        eksternalCountChip.setGraphic(Chips.of(eksternal.size() + " tugas", "chip-gray"));
        perbaruiChipKunci(tanggal);
    }

    /** Menampilkan penanda kunci shift setelah opname disetujui (R10.6). */
    private void perbaruiChipKunci(LocalDate tanggal) {
        Long shiftId = shiftTerpilih();
        boolean terkunci;
        if (shiftId == null) {
            terkunci = shiftRepo.findAll().stream()
                    .allMatch(s -> opnameService.isShiftTerkunci(tanggal, s.getId()));
        } else {
            terkunci = opnameService.isShiftTerkunci(tanggal, shiftId);
        }
        lockChipHolder.setGraphic(terkunci ? Chips.of("Shift Terkunci (Opname Disetujui)", "chip-red") : null);
    }

    private void setupInternalTable() {
        colIntProduk.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getNamaProduk() + "  ·  " + c.getValue().getNamaShift()));
        colIntTarget.setCellValueFactory(c -> new SimpleStringProperty(
                Formats.qtyWithSatuan(c.getValue().getQtyTarget(), c.getValue().getSatuanProduk())));
        colIntAktual.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getQtyAktual() == null ? "-"
                        : Formats.qtyWithSatuan(c.getValue().getQtyAktual(), c.getValue().getSatuanProduk())));
        colIntPj.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getNamaStaff() == null ? "-" : c.getValue().getNamaStaff()));
        setupStatusColumn(colIntStatus);
        setupAksiColumn(colIntAksi, true);
        internalTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        internalTable.setPlaceholder(new Label("Tidak ada tugas Produksi Internal pada filter ini."));
    }

    private void setupEksternalTable() {
        colEksProduk.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getNamaProduk() + "  ·  " + c.getValue().getNamaShift()));
        colEksTarget.setCellValueFactory(c -> new SimpleStringProperty(
                Formats.qtyWithSatuan(c.getValue().getQtyTarget(), c.getValue().getSatuanProduk())));
        colEksDiterima.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getQtyDiterima() == null ? "-"
                        : Formats.qtyWithSatuan(c.getValue().getQtyDiterima(), c.getValue().getSatuanProduk())));
        colEksPj.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getNamaStaff() == null ? "-" : c.getValue().getNamaStaff()));
        setupStatusColumn(colEksStatus);
        setupAksiColumn(colEksAksi, false);
        eksternalTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        eksternalTable.setPlaceholder(new Label("Tidak ada tugas Pengambilan Eksternal pada filter ini."));
    }

    private void setupStatusColumn(TableColumn<JadwalTugas, JadwalTugas> col) {
        col.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        col.setCellFactory(x -> new TableCell<>() {
            @Override
            protected void updateItem(JadwalTugas item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Label chip = Chips.statusTugas(item.getStatus());
                if (item.getStatus() == StatusTugas.TIDAK_TERPENUHI && item.getCatatan() != null) {
                    Tooltip.install(chip, new Tooltip("Alasan: " + item.getCatatan()));
                }
                setGraphic(chip);
            }
        });
    }

    private void setupAksiColumn(TableColumn<JadwalTugas, JadwalTugas> col, boolean internal) {
        col.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        col.setCellFactory(x -> new TableCell<>() {
            @Override
            protected void updateItem(JadwalTugas tugas, boolean empty) {
                super.updateItem(tugas, empty);
                if (empty || tugas == null) {
                    setGraphic(null);
                    return;
                }
                HBox box = new HBox(4);
                boolean shiftTerkunci = opnameService.isShiftTerkunci(tugas.getTanggal(), tugas.getShiftId());
                boolean bisaAksi = !tugas.getStatus().isFinal() && !shiftTerkunci;

                if (Session.isStaff() && bisaAksi) {
                    if (internal) {
                        Button selesai = smallButton("Selesai", "btn-success");
                        selesai.setOnAction(e -> onSelesai(tugas));
                        box.getChildren().add(selesai);
                    } else {
                        Button diambil = smallButton("Sudah Diambil", "btn-success");
                        diambil.setOnAction(e -> onSudahDiambil(tugas));
                        box.getChildren().add(diambil);
                    }
                    Button gagal = smallButton("Tidak Terpenuhi", "btn-danger");
                    gagal.setOnAction(e -> onTidakTerpenuhi(tugas));
                    box.getChildren().add(gagal);
                }
                if (Session.isSupervisor() && bisaAksi) {
                    Button edit = iconButton(Icons.EDIT, "#475467");
                    edit.setTooltip(new Tooltip("Ubah tugas"));
                    edit.setOnAction(e -> onEditTugas(tugas));
                    Button hapus = iconButton(Icons.TRASH, "#D92D20");
                    hapus.setTooltip(new Tooltip("Hapus tugas"));
                    hapus.setOnAction(e -> onHapusTugas(tugas));
                    box.getChildren().addAll(edit, hapus);
                }
                if (!bisaAksi) {
                    box.getChildren().add(Icons.boxed(Icons.LOCK, 14, Color.web("#98A2B3")));
                }
                setGraphic(box);
            }
        });
    }

    private Button smallButton(String text, String styleClass) {
        Button btn = new Button(text);
        btn.getStyleClass().add(styleClass);
        btn.setStyle("-fx-padding: 5 10 5 10; -fx-font-size: 11px;");
        return btn;
    }

    private Button iconButton(String icon, String warna) {
        Button btn = new Button();
        btn.getStyleClass().add("btn-icon");
        btn.setGraphic(Icons.of(icon, 14, Color.web(warna)));
        return btn;
    }

    // ===================== AKSI =====================

    @FXML
    private void onTambahTugas() {
        bukaFormTugas(null);
    }

    private void onEditTugas(JadwalTugas tugas) {
        bukaFormTugas(tugas);
    }

    private void onHapusTugas(JadwalTugas tugas) {
        boolean yakin = AlertUtil.confirm("Hapus Tugas",
                "Hapus tugas \"" + tugas.getNamaProduk() + "\" (" + tugas.getKategori().getLabel() + ")?");
        if (!yakin) {
            return;
        }
        try {
            tugasService.hapusTugas(tugas.getId());
            muatData();
        } catch (BusinessException e) {
            AlertUtil.error("Tidak Dapat Menghapus", e.getMessage());
        }
    }

    /** Tombol "Selesai" memicu Pop-Up Qty Aktual dengan pratinjau BOM (R06.1, ER05, ER06). */
    private void onSelesai(JadwalTugas tugas) {
        FXMLLoader loader = FxUtil.loader("qty_aktual_dialog.fxml");
        QtyAktualDialogController controller = loader.getController();
        controller.init(tugas);
        tampilkanDialog(loader, "Konfirmasi Produksi - " + tugas.getNamaProduk());
        if (controller.isBerhasil()) {
            muatData();
        }
    }

    /** Perubahan status "Sudah Diambil" memicu Pop-Up Qty Diterima (R07.2, ER07). */
    private void onSudahDiambil(JadwalTugas tugas) {
        FXMLLoader loader = FxUtil.loader("qty_diterima_dialog.fxml");
        QtyDiterimaDialogController controller = loader.getController();
        controller.init(tugas);
        tampilkanDialog(loader, "Konfirmasi Penerimaan - " + tugas.getNamaProduk());
        if (controller.isBerhasil()) {
            muatData();
        }
    }

    /** Dialog alasan kegagalan tugas (alur alternatif UC-06/UC-07). */
    private void onTidakTerpenuhi(JadwalTugas tugas) {
        FXMLLoader loader = FxUtil.loader("reason_dialog.fxml");
        ReasonDialogController controller = loader.getController();
        controller.init("Tugas Tidak Terpenuhi",
                "Isi alasan kegagalan tugas \"" + tugas.getNamaProduk() + "\" (mis. mati lampu, oven rusak).",
                "Simpan");
        tampilkanDialog(loader, "Tugas Tidak Terpenuhi");
        String alasan = controller.getHasil();
        if (alasan == null) {
            return;
        }
        try {
            tugasService.setTidakTerpenuhi(tugas.getId(), alasan);
            muatData();
        } catch (BusinessException e) {
            AlertUtil.error("Gagal", e.getMessage());
        }
    }

    private void bukaFormTugas(JadwalTugas existing) {
        FXMLLoader loader = FxUtil.loader("tugas_form_dialog.fxml");
        TugasFormDialogController controller = loader.getController();
        controller.init(existing, tanggalPicker.getValue());
        tampilkanDialog(loader, existing == null ? "Tambah Tugas" : "Ubah Tugas");
        if (controller.isTersimpan()) {
            muatData();
        }
    }

    private void tampilkanDialog(FXMLLoader loader, String judul) {
        Stage dialog = new Stage();
        dialog.setTitle(judul + " - Hako Tjeria");
        dialog.initModality(Modality.APPLICATION_MODAL);
        Scene scene = new Scene(loader.getRoot());
        FxUtil.applyStylesheet(scene);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
