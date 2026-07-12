package com.hakotjeria.controller;

import java.math.BigDecimal;
import java.util.List;

import com.hakotjeria.model.JadwalTugas;
import com.hakotjeria.model.JenisInventaris;
import com.hakotjeria.model.KategoriTugas;
import com.hakotjeria.model.MutasiStok;
import com.hakotjeria.model.Shift;
import com.hakotjeria.model.StokItem;
import com.hakotjeria.service.DashboardService;
import com.hakotjeria.util.Chips;
import com.hakotjeria.util.Formats;
import com.hakotjeria.util.Icons;
import com.hakotjeria.util.Navigation;
import com.hakotjeria.util.Session;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.geometry.Pos;

/** Controller Dashboard sesuai hak akses pengguna (UC-02, ER12, ER13). */
public class DashboardController {

    @FXML
    private Label titleLabel;
    @FXML
    private Label subtitleLabel;
    @FXML
    private StackPane card1Icon;
    @FXML
    private Label card1Title;
    @FXML
    private Label card1Value;
    @FXML
    private Label card1Note;
    @FXML
    private StackPane card2Icon;
    @FXML
    private Label card2Title;
    @FXML
    private Label card2Value;
    @FXML
    private Label card2Note;
    @FXML
    private StackPane card3Icon;
    @FXML
    private Label card3Title;
    @FXML
    private Label card3Value;
    @FXML
    private Label card3Note;
    @FXML
    private StackPane card4Icon;
    @FXML
    private Label card4Title;
    @FXML
    private Label card4Value;
    @FXML
    private Label card4Note;
    @FXML
    private HBox quickAccessBox;
    @FXML
    private Label lihatSemuaLink;
    @FXML
    private TableView<JadwalTugas> tugasTable;
    @FXML
    private TableColumn<JadwalTugas, String> colTugasProduk;
    @FXML
    private TableColumn<JadwalTugas, String> colTugasKategori;
    @FXML
    private TableColumn<JadwalTugas, String> colTugasShift;
    @FXML
    private TableColumn<JadwalTugas, String> colTugasTarget;
    @FXML
    private TableColumn<JadwalTugas, String> colTugasAktual;
    @FXML
    private TableColumn<JadwalTugas, JadwalTugas> colTugasStatus;
    @FXML
    private VBox chartCard;
    @FXML
    private BarChart<String, Number> produksiChart;
    @FXML
    private VBox stokBahanBox;
    @FXML
    private VBox stokProdukBox;
    @FXML
    private VBox aktivitasCard;
    @FXML
    private VBox aktivitasBox;

    private final DashboardService dashboardService = new DashboardService();

    @FXML
    private void initialize() {
        setupTable();
        List<JadwalTugas> tugasHariIni = dashboardService.tugasHariIni();
        tugasTable.setItems(FXCollections.observableArrayList(tugasHariIni));
        lihatSemuaLink.setOnMouseClicked(e -> Navigation.goTo("jadwal_tugas"));

        if (Session.isSupervisor()) {
            initSupervisor(tugasHariIni);
        } else {
            initStaff(tugasHariIni);
        }
        isiKesehatanStok(stokBahanBox, dashboardService.stokSemua(JenisInventaris.BAHAN_BAKU));
        isiKesehatanStok(stokProdukBox, dashboardService.stokSemua(JenisInventaris.PRODUK_JADI));
    }

    // ===================== SUPERVISOR (ER12) =====================

    private void initSupervisor(List<JadwalTugas> tugasHariIni) {
        subtitleLabel.setText("Ringkasan aktivitas dan status inventaris hari ini.");

        long selesai = dashboardService.hitungTugasSelesai(tugasHariIni);
        card1Icon.getChildren().add(Icons.of(Icons.CALENDAR, 18, Color.web("#175CD3")));
        card1Title.setText("Tugas Operasional Hari Ini");
        card1Value.setText(selesai + " / " + tugasHariIni.size());
        card1Note.setText("tugas berstatus final");

        int varianBahan = dashboardService.stokSemua(JenisInventaris.BAHAN_BAKU).size();
        int varianProduk = dashboardService.stokSemua(JenisInventaris.PRODUK_JADI).size();
        card2Icon.getChildren().add(Icons.of(Icons.DATABASE, 18, Color.web("#067647")));
        card2Title.setText("Varian Bahan Baku / Produk Jadi");
        card2Value.setText(varianBahan + " / " + varianProduk);
        card2Note.setText("terdaftar pada master data");

        int rendahBahan = dashboardService.stokDiBawahBatasAman(JenisInventaris.BAHAN_BAKU).size();
        int rendahProduk = dashboardService.stokDiBawahBatasAman(JenisInventaris.PRODUK_JADI).size();
        card3Icon.getChildren().add(Icons.of(Icons.WARNING, 18, Color.web("#B54708")));
        card3Title.setText("Peringatan Stok Rendah");
        card3Value.setText(String.valueOf(rendahBahan + rendahProduk));
        card3Note.setText("Bahan: " + rendahBahan + " | Produk: " + rendahProduk);

        Shift shift = Session.getActiveShift();
        card4Icon.getChildren().add(Icons.of(Icons.BOX, 18, Color.web("#9DB2F0")));
        card4Title.setText("Stock Opname Hari Ini (" + shift.getNamaShift() + ")");
        card4Value.setText(dashboardService.statusOpnameHariIni(shift.getId()));
        card4Note.setText(shift.getJamMulai() + " - " + shift.getJamSelesai());

        isiChart();
        isiAktivitas();
    }

    private void isiChart() {
        List<JadwalTugas> produksi = dashboardService.tugasProduksiHariIni();
        XYChart.Series<String, Number> target = new XYChart.Series<>();
        target.setName("Qty Target");
        XYChart.Series<String, Number> aktual = new XYChart.Series<>();
        aktual.setName("Qty Aktual");
        for (JadwalTugas t : produksi) {
            target.getData().add(new XYChart.Data<>(t.getNamaProduk(), t.getQtyTarget()));
            BigDecimal nilaiAktual = t.getQtyAktual() == null ? BigDecimal.ZERO : t.getQtyAktual();
            aktual.getData().add(new XYChart.Data<>(t.getNamaProduk(), nilaiAktual));
        }
        produksiChart.getData().add(target);
        produksiChart.getData().add(aktual);
        if (produksi.isEmpty()) {
            chartCard.getChildren().add(new Label("Belum ada tugas Produksi Internal hari ini."));
        }
    }

    private void isiAktivitas() {
        List<MutasiStok> terbaru = dashboardService.mutasiTerbaru(7);
        if (terbaru.isEmpty()) {
            aktivitasBox.getChildren().add(new Label("Belum ada aktivitas mutasi."));
            return;
        }
        for (MutasiStok m : terbaru) {
            VBox teks = new VBox(2);
            Label nama = new Label(m.getNamaBarang() + "  ·  " + Formats.qtyWithSatuan(m.getQty(), m.getSatuanBarang()));
            nama.getStyleClass().add("label-strong");
            Label ket = new Label(m.getKeterangan() + " — " + Formats.tanggal(m.getTanggal()) + " · " + m.getNamaShift());
            ket.getStyleClass().add("label-muted");
            teks.getChildren().addAll(nama, ket);
            HBox row = new HBox(10, Chips.jenisMutasi(m.getJenis()), teks);
            row.setAlignment(Pos.CENTER_LEFT);
            aktivitasBox.getChildren().add(row);
        }
    }

    // ===================== STAFF (ER13) =====================

    private void initStaff(List<JadwalTugas> tugasHariIni) {
        subtitleLabel.setText("Jadwal tugas dan status operasional Anda hari ini.");
        chartCard.setVisible(false);
        chartCard.setManaged(false);
        aktivitasCard.setVisible(false);
        aktivitasCard.setManaged(false);

        long selesai = dashboardService.hitungTugasSelesai(tugasHariIni);
        card1Icon.getChildren().add(Icons.of(Icons.CALENDAR, 18, Color.web("#175CD3")));
        card1Title.setText("Tugas Operasional Hari Ini");
        card1Value.setText(selesai + " / " + tugasHariIni.size());
        card1Note.setText("tugas berstatus final");

        Shift shift = Session.getActiveShift();
        card2Icon.getChildren().add(Icons.of(Icons.HISTORY, 18, Color.web("#067647")));
        card2Title.setText("Shift Aktif");
        card2Value.setText(shift.getNamaShift());
        card2Note.setText(shift.getJamMulai() + " - " + shift.getJamSelesai());

        card3Icon.getChildren().add(Icons.of(Icons.BOX, 18, Color.web("#B54708")));
        card3Title.setText("Stock Opname Hari Ini");
        card3Value.setText(dashboardService.statusOpnameHariIni(shift.getId()));
        card3Note.setText(shift.getNamaShift());

        long produksi = tugasHariIni.stream()
                .filter(t -> t.getKategori() == KategoriTugas.PRODUKSI_INTERNAL).count();
        card4Icon.getChildren().add(Icons.of(Icons.BREAD, 18, Color.web("#9DB2F0")));
        card4Title.setText("Produksi / Pengambilan");
        card4Value.setText(produksi + " / " + (tugasHariIni.size() - produksi));
        card4Note.setText("pembagian tugas hari ini");

        quickAccessBox.setVisible(true);
        quickAccessBox.setManaged(true);
        quickAccessBox.getChildren().addAll(
                quickButton("Kerjakan Jadwal Tugas", "jadwal_tugas"),
                quickButton("Input Stock Opname", "stock_opname"),
                quickButton("Catat Mutasi Manual", "riwayat_mutasi"),
                quickButton("Lihat Master Data", "master_data"));
    }

    private Button quickButton(String label, String pageKey) {
        Button btn = new Button(label);
        btn.getStyleClass().add("btn-secondary");
        btn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btn, Priority.ALWAYS);
        btn.setOnAction(e -> Navigation.goTo(pageKey));
        return btn;
    }

    // ===================== KOMPONEN BERSAMA =====================

    private void setupTable() {
        colTugasProduk.setCellValueFactory(new PropertyValueFactory<>("namaProduk"));
        colTugasKategori.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getKategori().getLabel()));
        colTugasShift.setCellValueFactory(new PropertyValueFactory<>("namaShift"));
        colTugasTarget.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                Formats.qtyWithSatuan(c.getValue().getQtyTarget(), c.getValue().getSatuanProduk())));
        colTugasAktual.setCellValueFactory(c -> {
            JadwalTugas t = c.getValue();
            BigDecimal nilai = t.getKategori() == KategoriTugas.PRODUKSI_INTERNAL
                    ? t.getQtyAktual() : t.getQtyDiterima();
            return new javafx.beans.property.SimpleStringProperty(
                    nilai == null ? "-" : Formats.qtyWithSatuan(nilai, t.getSatuanProduk()));
        });
        colTugasStatus.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue()));
        colTugasStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(JadwalTugas item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || item == null ? null : Chips.statusTugas(item.getStatus()));
                setText(null);
            }
        });
        tugasTable.setPlaceholder(new Label("Belum ada tugas operasional untuk hari ini."));
        tugasTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    /** Panel "Kesehatan Stok" dengan indikator Aman/Waspada/Rendah (R02.5). */
    private void isiKesehatanStok(VBox container, List<StokItem> items) {
        List<StokItem> urut = items.stream()
                .sorted((a, b) -> Integer.compare(peringkat(a), peringkat(b)))
                .limit(5)
                .toList();
        if (urut.isEmpty()) {
            container.getChildren().add(new Label("Belum ada data barang."));
            return;
        }
        for (StokItem item : urut) {
            Label nama = new Label(item.getNama());
            nama.getStyleClass().add("label-strong");
            Label sisa = new Label("Sisa: " + Formats.qtyWithSatuan(item.getStok(), item.getSatuan())
                    + "  ·  Batas aman: " + Formats.qtyWithSatuan(item.getBatasMin(), item.getSatuan()));
            sisa.getStyleClass().add("label-muted");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox headerRow = new HBox(8, nama, spacer, chipStok(item));
            headerRow.setAlignment(Pos.CENTER_LEFT);

            ProgressBar bar = new ProgressBar(hitungRasio(item));
            bar.setMaxWidth(Double.MAX_VALUE);
            if (item.isDiBawahBatasAman()) {
                bar.getStyleClass().add("progress-danger");
            } else if (peringkat(item) == 1) {
                bar.getStyleClass().add("progress-warning");
            }
            container.getChildren().add(new VBox(4, headerRow, sisa, bar));
        }
    }

    private Label chipStok(StokItem item) {
        return switch (peringkat(item)) {
            case 0 -> Chips.of("Rendah", "chip-red");
            case 1 -> Chips.of("Waspada", "chip-yellow");
            default -> Chips.of("Aman", "chip-green");
        };
    }

    /** 0 = di bawah batas aman, 1 = mendekati batas (<= 150%), 2 = aman. */
    private int peringkat(StokItem item) {
        if (item.isDiBawahBatasAman()) {
            return 0;
        }
        BigDecimal batas = item.getBatasMin();
        if (batas.signum() > 0 && item.getStok().compareTo(batas.multiply(new BigDecimal("1.5"))) <= 0) {
            return 1;
        }
        return 2;
    }

    private double hitungRasio(StokItem item) {
        BigDecimal batas = item.getBatasMin();
        if (batas.signum() <= 0) {
            return item.getStok().signum() > 0 ? 1.0 : 0.0;
        }
        double rasio = item.getStok().doubleValue() / (batas.doubleValue() * 2);
        return Math.max(0.02, Math.min(1.0, rasio));
    }
}
