package com.hakotjeria.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import com.hakotjeria.MainApp;
import com.hakotjeria.service.UserService;
import com.hakotjeria.util.AlertUtil;
import com.hakotjeria.util.FxUtil;
import com.hakotjeria.util.Icons;
import com.hakotjeria.util.Navigation;
import com.hakotjeria.util.Session;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * Kerangka utama aplikasi: sidebar navigasi sesuai hak akses (ER03, R02.6)
 * dan area konten halaman.
 */
public class MainController {

    @FXML
    private StackPane logoBox;
    @FXML
    private VBox navBox;
    @FXML
    private VBox logoutBox;
    @FXML
    private StackPane contentArea;

    private final UserService userService = new UserService();
    private final ToggleGroup navGroup = new ToggleGroup();
    private final Map<String, ToggleButton> navButtons = new LinkedHashMap<>();

    private record MenuItem(String key, String label, String icon, boolean supervisorOnly) {
    }

    private static final MenuItem[] MENU = {
            new MenuItem("dashboard", "Dashboard", Icons.DASHBOARD, false),
            new MenuItem("master_data", "Master Data", Icons.DATABASE, false),
            new MenuItem("bom", "Formulasi BOM", Icons.BREAD, false),
            new MenuItem("jadwal_tugas", "Jadwal Tugas Operasional", Icons.CALENDAR, false),
            new MenuItem("riwayat_mutasi", "Riwayat Mutasi Stok", Icons.SWAP, false),
            new MenuItem("stock_opname", "Stock Opname", Icons.BOX, false),
            new MenuItem("riwayat_opname", "Riwayat Stock Opname", Icons.HISTORY, false),
            new MenuItem("manajemen_pengguna", "Manajemen Pengguna", Icons.USERS, true)
    };

    @FXML
    private void initialize() {
        logoBox.getChildren().add(Icons.of(Icons.BREAD, 22, Color.web("#9DB2F0")));
        buildMenu();
        Navigation.setHandler(this::navigate);
        navigate("dashboard");
    }

    private void buildMenu() {
        for (MenuItem item : MENU) {
            if (item.supervisorOnly() && !Session.isSupervisor()) {
                continue; // Menu Manajemen Pengguna khusus Supervisor (ER03).
            }
            ToggleButton btn = new ToggleButton(item.label());
            btn.getStyleClass().add("nav-item");
            btn.setGraphic(Icons.nav(item.icon(), 15));
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setToggleGroup(navGroup);
            btn.setOnAction(e -> {
                btn.setSelected(true); // navigasi tidak boleh membuat semua toggle kosong
                navigate(item.key());
            });
            navButtons.put(item.key(), btn);
            navBox.getChildren().add(btn);
        }

        Button logoutBtn = new Button("Logout");
        logoutBtn.getStyleClass().add("nav-logout");
        logoutBtn.setGraphic(Icons.nav(Icons.LOGOUT, 15));
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setOnAction(e -> onLogout());
        logoutBox.getChildren().add(logoutBtn);
    }

    /** Memuat ulang halaman tujuan agar data selalu segar (SR11 < 3 detik). */
    private void navigate(String pageKey) {
        ToggleButton btn = navButtons.get(pageKey);
        if (btn != null) {
            btn.setSelected(true);
        }
        Parent page = FxUtil.load(pageKey + ".fxml");
        contentArea.getChildren().setAll(page);
    }

    /** Konfirmasi logout, akhiri sesi, kembali ke halaman Login (R13.1 - R13.3). */
    private void onLogout() {
        boolean yakin = AlertUtil.confirm("Konfirmasi Logout", "Apakah Anda yakin ingin keluar dari sistem Hako Tjeria?");
        if (!yakin) {
            return;
        }
        userService.logout();
        Parent root = FxUtil.load("login.fxml");
        Stage stage = (Stage) contentArea.getScene().getWindow();
        Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
        FxUtil.applyStylesheet(scene);
        stage.setScene(scene);
        stage.setTitle(MainApp.APP_TITLE);
        AlertUtil.info("Logout Berhasil", "Anda telah keluar dari sistem Hako Tjeria.");
    }
}
