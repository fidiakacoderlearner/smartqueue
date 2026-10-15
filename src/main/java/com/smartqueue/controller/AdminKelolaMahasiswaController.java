package com.smartqueue.controller;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.smartqueue.SmartQueueApp;
import com.smartqueue.model.SlotKelompok;
import javafx.application.Platform; // <-- IMPORT BARU
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

public class AdminKelolaMahasiswaController {

    @FXML private Label lblStatus;
    @FXML private TextField txtNamaKelompokBaru;
    @FXML private TableView<SlotKelompok> slotTable;
    @FXML private TableColumn<SlotKelompok, String> colNamaKelompok;
    @FXML private TableColumn<SlotKelompok, String> colStatus;
    @FXML private TableColumn<SlotKelompok, String> colNIM;
    @FXML private TableColumn<SlotKelompok, String> colNama;
    @FXML private TableColumn<SlotKelompok, Void> colAksi;

    private final FirebaseManager manager = FirebaseManager.getInstance();
    private final ObservableList<SlotKelompok> slotList = FXCollections.observableArrayList();
    private ChildEventListener slotListener;

    @FXML
    public void initialize() {
        // Setup Tabel
        colNamaKelompok.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNamaKelompok()));
        colStatus.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        colNIM.setCellValueFactory(cellData -> cellData.getValue().nimListProperty());
        colNama.setCellValueFactory(cellData -> cellData.getValue().namaListProperty());

        // Kolom Aksi (Tombol Reset & Hapus)
        colAksi.setCellFactory(param -> new TableCell<>() {
            private final Button btnReset = new Button("Reset");
            private final Button btnHapus = new Button("Hapus");
            private final HBox pane = new HBox(5, btnReset, btnHapus);

            {
                btnReset.getStyleClass().add("warning");
                btnHapus.getStyleClass().add("danger");
                pane.setAlignment(Pos.CENTER);

                btnReset.setOnAction(event -> {
                    SlotKelompok slot = getTableView().getItems().get(getIndex());
                    handleReset(slot);
                });
                btnHapus.setOnAction(event -> {
                    SlotKelompok slot = getTableView().getItems().get(getIndex());
                    handleHapus(slot);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        slotTable.setItems(slotList);
        attachListener();
    }

    private void attachListener() {
        if (slotListener != null) {
            manager.getSlotRef().removeEventListener(slotListener);
        }
        slotList.clear();

        slotListener = manager.getSlotRef().addChildEventListener(new ChildEventListener() {

            // *** INI ADALAH PERBAIKANNYA ***
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                Platform.runLater(() -> {
                    SlotKelompok slot = snapshot.getValue(SlotKelompok.class);
                    slot.setNamaKelompok(snapshot.getKey());
                    slotList.add(slot);
                });
            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
                Platform.runLater(() -> {
                    SlotKelompok updatedSlot = snapshot.getValue(SlotKelompok.class);
                    updatedSlot.setNamaKelompok(snapshot.getKey());

                    slotList.stream()
                            .filter(s -> s.getNamaKelompok().equals(updatedSlot.getNamaKelompok()))
                            .findFirst()
                            .ifPresent(s -> s.updateFrom(updatedSlot));
                });
            }

            @Override
            public void onChildRemoved(DataSnapshot snapshot) {
                Platform.runLater(() -> {
                    slotList.removeIf(s -> s.getNamaKelompok().equals(snapshot.getKey()));
                });
            }
            // *** AKHIR PERBAIKAN ***

            @Override public void onChildMoved(DataSnapshot snapshot, String previousChildName) {}
            @Override public void onCancelled(DatabaseError error) {
                Platform.runLater(() -> {
                    showError("Gagal memuat listener slot: " + error.getMessage());
                });
            }
        });
    }

    @FXML
    protected void onKembali(ActionEvent event) throws IOException {
        if (slotListener != null) {
            manager.getSlotRef().removeEventListener(slotListener);
        }
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        SmartQueueApp.loadScene(stage, "admin-settings-view.fxml", "Pengaturan Profile");
    }

    @FXML
    protected void onTambahSlot() {
        String namaKelompok = txtNamaKelompokBaru.getText();
        manager.tambahSlotKelompok(namaKelompok, (success, message) -> {
            Platform.runLater(() -> {
                if (success) {
                    showInfo(message);
                    txtNamaKelompokBaru.clear();
                } else {
                    showError(message);
                }
            });
        });
    }


    // ============ INI ADALAH METODE YANG DIPERBAIKI (TEKS-NYA) ============
    private void handleReset(SlotKelompok slot) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Konfirmasi Reset");
        confirmAlert.setHeaderText("Reset Slot '" + slot.getNamaKelompok() + "'?");

        // Teks diperbarui dengan \n agar ada baris baru
        confirmAlert.setContentText("Ini akan menghapus akun login yang terdaftar dan mengizinkan\nkelompok baru mendaftar ulang di slot ini. Lanjutkan?");

        confirmAlert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            manager.resetSlot(slot.getNamaKelompok(), (success, message) -> {
                Platform.runLater(() -> {
                    if (success) showInfo(message);
                    else showError(message);
                });
            });
        }
    }

    // ============ INI ADALAH METODE YANG DIPERBAIKI (TEKS-NYA) ============
    private void handleHapus(SlotKelompok slot) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Konfirmasi Hapus");
        confirmAlert.setHeaderText("HAPUS SLOT '" + slot.getNamaKelompok() + "'?");

        // Teks diperbarui dengan \n agar ada baris baru
        confirmAlert.setContentText("PERINGATAN: Ini akan menghapus slot DAN akun yang terdaftar\nsecara permanen. Anda tidak bisa mengembalikannya. Lanjutkan?");

        confirmAlert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            manager.hapusSlotDanAkun(slot.getNamaKelompok(), (success, message) -> {
                Platform.runLater(() -> {
                    if (success) showInfo(message);
                    else showError(message);
                });
            });
        }
    }
    // ============ AKHIR DARI METODE YANG DIPERBAIKI ============


    private void showInfo(String message) {
        lblStatus.setText(message);
        lblStatus.setStyle("-fx-text-fill: green;");
    }
    private void showError(String message) {
        lblStatus.setText(message);
        lblStatus.setStyle("-fx-text-fill: red;");
    }
}