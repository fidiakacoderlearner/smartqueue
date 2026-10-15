package com.smartqueue.controller;

import com.google.firebase.database.*;
import com.smartqueue.model.Antrian;
import com.smartqueue.model.ChatMessage;
import com.smartqueue.SmartQueueApp;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;

public class AsistenController {

    @FXML private TableView<Antrian> antrianTable;
    @FXML private TableColumn<Antrian, String> colNomor;
    @FXML private TableColumn<Antrian, String> colNama;
    @FXML private TableView<Antrian> historyTable;
    @FXML private TableColumn<Antrian, String> hisColNo;
    @FXML private TableColumn<Antrian, String> hisColNama;
    @FXML private TableColumn<Antrian, String> hisColStatus;
    @FXML private TableColumn<Antrian, String> hisColWaktuMasuk;
    @FXML private TableColumn<Antrian, String> hisColWaktuSelesai;
    @FXML private Label lblSedangDilayani;
    @FXML private Label lblStatus;

    // *** NAMA VARIABEL DIPERBAIKI DI SINI ***
    @FXML private ToggleButton btnBukaTutupAntrian;

    @FXML private Button btnPanggilBerikutnya;
    @FXML private Button btnSelesai;
    @FXML private Button btnLewati;
    @FXML private Button btnRevisi;

    @FXML private VBox vboxChat;
    @FXML private TextArea txtChatArea;
    @FXML private TextField txtPesan;
    @FXML private Button btnKirim;

    private final ObservableList<Antrian> antrianMenungguList = FXCollections.observableArrayList();
    private final ObservableList<Antrian> antrianHistoryList = FXCollections.observableArrayList();
    private final FirebaseManager manager = FirebaseManager.getInstance();
    private Antrian antrianAktifSaatIni;

    private ChildEventListener antrianMenungguListener;
    private ValueEventListener sedangDilayaniListener;
    private ChildEventListener historyListener;
    private ValueEventListener queueStatusListener;
    private ChildEventListener chatListener;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private Timeline displayTimer;
    private long waktuTungguBerakhirTimestamp = 0;

    private final SimpleDateFormat chatTimeFormatter = new SimpleDateFormat("HH:mm:ss");

    @FXML
    public void initialize() {
        colNomor.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNomor()));
        colNama.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNamaKelompok()));
        antrianTable.setItems(antrianMenungguList);

        hisColNo.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNomor()));
        hisColNama.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNamaKelompok()));
        hisColStatus.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus()));
        hisColWaktuMasuk.setCellValueFactory(cellData -> formatWaktu(cellData.getValue().getWaktuMasuk()));
        hisColWaktuSelesai.setCellValueFactory(cellData -> formatWaktu(cellData.getValue().getWaktuSelesai()));
        historyTable.setItems(antrianHistoryList);

        setChatVisibility(false);
        initializeDisplayTimer();
        attachListeners();
        updateButtonStates();
    }

    private void setChatVisibility(boolean visible) {
        vboxChat.setVisible(visible);
        vboxChat.setManaged(visible);
    }

    private void initializeDisplayTimer() {
        displayTimer = new Timeline(new KeyFrame(Duration.millis(500), event -> {
            long sisaMs = waktuTungguBerakhirTimestamp - System.currentTimeMillis();
            long sisaDetik = (sisaMs / 1000) + 1;

            if (sisaMs > 0) {
                lblStatus.setText("Waktu tunggu mahasiswa: " + sisaDetik + " detik...");
            } else {
                displayTimer.stop();
                lblStatus.setText("Waktu tunggu habis. Silakan ambil tindakan.");
                updateButtonStates();
            }
        }));
        displayTimer.setCycleCount(Timeline.INDEFINITE);
    }

    private void updateButtonStates() {
        boolean antrianKosong = antrianMenungguList.isEmpty();
        boolean tidakAdaYangDilayani = (antrianAktifSaatIni == null);

        btnPanggilBerikutnya.setDisable(antrianKosong && tidakAdaYangDilayani);
        btnSelesai.setDisable(tidakAdaYangDilayani);
        btnLewati.setDisable(tidakAdaYangDilayani);
        btnRevisi.setDisable(tidakAdaYangDilayani); // <-- TAMBAHKAN BARIS INI
    }

    private SimpleStringProperty formatWaktu(String waktuStr) {
        if (waktuStr == null) return new SimpleStringProperty("---");
        try {
            return new SimpleStringProperty(LocalDateTime.parse(waktuStr).format(formatter));
        } catch (Exception e) {
            return new SimpleStringProperty("Invalid Date");
        }
    }

    private void attachListeners() {
        sedangDilayaniListener = manager.getSedangDilayaniRef().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                antrianAktifSaatIni = snapshot.getValue(Antrian.class);

                if (displayTimer != null) displayTimer.stop();
                detachChatListener();
                setChatVisibility(false);

                if (antrianAktifSaatIni != null) {
                    waktuTungguBerakhirTimestamp = antrianAktifSaatIni.getWaktuTungguBerakhir();
                    long sisaMs = waktuTungguBerakhirTimestamp - System.currentTimeMillis();

                    if (sisaMs > 0) {
                        displayTimer.playFromStart();
                    }

                    setChatVisibility(true);
                    attachChatListener(antrianAktifSaatIni.getNomor());

                } else {
                    waktuTungguBerakhirTimestamp = 0;
                }

                Platform.runLater(() -> updateLabelSedangDilayani());
            }
            @Override
            public void onCancelled(DatabaseError error) {}
        });

        antrianMenungguListener = manager.getAntrianMenungguRef().orderByChild("waktuMasuk").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                Antrian a = snapshot.getValue(Antrian.class);
                if (a != null) Platform.runLater(() -> {
                    antrianMenungguList.add(a);
                    updateButtonStates();
                });
            }
            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {}
            @Override
            public void onChildRemoved(DataSnapshot snapshot) {
                Antrian a = snapshot.getValue(Antrian.class);
                if (a != null) Platform.runLater(() -> {
                    antrianMenungguList.removeIf(ant -> ant.getNomor().equals(a.getNomor()));
                    updateButtonStates();
                });
            }
            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {}
            @Override
            public void onCancelled(DatabaseError error) {}
        });

        historyListener = manager.getHistoryRef().orderByChild("waktuSelesai").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                Antrian a = snapshot.getValue(Antrian.class);
                if (a != null) Platform.runLater(() -> antrianHistoryList.add(a));
            }
            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {}
            @Override
            public void onChildRemoved(DataSnapshot snapshot) {
                Antrian a = snapshot.getValue(Antrian.class);
                if (a != null) Platform.runLater(() -> antrianHistoryList.removeIf(ant -> ant.getNomor().equals(a.getNomor())));
            }
            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {}
            @Override
            public void onCancelled(DatabaseError error) {}
        });

        queueStatusListener = manager.getQueueStatusRef().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Boolean isOpen = snapshot.getValue(Boolean.class);
                if (isOpen != null) {
                    Platform.runLater(() -> {
                        // *** KODE DIPERBAIKI DI SINI ***
                        btnBukaTutupAntrian.setSelected(isOpen);
                        btnBukaTutupAntrian.setText(isOpen ? "ANTRIAN DIBUKA" : "ANTRIAN DITUTUP");
                        btnBukaTutupAntrian.getStyleClass().removeAll("success", "danger");
                        if(isOpen) {
                            btnBukaTutupAntrian.getStyleClass().add("success");
                        } else {
                            btnBukaTutupAntrian.getStyleClass().add("danger");
                        }
                        // *** AKHIR PERBAIKAN ***
                    });
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    private void attachChatListener(String nomorAntrian) {
        detachChatListener();
        txtChatArea.clear();

        chatListener = manager.getChatRef(nomorAntrian)
                .orderByChild("timestamp")
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                        ChatMessage message = snapshot.getValue(ChatMessage.class);
                        if (message != null) {
                            Platform.runLater(() -> appendMessage(message));
                        }
                    }
                    @Override
                    public void onChildChanged(DataSnapshot snapshot, String previousChildName) {}
                    @Override
                    public void onChildRemoved(DataSnapshot snapshot) {}
                    @Override
                    public void onChildMoved(DataSnapshot snapshot, String previousChildName) {}
                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    private void detachChatListener() {
        if (chatListener != null && antrianAktifSaatIni != null) {
            manager.getChatRef(antrianAktifSaatIni.getNomor()).removeEventListener(chatListener);
            chatListener = null;
        }
    }

    private void appendMessage(ChatMessage message) {
        String timestampStr;
        try {
            long timestamp = (Long) message.getTimestamp();
            timestampStr = chatTimeFormatter.format(new Date(timestamp));
        } catch (Exception e) {
            timestampStr = "mengirim...";
        }

        String sender = message.getSender();
        if (sender.equals("Asisten")) sender = "Anda";

        txtChatArea.appendText(String.format("[%s] %s: %s\n",
                timestampStr,
                sender,
                message.getText()));
    }

    public void stopSync() {
        if (sedangDilayaniListener != null) manager.getSedangDilayaniRef().removeEventListener(sedangDilayaniListener);
        if (antrianMenungguListener != null) manager.getAntrianMenungguRef().removeEventListener(antrianMenungguListener);
        if (historyListener != null) manager.getHistoryRef().removeEventListener(historyListener);
        if (queueStatusListener != null) manager.getQueueStatusRef().removeEventListener(queueStatusListener);

        detachChatListener();
        if (displayTimer != null) displayTimer.stop();
    }

    private void updateLabelSedangDilayani() {
        if (antrianAktifSaatIni != null) {
            lblSedangDilayani.setText(antrianAktifSaatIni.getNomor() + " - " + antrianAktifSaatIni.getNamaKelompok() + " (Status: Aktif)");
        } else {
            lblStatus.setText("Selamat datang, Asisten.");
            lblSedangDilayani.setText("Tidak ada antrian yang aktif.");
        }
        updateButtonStates();
    }

    // --- AKSI TOMBOL (Antrian) ---
    @FXML
    protected void onPanggilBerikutnya() {
        lblStatus.setText("Memanggil antrian berikutnya...");
        manager.panggilBerikutnya();
    }

    @FXML
    protected void onSelesai() {
        if (antrianAktifSaatIni != null) {
            manager.selesaiMelayani();
            lblStatus.setText("Antrian Selesai.");
            if(displayTimer != null) displayTimer.stop();
            setChatVisibility(false);
            detachChatListener();
        } else {
            lblStatus.setText("Tidak ada antrian yang sedang aktif.");
        }
    }

    @FXML
    protected void onLewati() {
        if (antrianAktifSaatIni != null) {
            manager.lewatiAntrian();
            lblStatus.setText("Antrian Dilewati.");
            if(displayTimer != null) displayTimer.stop();
            setChatVisibility(false);
            detachChatListener();
        } else {
            lblStatus.setText("Tidak ada antrian yang sedang aktif.");
        }
    }

    @FXML
    protected void onRevisi() {
        if (antrianAktifSaatIni != null) {
            manager.revisiAntrian();
            lblStatus.setText("Antrian ditandai REVISI.");
            if(displayTimer != null) displayTimer.stop();
            setChatVisibility(false);
            detachChatListener();
        } else {
            lblStatus.setText("Tidak ada antrian yang sedang aktif.");
        }
    }

    @FXML
    protected void onResetAntrian() {
        Alert confirmAlert = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Apakah Anda yakin ingin MERESET SEMUA ANTRIAN?",
                ButtonType.YES, ButtonType.NO
        );
        confirmAlert.setTitle("Konfirmasi Reset");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            if(displayTimer != null) displayTimer.stop();
            setChatVisibility(false);
            detachChatListener(); // Ini adalah baris yang typo "Services" tadi, sudah diperbaiki

            manager.resetAntrian();
            antrianMenungguList.clear();
            antrianHistoryList.clear();
            lblStatus.setText("RESET BERHASIL!");
        }
    }

    @FXML
    protected void onKirimPesan() {
        String teks = txtPesan.getText();
        if (teks.trim().isEmpty() || antrianAktifSaatIni == null) {
            return;
        }
        manager.kirimPesan(antrianAktifSaatIni.getNomor(), "Asisten", teks);
        txtPesan.clear();
    }

    // --- TOMBOL SAKLAR ---
    @FXML
    protected void onToggleQueue() {
        // *** KODE DIPERBAIKI DI SINI ***
        boolean newState = btnBukaTutupAntrian.isSelected();
        manager.setQueueOpen(newState);
        lblStatus.setText("Status antrian diubah menjadi: " + (newState ? "DIBUKA" : "DITUTUP"));
    }

    // --- TOMBOL NAVIGASI ---
    @FXML
    protected void onBukaProfile(ActionEvent event) throws IOException {
        stopSync();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        SmartQueueApp.loadScene(stage, "admin-settings-view.fxml", "Pengaturan Profile");
    }
}