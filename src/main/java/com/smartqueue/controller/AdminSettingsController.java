package com.smartqueue.controller;

import com.smartqueue.SmartQueueApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

public class AdminSettingsController {

    @FXML private TextArea txtInputInfo;
    @FXML private TextField txtNewUsername;
    @FXML private PasswordField txtOldPassword;
    @FXML private PasswordField txtNewPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private TextField txtPertanyaanKeamanan;
    @FXML private PasswordField txtJawabanKeamanan;
    @FXML private Label lblStatusAdmin;
    @FXML private TextField txtWaktuTunggu; // BARU

    private final FirebaseManager manager = FirebaseManager.getInstance();

    @FXML
    public void initialize() {
        // Ambil info asistensi saat ini dan tampilkan
        manager.getInfoRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String info = snapshot.getValue(String.class);
                if (info != null) {
                    Platform.runLater(() -> txtInputInfo.setText(info));
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {
                Platform.runLater(() -> showError("Gagal memuat info asistensi."));
            }
        });

        // BARU: Ambil info waktu tunggu saat ini
        manager.getWaktuTungguRef().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Integer detik = snapshot.getValue(Integer.class);
                if (detik != null) {
                    Platform.runLater(() -> txtWaktuTunggu.setText(detik.toString()));
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {
                Platform.runLater(() -> showError("Gagal memuat waktu tunggu."));
            }
        });
    }

    @FXML
    protected void onKembali(ActionEvent event) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        SmartQueueApp.loadScene(stage, "asisten-panel-view.fxml", "SmartQueue - Panel Asisten");
    }

    // --- Method Handler ---
    @FXML
    protected void onUpdateInfo() {
        manager.setInfoAsistensi(txtInputInfo.getText());
        showInfo("Pengumuman berhasil diperbarui!");
    }

    @FXML
    protected void onGantiUsername() {
        String newUsername = txtNewUsername.getText();
        if (newUsername.isEmpty() || newUsername.length() < 4) {
            showError("Username baru harus minimal 4 karakter.");
            return;
        }

        manager.gantiUsername(newUsername, (success, message) -> {
            Platform.runLater(() -> {
                if (success) {
                    showInfo(message);
                    txtNewUsername.clear();
                } else {
                    showError(message);
                }
            });
        });
    }

    @FXML
    protected void onGantiPassword() {
        String oldPass = txtOldPassword.getText();
        String newPass = txtNewPassword.getText();
        String confirmPass = txtConfirmPassword.getText();

        if (oldPass.isEmpty() || newPass.isEmpty()) {
            showError("Semua field password harus diisi.");
            return;
        }
        if (newPass.length() < 6) {
            showError("Password baru minimal 6 karakter.");
            return;
        }
        if (!newPass.equals(confirmPass)) {
            showError("Password baru dan konfirmasi tidak cocok.");
            return;
        }

        manager.gantiPassword(oldPass, newPass, (success, message) -> {
            Platform.runLater(() -> {
                if (success) {
                    showInfo(message);
                    txtOldPassword.clear();
                    txtNewPassword.clear();
                    txtConfirmPassword.clear();
                } else {
                    showError(message);
                }
            });
        });
    }

    @FXML
    protected void onSetPertanyaan() {
        String pertanyaan = txtPertanyaanKeamanan.getText();
        String jawaban = txtJawabanKeamanan.getText();

        manager.setPertanyaanKeamanan(pertanyaan, jawaban, (success, message) -> {
            Platform.runLater(() -> {
                if (success) {
                    showInfo(message);
                    txtPertanyaanKeamanan.clear();
                    txtJawabanKeamanan.clear();
                } else {
                    showError(message);
                }
            });
        });
    }

    // METHOD BARU
    @FXML
    protected void onUpdateWaktuTunggu() {
        try {
            int detik = Integer.parseInt(txtWaktuTunggu.getText());
            manager.setWaktuTunggu(detik, (success, message) -> {
                Platform.runLater(() -> {
                    if (success) {
                        showInfo(message);
                    } else {
                        showError(message);
                    }
                });
            });
        } catch (NumberFormatException e) {
            showError("Input tidak valid. Harap masukkan angka saja (misal: 30).");
        }
    }

    @FXML
    protected void onBukaKelolaAkun(ActionEvent event) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        SmartQueueApp.loadScene(stage, "admin-kelola-mahasiswa.fxml", "Kelola Akun Mahasiswa");
    }

    // --- Helper UI ---
    private void showInfo(String message) {
        lblStatusAdmin.setText(message);
        lblStatusAdmin.setStyle("-fx-text-fill: green;");
    }
    private void showError(String message) {
        lblStatusAdmin.setText(message);
        lblStatusAdmin.setStyle("-fx-text-fill: red;");
    }
}