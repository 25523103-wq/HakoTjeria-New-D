package com.hakotjeria.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.hakotjeria.model.JadwalTugas;
import com.hakotjeria.model.JenisInventaris;
import com.hakotjeria.model.KategoriTugas;
import com.hakotjeria.model.ProdukJadi;
import com.hakotjeria.model.Shift;
import com.hakotjeria.model.SumberProduk;
import com.hakotjeria.model.User;
import com.hakotjeria.repository.ShiftRepository;
import com.hakotjeria.service.JadwalTugasService;
import com.hakotjeria.service.MasterDataService;
import com.hakotjeria.service.MutasiStokService;
import com.hakotjeria.service.ProduksiService;
import com.hakotjeria.service.ProduksiService.KebutuhanBahan;
import com.hakotjeria.service.UserService;
import com.hakotjeria.util.AlertUtil;
import com.hakotjeria.util.BusinessException;
import com.hakotjeria.util.Chips;
import com.hakotjeria.util.Formats;
import com.hakotjeria.util.Session;

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
import javafx.stage.Stage;

/**
 * Dialog tambah/ubah tugas operasional (UC-05).
 * Daftar produk menyesuaikan Kategori Tugas agar selaras dengan
 * Sumber Produk (R05.3).
 */
public class TugasFormDialogController {

    @FXML
    private Label judulLabel;
    @FXML
    private DatePicker tanggalPicker;
    @FXML
    private ComboBox<Shift> shiftCombo;
    @FXML
    private ComboBox<KategoriTugas> kategoriCombo;
    @FXML
    private ComboBox<ProdukJadi> produkCombo;
    @FXML
    private TextField qtyTargetField;
    @FXML
    private ComboBox<User> staffCombo;
    @FXML
    private TextField catatanField;
    @FXML
    private Button simpanButton;
    @FXML
    private VBox previewBox;
    @FXML
    private Label stokProdukLabel;
    @FXML
    private Label previewJudulLabel;
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

    private final JadwalTugasService tugasService = new JadwalTugasService();
    private final MasterDataService masterService = new MasterDataService();
    private final UserService userService = new UserService();
    private final ShiftRepository shiftRepo = new ShiftRepository();
    private final ProduksiService produksiService = new ProduksiService();
    private final MutasiStokService mutasiService = new MutasiStokService();

    private JadwalTugas existing;
    private boolean tersimpan;

    public void init(JadwalTugas existing, LocalDate tanggalDefault) {
        this.existing = existing;

        shiftCombo.setItems(FXCollections.observableArrayList(shiftRepo.findAll()));
        kategoriCombo.setItems(FXCollections.observableArrayList(KategoriTugas.values()));
        staffCombo.setItems(FXCollections.observableArrayList(userService.daftarStaffAktif()));
        kategoriCombo.valueProperty().addListener((obs, o, n) -> muatProduk(n));
        siapkanPreview();

        if (existing == null) {
            judulLabel.setText("Tambah Tugas Operasional");
            tanggalPicker.setValue(tanggalDefault == null ? LocalDate.now() : tanggalDefault);
            Shift aktif = Session.getActiveShift();
            shiftCombo.getItems().stream()
                    .filter(s -> s.getId() == aktif.getId())
                    .findFirst()
                    .ifPresent(shiftCombo::setValue);
            return;
        }
        judulLabel.setText("Ubah Tugas Operasional");
        tanggalPicker.setValue(existing.getTanggal());
        shiftCombo.getItems().stream()
                .filter(s -> s.getId() == existing.getShiftId())
                .findFirst()
                .ifPresent(shiftCombo::setValue);
        kategoriCombo.setValue(existing.getKategori());
        muatProduk(existing.getKategori());
        produkCombo.getItems().stream()
                .filter(p -> p.getId() == existing.getProdukId())
                .findFirst()
                .ifPresent(produkCombo::setValue);
        qtyTargetField.setText(Formats.qtyEditable(existing.getQtyTarget()));
        if (existing.getStaffId() != null) {
            staffCombo.getItems().stream()
                    .filter(u -> u.getId() == existing.getStaffId())
                    .findFirst()
                    .ifPresent(staffCombo::setValue);
        }
        catatanField.setText(existing.getCatatan());
    }

    /** Produk difilter sesuai kategori: Internal ber-BOM vs Eksternal (R05.3). */
    private void muatProduk(KategoriTugas kategori) {
        produkCombo.setValue(null);
        if (kategori == null) {
            produkCombo.setItems(FXCollections.observableArrayList());
            return;
        }
        SumberProduk sumber = kategori == KategoriTugas.PRODUKSI_INTERNAL
                ? SumberProduk.INTERNAL : SumberProduk.EKSTERNAL;
        List<ProdukJadi> produk = masterService.daftarProdukJadi().stream()
                .filter(p -> p.getSumber() == sumber)
                .toList();
        produkCombo.setItems(FXCollections.observableArrayList(produk));
    }

    /**
     * Pratinjau stok bagi Supervisor sebelum tugas sampai ke Staff:
     * stok produk terkini + tabel kebutuhan bahan (BOM x Qty Target) dengan
     * status Cukup/Kurang, diperbarui dinamis mengikuti isian form.
     */
    private void siapkanPreview() {
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
        previewTable.setPlaceholder(new Label("Isi Qty Target untuk melihat kebutuhan bahan."));

        produkCombo.valueProperty().addListener((obs, o, n) -> perbaruiPreview());
        qtyTargetField.textProperty().addListener((obs, o, n) -> perbaruiPreview());
    }

    private void perbaruiPreview() {
        perbaruiIsiPreview();
        // Window menyesuaikan tinggi otomatis saat pratinjau muncul/hilang,
        // sehingga tabel langsung terlihat penuh tanpa resize manual.
        if (previewBox.getScene() != null && previewBox.getScene().getWindow() != null) {
            previewBox.getScene().getWindow().sizeToScene();
        }
    }

    private void perbaruiIsiPreview() {
        ProdukJadi produk = produkCombo.getValue();
        boolean adaProduk = produk != null;
        previewBox.setVisible(adaProduk);
        previewBox.setManaged(adaProduk);
        if (!adaProduk) {
            return;
        }
        BigDecimal stokProduk = mutasiService.stokBarang(JenisInventaris.PRODUK_JADI, produk.getId());
        stokProdukLabel.setText("Stok \"" + produk.getNama() + "\" saat ini: "
                + Formats.qtyWithSatuan(stokProduk, produk.getSatuan()));

        // Tabel kebutuhan bahan hanya relevan untuk Produksi Internal ber-BOM.
        boolean internal = kategoriCombo.getValue() == KategoriTugas.PRODUKSI_INTERNAL;
        previewJudulLabel.setVisible(internal);
        previewJudulLabel.setManaged(internal);
        previewTable.setVisible(internal);
        previewTable.setManaged(internal);
        if (!internal) {
            kurangBanner.setVisible(false);
            kurangBanner.setManaged(false);
            return;
        }
        BigDecimal qty;
        try {
            qty = new BigDecimal(qtyTargetField.getText().trim().replace(",", "."));
        } catch (Exception e) {
            qty = BigDecimal.ZERO;
        }
        try {
            List<KebutuhanBahan> preview = produksiService.previewKebutuhan(produk.getId(), qty);
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
                kurangLabel.setText("Stok bahan baku belum mencukupi untuk Qty Target ini: "
                        + String.join(", ", kurang)
                        + ". Tugas tetap dapat disimpan; pastikan restok sebelum dieksekusi Staff.");
            }
        } catch (BusinessException e) {
            // Produk belum memiliki BOM: tampilkan pesannya sebagai informasi.
            previewTable.setItems(FXCollections.observableArrayList());
            kurangBanner.setVisible(true);
            kurangBanner.setManaged(true);
            kurangLabel.setText(e.getMessage());
        }
    }

    public boolean isTersimpan() {
        return tersimpan;
    }

    @FXML
    private void onSimpan() {
        try {
            JadwalTugas tugas = existing == null ? new JadwalTugas() : existing;
            tugas.setTanggal(tanggalPicker.getValue());
            tugas.setShiftId(shiftCombo.getValue() == null ? 0 : shiftCombo.getValue().getId());
            tugas.setKategori(kategoriCombo.getValue());
            tugas.setProdukId(produkCombo.getValue() == null ? 0 : produkCombo.getValue().getId());
            tugas.setQtyTarget(Formats.parseQty(qtyTargetField.getText(), "Qty Target"));
            tugas.setStaffId(staffCombo.getValue() == null ? null : staffCombo.getValue().getId());
            tugas.setCatatan(catatanField.getText() == null || catatanField.getText().isBlank()
                    ? null : catatanField.getText().trim());

            if (existing == null) {
                tugasService.buatTugas(tugas);
            } else {
                tugasService.ubahTugas(tugas);
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
