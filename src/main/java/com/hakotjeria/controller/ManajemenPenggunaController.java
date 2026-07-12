package com.hakotjeria.controller;

import java.util.List;

import com.hakotjeria.model.Role;
import com.hakotjeria.model.User;
import com.hakotjeria.service.UserService;
import com.hakotjeria.util.AlertUtil;
import com.hakotjeria.util.BusinessException;
import com.hakotjeria.util.Chips;
import com.hakotjeria.util.Icons;
import com.hakotjeria.util.Session;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Controller Manajemen Pengguna (UC-12). Menampilkan formulir dan daftar
 * akun secara bersamaan (R12.1). Khusus Supervisor.
 */
public class ManajemenPenggunaController {

    @FXML
    private Button tambahBaruButton;
    @FXML
    private TextField cariField;
    @FXML
    private TableView<User> userTable;
    @FXML
    private TableColumn<User, String> colNama;
    @FXML
    private TableColumn<User, String> colUsername;
    @FXML
    private TableColumn<User, String> colRole;
    @FXML
    private TableColumn<User, User> colStatus;
    @FXML
    private TableColumn<User, User> colAksi;
    @FXML
    private StackPane detailIcon;
    @FXML
    private Label detailNamaLabel;
    @FXML
    private TextField namaField;
    @FXML
    private TextField usernameField;
    @FXML
    private VBox passwordBox;
    @FXML
    private PasswordField passwordField;
    @FXML
    private ComboBox<Role> roleCombo;
    @FXML
    private VBox statusBox;
    @FXML
    private RadioButton aktifRadio;
    @FXML
    private RadioButton nonaktifRadio;
    @FXML
    private Button simpanButton;
    @FXML
    private Button bersihkanButton;
    @FXML
    private Button hapusButton;
    @FXML
    private Button resetPasswordButton;

    private final UserService userService = new UserService();
    private User editing;

    @FXML
    private void initialize() {
        detailIcon.getChildren().add(Icons.of(Icons.PERSON, 20, Color.web("#175CD3")));
        roleCombo.setItems(FXCollections.observableArrayList(Role.values()));

        colNama.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNamaLengkap()));
        colUsername.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUsername()));
        colRole.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRole().getLabel()));
        colRole.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) {
                    setGraphic(null);
                } else {
                    setGraphic(Chips.of(role, role.equals(Role.SUPERVISOR.getLabel()) ? "chip-blue" : "chip-green"));
                }
            }
        });
        colStatus.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setGraphic(null);
                } else {
                    setGraphic(user.isStatusAktif()
                            ? Chips.of("Aktif", "chip-green") : Chips.of("Nonaktif", "chip-red"));
                }
            }
        });
        colAksi.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colAksi.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setGraphic(null);
                    return;
                }
                Button edit = new Button();
                edit.getStyleClass().add("btn-icon");
                edit.setGraphic(Icons.of(Icons.EDIT, 14, Color.web("#475467")));
                edit.setOnAction(e -> pilihUntukEdit(user));
                setGraphic(edit);
            }
        });
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        userTable.setPlaceholder(new Label("Belum ada pengguna."));
        userTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) {
                pilihUntukEdit(n);
            }
        });
        cariField.textProperty().addListener((obs, o, n) -> muatData());

        muatData();
        onBersihkan();
    }

    private void muatData() {
        List<User> semua = userService.daftarPengguna();
        String kunci = cariField.getText() == null ? "" : cariField.getText().trim().toLowerCase();
        List<User> hasil = semua.stream()
                .filter(u -> kunci.isEmpty()
                        || u.getNamaLengkap().toLowerCase().contains(kunci)
                        || u.getRole().getLabel().toLowerCase().contains(kunci)
                        || u.getUsername().toLowerCase().contains(kunci))
                .toList();
        userTable.setItems(FXCollections.observableArrayList(hasil));
    }

    private void pilihUntukEdit(User user) {
        editing = user;
        detailNamaLabel.setText(user.getNamaLengkap());
        namaField.setText(user.getNamaLengkap());
        usernameField.setText(user.getUsername());
        usernameField.setDisable(true); // username tidak diubah setelah dibuat
        roleCombo.setValue(user.getRole());
        aktifRadio.setSelected(user.isStatusAktif());
        nonaktifRadio.setSelected(!user.isStatusAktif());
        passwordBox.setVisible(false);
        passwordBox.setManaged(false);
        statusBox.setVisible(true);
        statusBox.setManaged(true);
        hapusButton.setDisable(false);
        resetPasswordButton.setDisable(false);
        simpanButton.setText("Simpan Perubahan");
    }

    @FXML
    private void onTambahBaru() {
        onBersihkan();
    }

    @FXML
    private void onBersihkan() {
        editing = null;
        userTable.getSelectionModel().clearSelection();
        detailNamaLabel.setText("Formulir Pengguna Baru");
        namaField.clear();
        usernameField.clear();
        usernameField.setDisable(false);
        passwordField.clear();
        roleCombo.setValue(Role.STAFF);
        aktifRadio.setSelected(true);
        passwordBox.setVisible(true);
        passwordBox.setManaged(true);
        statusBox.setVisible(false);
        statusBox.setManaged(false);
        hapusButton.setDisable(true);
        resetPasswordButton.setDisable(true);
        simpanButton.setText("Simpan Pengguna Baru");
    }

    @FXML
    private void onSimpan() {
        try {
            if (editing == null) {
                userService.buatPengguna(namaField.getText(), usernameField.getText(),
                        passwordField.getText(), roleCombo.getValue());
                AlertUtil.info("Berhasil", "Pengguna baru berhasil ditambahkan.");
            } else {
                editing.setNamaLengkap(namaField.getText() == null ? "" : namaField.getText().trim());
                editing.setRole(roleCombo.getValue());
                editing.setStatusAktif(aktifRadio.isSelected());
                userService.perbaruiPengguna(editing);
                AlertUtil.info("Berhasil", "Data pengguna berhasil diperbarui.");
            }
            muatData();
            onBersihkan();
        } catch (BusinessException e) {
            AlertUtil.error("Validasi Gagal", e.getMessage());
        }
    }

    @FXML
    private void onHapus() {
        if (editing == null) {
            return;
        }
        boolean yakin = AlertUtil.confirm("Hapus Akun",
                "Nonaktifkan akun \"" + editing.getNamaLengkap() + "\"? Identitas tetap tersimpan pada "
                        + "riwayat mutasi historis, namun akun tidak dapat login lagi.");
        if (!yakin) {
            return;
        }
        try {
            userService.hapusPengguna(editing.getId());
            AlertUtil.info("Berhasil", "Akun telah dinonaktifkan.");
            muatData();
            onBersihkan();
        } catch (BusinessException e) {
            AlertUtil.error("Tidak Dapat Menghapus", e.getMessage());
        }
    }

    @FXML
    private void onResetPassword() {
        if (editing == null) {
            return;
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Reset Password");
        dialog.setHeaderText("Reset password untuk " + editing.getNamaLengkap());
        dialog.setContentText("Password baru (min. 6 karakter):");
        dialog.showAndWait().ifPresent(pw -> {
            try {
                userService.resetPassword(editing.getId(), pw);
                AlertUtil.info("Berhasil",
                        "Password direset. Pengguna wajib mengganti sandi saat login berikutnya.");
            } catch (BusinessException e) {
                AlertUtil.error("Gagal", e.getMessage());
            }
        });
    }
}
