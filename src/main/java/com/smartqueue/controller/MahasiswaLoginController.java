package com.smartqueue.controller;

import com.smartqueue.SmartQueueApp;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class MahasiswaLoginController {

    @FXML private Label lblStatus;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;

    private final FirebaseManager manager = FirebaseManager.getInstance();

    @FXML
    public void initialize() {
        lblStatus.setText(""); // Kosongkan status
    }

    @FXML
    protected void onLogin(ActionEvent event) {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        if (username.trim().isEmpty() || password.isEmpty()) {
            showError("Username dan Password tidak boleh kosong.");
            return;
        }

        setLoading(true);

        manager.autentikasiMahasiswa(username, password, new FirebaseManager.AuthCallback() {
            @Override
            public void onSuccess() {
                Platform.runLater(() -> {
                    try {
                        // SIMPAN username ke sesi global
                        SmartQueueApp.loggedInMahasiswaUsername = username;

                        // Pindah ke Halaman Dashboard Mahasiswa (yang akan kita buat)
                        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                        SmartQueueApp.loadScene(stage, "mahasiswa-view.fxml", "SmartQueue - Dashboard");

                    } catch (IOException e) {
                        e.printStackTrace();
                        showError("Gagal memuat halaman dashboard.");
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                Platform.runLater(() -> {
                    showError(error);
                    setLoading(false);
                });
            }
        });
    }

    @FXML
    protected void onBukaRegistrasi(ActionEvent event) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        SmartQueueApp.loadScene(stage, "mahasiswa-register.fxml", "Registrasi Akun Kelompok");
    }

    @FXML
    protected void onKembali(ActionEvent event) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        SmartQueueApp.loadScene(stage, "role-selector.fxml", "SmartQueue - Pilih Akses");
    }

    private void setLoading(boolean isLoading) {
        txtUsername.setDisable(isLoading);
        txtPassword.setDisable(isLoading);
        if (isLoading) {
            lblStatus.setText("Mencoba login...");
            lblStatus.setStyle("-fx-text-fill: orange;");
        }
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