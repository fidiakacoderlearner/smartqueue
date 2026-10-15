package com.smartqueue.controller;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.Query;
import com.smartqueue.SmartQueueApp;
import com.smartqueue.model.SlotKelompok;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MahasiswaRegisterController {

    @FXML private Label lblStatus;
    @FXML private ComboBox<String> comboSlot;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private ScrollPane scrollPaneAnggota;
    @FXML private VBox vboxAnggotaContainer;
    @FXML private Button btnTambahAnggota;
    @FXML private Button btnDaftar;

    private final FirebaseManager manager = FirebaseManager.getInstance();
    private final ObservableList<String> slotTersedia = FXCollections.observableArrayList();

    private ValueEventListener slotListener;
    private Query slotQuery;

    private final List<HBox> barisAnggota = new ArrayList<>();

    @FXML
    public void initialize() {
        lblStatus.setText("");
        comboSlot.setItems(slotTersedia);
        loadSlotTersedia(); // Mengaktifkan listener

        tambahBarisAnggota(true);
    }

    private void detachSlotListener() {
        if (slotListener != null && slotQuery != null) {
            slotQuery.removeEventListener(slotListener);
            slotListener = null;
            slotQuery = null;
        }
    }

    private void loadSlotTersedia() {
        detachSlotListener();

        if (lblStatus.getText().isEmpty() || !lblStatus.getStyle().contains("-fx-text-fill: red;")) {
            lblStatus.setText("Memuat slot yang tersedia...");
        }

        slotQuery = manager.getSlotRef().orderByChild("isRegistered").equalTo(false);

        slotListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Platform.runLater(() -> {

                    // Simpan apa yang sedang dipilih pengguna
                    String selected = comboSlot.getValue();

                    // Buat daftar baru berdasarkan data live
                    List<String> newList = new ArrayList<>();
                    if (snapshot.exists()) {
                        for (DataSnapshot slot : snapshot.getChildren()) {
                            newList.add(slot.getKey());
                        }
                    }

                    // Perbarui ObservableList (ini akan update ComboBox)
                    slotTersedia.setAll(newList);

                    if (newList.isEmpty()) {
                        comboSlot.setPromptText("Tidak ada slot tersedia");
                        lblStatus.setText("Tidak ada slot pendaftaran yang tersedia saat ini.");
                    } else {
                        comboSlot.setPromptText("Pilih slot kelompok");
                        // Hapus status 'memuat...' jika bukan error
                        if (!lblStatus.getStyle().contains("-fx-text-fill: red;")) {
                            lblStatus.setText("");
                        }
                    }

                    // === INI ADALAH LOGIKA BARU YANG PENTING ===
                    if (selected != null) {
                        // Cek apakah slot yang tadi dipilih MASIH ada di daftar baru
                        if (newList.contains(selected)) {
                            // Slot masih valid, pilih kembali
                            comboSlot.setValue(selected);
                        } else {
                            // Slot sudah tidak valid (diambil orang)!
                            // Hapus pilihan dan beri tahu pengguna.
                            comboSlot.setValue(null);
                            showError("Slot '" + selected + "' baru saja diambil orang lain. Halaman diperbarui.");
                        }
                    }
                    // === AKHIR LOGIKA BARU ===
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Platform.runLater(() -> {
                    showError("Gagal memuat slot: " + error.getMessage());
                });
            }
        };

        slotQuery.addValueEventListener(slotListener);
    }

    @FXML
    protected void onTambahAnggota() {
        tambahBarisAnggota(false);
    }

    private void tambahBarisAnggota(boolean isFirstRow) {
        TextField txtNIM = new TextField();
        txtNIM.setPromptText("NIM Anggota " + (barisAnggota.size() + 1));
        HBox.setHgrow(txtNIM, Priority.SOMETIMES);

        TextField txtNama = new TextField();
        txtNama.setPromptText("Nama Anggota " + (barisAnggota.size() + 1));
        HBox.setHgrow(txtNama, Priority.SOMETIMES);

        HBox barisBaru = new HBox(5, txtNIM, txtNama);
        barisBaru.setAlignment(Pos.CENTER_LEFT);

        if (!isFirstRow) {
            Button btnHapus = new Button("X");
            btnHapus.getStyleClass().add("danger");
            btnHapus.setOnAction(event -> hapusBarisAnggota(barisBaru));
            barisBaru.getChildren().add(btnHapus);
        }

        barisAnggota.add(barisBaru);
        vboxAnggotaContainer.getChildren().add(barisBaru);
    }

    private void hapusBarisAnggota(HBox baris) {
        barisAnggota.remove(baris);
        vboxAnggotaContainer.getChildren().remove(baris);
    }

    @FXML
    protected void onDaftar() {
        // 1. Validasi Input UI
        String slot = comboSlot.getValue();
        if (slot == null) {
            showError("Silakan pilih slot kelompok Anda.");
            return;
        }

        String password = txtPassword.getText();
        String confirmPassword = txtConfirmPassword.getText();

        if (password.length() < 6) {
            showError("Password harus minimal 6 karakter.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            showError("Password dan Konfirmasi Password tidak cocok.");
            return;
        }

        // 2. Kumpulkan Data Anggota
        List<String> nims = new ArrayList<>();
        List<String> namas = new ArrayList<>();

        for (HBox baris : barisAnggota) {
            TextField txtNIM = (TextField) baris.getChildren().get(0);
            TextField txtNama = (TextField) baris.getChildren().get(1);

            String nim = txtNIM.getText().trim();
            String nama = txtNama.getText().trim();

            if (nim.isEmpty() || nama.isEmpty()) {
                showError("Semua field NIM dan Nama anggota harus diisi.");
                return;
            }
            if (!nim.matches("\\d+")) {
                showError("NIM harus berupa angka. Kesalahan pada: " + nim);
                return;
            }
            nims.add(nim);
            namas.add(nama);
        }

        if (nims.isEmpty()) {
            showError("Anda harus mendaftarkan minimal satu anggota.");
            return;
        }

        // 3. Kirim ke FirebaseManager
        setLoading(true);
        detachSlotListener(); // Hentikan listener saat memproses

        manager.registrasiMahasiswa(slot, password, nims, namas, (success, message) -> {
            Platform.runLater(() -> {
                if (success) {
                    showInfo("Registrasi Berhasil! Silakan kembali ke halaman login.");
                    btnDaftar.setDisable(true);
                    btnTambahAnggota.setDisable(true);
                } else {
                    setLoading(false);
                    loadSlotTersedia(); // Nyalakan kembali listener
                    showError(message);
                }
            });
        });
    }

    @FXML
    protected void onKembali(ActionEvent event) throws IOException {
        detachSlotListener();

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        SmartQueueApp.loadScene(stage, "mahasiswa-login.fxml", "SmartQueue - Login Mahasiswa");
    }


    private void setLoading(boolean isLoading) {
        btnDaftar.setDisable(isLoading);
        // Teks "Memproses..." dihapus
    }


    private void showInfo(String message) {
        lblStatus.setText(message);
        lblStatus.setStyle("-fx-text-fill: green;");
    }
    private void showError(String message) {
        lblStatus.setText(message);
        lblStatus.setStyle("-fx-text-fill: red;");
    }
}