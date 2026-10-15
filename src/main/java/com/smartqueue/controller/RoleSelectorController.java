package com.smartqueue.controller;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.smartqueue.SmartQueueApp;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

public class RoleSelectorController {

    private final FirebaseManager manager = FirebaseManager.getInstance();

    /**
     * KODE DEBUG UNTUK TES KONEKSI FIREBASE SAAT STARTUP
     */
    @FXML
    public void initialize() {
        System.out.println("==========================================================");
        System.out.println("DEBUG: RoleSelectorController.initialize() dimulai.");
        System.out.println("DEBUG: Mencoba mengambil instance FirebaseManager...");

        try {
            // Kita panggil getInstance() di sini untuk memicu inisialisasi Firebase
            DatabaseReference testRef = manager.getInfoRef();
            System.out.println("DEBUG: Instance FirebaseManager berhasil didapat. Mencoba membaca data...");

            // Tes membaca data sederhana (info asistensi)
            testRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    // JIKA ANDA MELIHAT INI, KONEKSI BERHASIL
                    System.out.println("==========================================================");
                    System.out.println("DEBUG: KONEKSI BERHASIL!");
                    System.out.println("Data 'info' diterima: " + snapshot.getValue());
                    System.out.println("==========================================================");
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    // JIKA ANDA MELIHAT INI, KONEKSI GAGAL
                    System.err.println("==========================================================");
                    System.err.println("DEBUG: KONEKSI GAGAL! INI ADALAH ERRORNYA:");
                    // Ini akan mencetak error yang sebenarnya (cth: "Permission Denied", "Invalid Auth Token")
                    error.toException().printStackTrace();
                    System.err.println("==========================================================");
                }
            });

            System.out.println("DEBUG: Panggilan ke Firebase (addListener) telah dikirim. Menunggu jawaban...");

        } catch (Exception e) {
            // Jika GAGAL bahkan saat inisialisasi (cth: file JSON tidak ditemukan)
            System.err.println("==========================================================");
            System.err.println("DEBUG: GAGAL TOTAL SAAT INISIALISASI FIREBASE!");
            e.printStackTrace();
            System.err.println("==========================================================");
        }
    }
    // *** AKHIR DARI KODE DEBUG ***


    @FXML
    protected void onMahasiswaClick(ActionEvent event) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        SmartQueueApp.loadScene(stage, "mahasiswa-login.fxml", "SmartQueue - Login Mahasiswa");
    }

    @FXML
    protected void onAsistenClick(ActionEvent event) {
        showLoginDialog(event);
    }

    private void showLoginDialog(ActionEvent event) {
        final Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Autentikasi Asisten");
        dialog.setHeaderText("Masukkan kredensial Asisten.");

        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        VBox vbox = new VBox(10);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        grid.add(new Label("Username:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(passwordField, 1, 1);

        Hyperlink lupaPasswordLink = new Hyperlink("Lupa Password?");
        lupaPasswordLink.setOnAction(e -> handleLupaPassword());

        vbox.getChildren().addAll(grid, lupaPasswordLink);
        dialog.getDialogPane().setContent(vbox);
        dialog.getDialogPane().setPadding(new Insets(20, 20, 10, 20));

        Platform.runLater(usernameField::requestFocus);

        final Button btnLogin = (Button) dialog.getDialogPane().lookupButton(loginButtonType);

        btnLogin.addEventFilter(ActionEvent.ACTION, ae -> {
            ae.consume();
            btnLogin.setDisable(true);
            btnLogin.setText("Memverifikasi...");

            String username = usernameField.getText();
            String password = passwordField.getText();

            manager.autentikasiAsisten(username, password, new FirebaseManager.AuthCallback() {
                @Override
                public void onSuccess() {
                    Platform.runLater(() -> {
                        dialog.close();
                        try {
                            SmartQueueApp.loadScene(currentStage, "asisten-panel-view.fxml", "SmartQueue - Panel Asisten");
                        } catch (IOException e) {
                            e.printStackTrace();
                            showAlert(Alert.AlertType.ERROR, "Error", "Gagal memuat panel asisten.");
                        }
                    });
                }
                @Override
                public void onFailure(String error) {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "Login Gagal", error);
                        btnLogin.setDisable(false);
                        btnLogin.setText("Login");
                    });
                }
            });
        });

        dialog.setResultConverter(null);
        dialog.showAndWait();
    }

    private void handleLupaPassword() {
        manager.getPertanyaanKeamanan(pertanyaan -> {
            if (pertanyaan == null || pertanyaan.equals("Belum diatur")) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Gagal", "Pertanyaan keamanan belum diatur oleh Asisten."));
                return;
            }

            Platform.runLater(() -> {
                TextInputDialog answerDialog = new TextInputDialog();
                answerDialog.setTitle("Lupa Password");
                answerDialog.setHeaderText("Pertanyaan Keamanan:\n" + pertanyaan);
                answerDialog.setContentText("Masukkan Jawaban Anda:");

                Optional<String> resultJawaban = answerDialog.showAndWait();

                if (resultJawaban.isPresent() && !resultJawaban.get().isEmpty()) {
                    String jawaban = resultJawaban.get();

                    manager.verifikasiJawabanKeamanan(jawaban, (success, message) -> {
                        Platform.runLater(() -> {
                            if (success) {
                                showResetPasswordDialog();
                            } else {
                                showAlert(Alert.AlertType.ERROR, "Gagal", message);
                            }
                        });
                    });
                }
            });
        });
    }

    private void showResetPasswordDialog() {
        TextInputDialog newPasswordDialog = new TextInputDialog();
        newPasswordDialog.setTitle("Reset Password");
        newPasswordDialog.setHeaderText("Jawaban benar. Masukkan Password Baru Anda (minimal 6 karakter):");
        Optional<String> resultPassword = newPasswordDialog.showAndWait();

        if (resultPassword.isPresent() && !resultPassword.get().isEmpty()) {
            String passwordBaru = resultPassword.get();

            manager.resetPasswordBaru(passwordBaru, (success, message) -> {
                Platform.runLater(() -> {
                    if (success) {
                        showAlert(Alert.AlertType.INFORMATION, "Sukses", message);
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Gagal", message);
                    }
                });
            });
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}