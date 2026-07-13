package com.hakotjeria.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.hakotjeria.model.DetailStockOpname;
import com.hakotjeria.model.JenisInventaris;
import com.hakotjeria.model.Shift;
import com.hakotjeria.model.StatusValidasi;
import com.hakotjeria.model.StockOpname;
import com.hakotjeria.repository.ShiftRepository;
import com.hakotjeria.service.StockOpnameService;
import com.hakotjeria.util.AlertUtil;
import com.hakotjeria.util.BusinessException;
import com.hakotjeria.util.Chips;
import com.hakotjeria.util.Formats;
import com.hakotjeria.util.FxUtil;
import com.hakotjeria.util.Session;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.converter.DefaultStringConverter;

/**
 * Controller Stock Opname (UC-10). Staff mengisi Stok Fisik dan mengirim ajuan;
 * Supervisor menyetujui/menolak. Selisih dihitung otomatis (R10.2, SR12).
 */
public class StockOpnameController {

    @FXML
    private Label statusChipHolder;
    @FXML
    private DatePicker tanggalPicker;
    @FXML
    private ComboBox<Shift> shiftCombo;
    @FXML
    private HBox infoBox;
    @FXML
    private Label infoLabel;
    @FXML
    private TextField cariField;
    @FXML
    private Label totalSelisihLabel;
    @FXML
    private TabPane tabPane;
    @FXML
    private TableView<DetailStockOpname> bahanTable;
    @FXML
    private TableColumn<DetailStockOpname, String> colBbNama;
    @FXML
    private TableColumn<DetailStockOpname, String> colBbSatuan;
    @FXML
    private TableColumn<DetailStockOpname, String> colBbSistem;
    @FXML
    private TableColumn<DetailStockOpname, String> colBbFisik;
    @FXML
    private TableColumn<DetailStockOpname, DetailStockOpname> colBbSelisih;
    @FXML
    private TableView<DetailStockOpname> produkTable;
    @FXML
    private TableColumn<DetailStockOpname, String> colPjNama;
    @FXML
    private TableColumn<DetailStockOpname, String> colPjSatuan;
    @FXML
    private TableColumn<DetailStockOpname, String> colPjSistem;
    @FXML
    private TableColumn<DetailStockOpname, String> colPjFisik;
    @FXML
    private TableColumn<DetailStockOpname, DetailStockOpname> colPjSelisih;
    @FXML
    private Button simpanDraftButton;
    @FXML
    private Button kirimButton;
    @FXML
    private Button setujuiButton;
    @FXML
    private Button tolakButton;

    private final StockOpnameService opnameService = new StockOpnameService();
    private final ShiftRepository shiftRepo = new ShiftRepository();

    private StockOpname dokumen;
    private final ObservableList<DetailStockOpname> bahanItems = FXCollections.observableArrayList();
    private final ObservableList<DetailStockOpname> produkItems = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        tanggalPicker.setValue(LocalDate.now());
        shiftCombo.setItems(FXCollections.observableArrayList(shiftRepo.findAll()));
        Shift aktif = Session.getActiveShift();
        shiftCombo.getItems().stream()
                .filter(s -> s.getId() == aktif.getId())
                .findFirst()
                .ifPresent(shiftCombo::setValue);

        setupTable(colBbNama, colBbSatuan, colBbSistem, colBbFisik, colBbSelisih, bahanTable, bahanItems);
        setupTable(colPjNama, colPjSatuan, colPjSistem, colPjFisik, colPjSelisih, produkTable, produkItems);

        cariField.textProperty().addListener((obs, o, n) -> terapkanFilter());
        onMuat();
    }

    @FXML
    private void onMuat() {
        Shift shift = shiftCombo.getValue();
        if (shift == null) {
            AlertUtil.warn("Pilih Shift", "Silakan pilih shift terlebih dahulu.");
            return;
        }
        LocalDate tanggal = tanggalPicker.getValue();
        try {
            dokumen = opnameService.muatDokumen(tanggal, shift.getId());
        } catch (BusinessException e) {
            AlertUtil.error("Gagal Memuat", e.getMessage());
            return;
        }
        tampilkanDokumen();
    }

    private void tampilkanDokumen() {
        statusChipHolder.setGraphic(Chips.statusValidasi(dokumen.getStatus()));
        bahanItems.setAll(dokumen.getDetail().stream()
                .filter(d -> d.getJenisInventaris() == JenisInventaris.BAHAN_BAKU).toList());
        produkItems.setAll(dokumen.getDetail().stream()
                .filter(d -> d.getJenisInventaris() == JenisInventaris.PRODUK_JADI).toList());
        terapkanFilter();
        aturKontrol();
        perbaruiTotalSelisih();
    }

    /** Menyesuaikan tombol dan editabilitas menurut status & peran (R10.3, R10.4). */
    private void aturKontrol() {
        boolean editable = opnameService.bisaDiedit(dokumen) && Session.isStaff();
        boolean draftBaru = opnameService.bisaDiedit(dokumen);
        boolean menunggu = dokumen.getStatus() == StatusValidasi.MENUNGGU_VALIDASI;
        boolean supervisor = Session.isSupervisor();

        bahanTable.setEditable(editable);
        produkTable.setEditable(editable);

        // Staff menyusun & mengirim; Supervisor memvalidasi.
        simpanDraftButton.setVisible(editable);
        simpanDraftButton.setManaged(editable);
        kirimButton.setVisible(editable);
        kirimButton.setManaged(editable);
        setujuiButton.setVisible(supervisor && menunggu);
        setujuiButton.setManaged(supervisor && menunggu);
        tolakButton.setVisible(supervisor && menunggu);
        tolakButton.setManaged(supervisor && menunggu);

        String pesan = null;
        if (dokumen.getStatus() == StatusValidasi.TERVALIDASI) {
            pesan = "Dokumen telah tervalidasi. Shift terkunci permanen dan data bersifat Read-Only (R10.6, R11.5).";
        } else if (menunggu) {
            pesan = supervisor
                    ? "Ajuan menunggu validasi Anda. Tinjau nilai selisih, lalu Setujui atau Tolak."
                    : "Ajuan sedang menunggu validasi Supervisor. Angka terkunci hingga divalidasi (R10.4).";
        } else if (draftBaru && dokumen.getCatatanValidasi() != null) {
            pesan = "Draf Revisi — ditolak Supervisor. Catatan: " + dokumen.getCatatanValidasi();
        } else if (draftBaru && supervisor) {
            pesan = "Draf ini biasanya diisi oleh Staff di akhir shift. Sebagai Supervisor, Anda dapat meninjau nilai Stok Sistem.";
        }
        infoBox.setVisible(pesan != null);
        infoBox.setManaged(pesan != null);
        infoLabel.setText(pesan == null ? "" : pesan);
    }

    private void setupTable(TableColumn<DetailStockOpname, String> nama,
                            TableColumn<DetailStockOpname, String> satuan,
                            TableColumn<DetailStockOpname, String> sistem,
                            TableColumn<DetailStockOpname, String> fisik,
                            TableColumn<DetailStockOpname, DetailStockOpname> selisih,
                            TableView<DetailStockOpname> table,
                            ObservableList<DetailStockOpname> backing) {
        nama.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNamaBarang()));
        satuan.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getSatuanBarang() == null ? "-" : c.getValue().getSatuanBarang().getLabel()));
        sistem.setCellValueFactory(c -> new SimpleStringProperty(Formats.qty(c.getValue().getStokSistem())));

        fisik.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getStokFisik() == null ? "" : Formats.qty(c.getValue().getStokFisik())));
        fisik.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        fisik.setOnEditCommit(evt -> {
            DetailStockOpname detail = evt.getRowValue();
            try {
                BigDecimal nilai = Formats.parseQty(evt.getNewValue(), "Stok Fisik");
                if (nilai.signum() < 0) {
                    throw new BusinessException("Stok Fisik tidak boleh negatif.");
                }
                detail.setStokFisik(nilai);
                detail.hitungSelisih();
            } catch (BusinessException e) {
                AlertUtil.error("Input Tidak Valid", e.getMessage());
            }
            table.refresh();
            perbaruiTotalSelisih();
        });

        selisih.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        selisih.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(DetailStockOpname item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.getStokFisik() == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                BigDecimal s = item.getSelisih();
                String label = (s.signum() > 0 ? "+" : "") + Formats.qtyWithSatuan(s, item.getSatuanBarang());
                String warna = s.signum() == 0 ? "chip-green" : (s.signum() > 0 ? "chip-blue" : "chip-red");
                setGraphic(Chips.of(label, warna));
                setText(null);
            }
        });

        table.setItems(backing);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Belum ada barang untuk diaudit."));
    }

    private void terapkanFilter() {
        String kunci = cariField.getText() == null ? "" : cariField.getText().trim().toLowerCase();
        bahanTable.setItems(FXCollections.observableArrayList(bahanItems.stream()
                .filter(d -> d.getNamaBarang().toLowerCase().contains(kunci)).toList()));
        produkTable.setItems(FXCollections.observableArrayList(produkItems.stream()
                .filter(d -> d.getNamaBarang().toLowerCase().contains(kunci)).toList()));
    }

    private void perbaruiTotalSelisih() {
        BigDecimal total = dokumen.getDetail().stream()
                .filter(d -> d.getStokFisik() != null)
                .map(DetailStockOpname::getSelisih)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long terisi = dokumen.getDetail().stream().filter(d -> d.getStokFisik() != null).count();
        totalSelisihLabel.setText("Terisi: " + terisi + "/" + dokumen.getDetail().size()
                + "   |   Total Selisih: " + Formats.qty(total));
    }

    @FXML
    private void onSimpanDraft() {
        try {
            opnameService.simpanDraft(dokumen);
            AlertUtil.info("Draft Tersimpan", "Draft Stock Opname berhasil disimpan.");
            tampilkanDokumen();
        } catch (BusinessException e) {
            AlertUtil.error("Gagal", e.getMessage());
        }
    }

    @FXML
    private void onKirimAjuan() {
        boolean yakin = AlertUtil.confirm("Kirim Ajuan Audit",
                "Kirim ajuan Stock Opname ke Supervisor? Angka akan terkunci hingga divalidasi.");
        if (!yakin) {
            return;
        }
        try {
            opnameService.kirimAjuan(dokumen);
            AlertUtil.info("Ajuan Terkirim", "Ajuan Stock Opname dikirim dengan status \"Menunggu Validasi\".");
            tampilkanDokumen();
        } catch (BusinessException e) {
            AlertUtil.error("Gagal Mengirim", e.getMessage());
        }
    }

    @FXML
    private void onSetujui() {
        boolean yakin = AlertUtil.confirm("Setujui Stock Opname",
                "Setujui ajuan ini? Sistem akan membuat mutasi penyesuaian dan mengunci shift secara permanen.");
        if (!yakin) {
            return;
        }
        try {
            opnameService.setujui(dokumen.getId());
            AlertUtil.info("Tervalidasi",
                    "Stock Opname disetujui. Mutasi penyesuaian dibuat dan shift dikunci. "
                            + "Stok akhir menjadi stok awal shift berikutnya.");
            onMuat();
        } catch (BusinessException e) {
            AlertUtil.error("Gagal", e.getMessage());
        }
    }

    @FXML
    private void onTolak() {
        FXMLLoader loader = FxUtil.loader("reason_dialog.fxml");
        ReasonDialogController controller = loader.getController();
        controller.init("Tolak Stock Opname",
                "Tuliskan instruksi hitung ulang untuk Staff. Status akan kembali menjadi \"Draf Revisi\".",
                "Kirim Penolakan");
        Stage dialog = new Stage();
        dialog.setTitle("Tolak Stock Opname - Hako Tjeria");
        dialog.initModality(Modality.APPLICATION_MODAL);
        Scene scene = new Scene(loader.getRoot());
        FxUtil.applyStylesheet(scene);
        dialog.setScene(scene);
        FxUtil.showAndWait(dialog);

        String catatan = controller.getHasil();
        if (catatan == null) {
            return;
        }
        try {
            opnameService.tolak(dokumen.getId(), catatan);
            AlertUtil.info("Ditolak", "Ajuan dikembalikan sebagai Draf Revisi. Hak input Staff dibuka kembali.");
            onMuat();
        } catch (BusinessException e) {
            AlertUtil.error("Gagal", e.getMessage());
        }
    }
}
