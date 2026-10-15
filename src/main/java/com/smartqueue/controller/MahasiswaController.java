package com.smartqueue.controller;

import com.google.firebase.database.*;
import com.smartqueue.SmartQueueApp;
import com.smartqueue.model.Antrian;
import com.smartqueue.model.ChatMessage;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MahasiswaController {

    // --- Field FXML Baru ---
    @FXML private Label lblSelamatDatang;
    @FXML private Button btnLogout;

    // --- Field FXML Lama (masih dipakai) ---
    @FXML private Label lblNomorAntrian;
    @FXML private Label statusLabel;
    @FXML private TextArea txtInfoAsistensi;
    @FXML private TextArea txtDaftarAntrian;
    @FXML private Button btnAmbilAntrian;
    @FXML private Label lblWaktuTunggu;
    @FXML private VBox vboxChat;
    @FXML private TextArea txtChatArea;
    @FXML private TextField txtPesan;
    @FXML private Button btnKirim;

    // --- Variabel Internal ---
    private final FirebaseManager manager = FirebaseManager.getInstance();
    private String myUsername; // BARU: Menggantikan input teks
    private String myAntrianNomor = "---"; // Nomor antrian saya (A01, A02)

    // Listeners
    private ChildEventListener antrianMenungguListener;
    private ValueEventListener sedangDilayaniListener;
    private ValueEventListener infoListener;
    private ChildEventListener historyListener;
    private ValueEventListener queueStatusListener;
    private ChildEventListener chatListener;

    private boolean isQueueOpen = false;
    private Timeline displayTimer;
    private long waktuTungguBerakhirTimestamp = 0;
    private final SimpleDateFormat chatTimeFormatter = new SimpleDateFormat("HH:mm:ss");

    @FXML
    public void initialize() {
        // Ambil username dari sesi login
        myUsername = SmartQueueApp.loggedInMahasiswaUsername;
        if (myUsername == null) {
            // Pengaman jika halaman terbuka tanpa login
            lblSelamatDatang.setText("Error: Sesi tidak ditemukan.");
            return;
        }

        lblSelamatDatang.setText("Selamat datang, " + myUsername);
        statusLabel.setText("Memuat status...");
        lblWaktuTunggu.setText("");
        setChatVisibility(false);
        initializeDisplayTimer();
        attachListeners();

        // Cek status pendaftaran awal
        checkInitialRegistrationStatus();
    }

    // BARU: Cek apakah kita sudah terdaftar di antrian saat halaman dibuka
    private void checkInitialRegistrationStatus() {
        // 1. Cek 'sedangDilayani'
        // *** PERBAIKAN DI SINI ***
        manager.getSedangDilayaniRef().orderByChild("namaKelompok").equalTo(myUsername).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Antrian a = snapshot.getChildren().iterator().next().getValue(Antrian.class);
                    myAntrianNomor = a.getNomor();
                    Platform.runLater(() -> {
                        lblNomorAntrian.setText(myAntrianNomor);
                        btnAmbilAntrian.setDisable(true); // Sudah terdaftar
                    });
                } else {
                    // 2. Jika tidak ada, cek 'antrianMenunggu'
                    // *** PERBAIKAN DI SINI ***
                    manager.getAntrianMenungguRef().orderByChild("namaKelompok").equalTo(myUsername).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                Antrian a = snapshot.getChildren().iterator().next().getValue(Antrian.class);
                                myAntrianNomor = a.getNomor();
                                Platform.runLater(() -> {
                                    lblNomorAntrian.setText(myAntrianNomor);
                                    btnAmbilAntrian.setDisable(true); // Sudah terdaftar
                                });
                            }
                        }
                        @Override public void onCancelled(DatabaseError error) {}
                    });
                }
            }
            @Override public void onCancelled(DatabaseError error) {}
        });
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
                lblWaktuTunggu.setText("SISA WAKTU ANDA: " + sisaDetik + " DETIK");
            } else {
                displayTimer.stop();
                lblWaktuTunggu.setText("WAKTU TUNGGU HABIS!");
            }
        }));
        displayTimer.setCycleCount(Timeline.INDEFINITE);
    }

    private void attachListeners() {
        // Listener Info
        infoListener = manager.getInfoRef().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String info = snapshot.getValue(String.class);
                if (info != null) Platform.runLater(() -> txtInfoAsistensi.setText(info));
            }
            @Override public void onCancelled(DatabaseError error) {}
        });

        sedangDilayaniListener = manager.getSedangDilayaniRef().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Platform.runLater(() -> updateAntrianDisplay());
            }
            @Override public void onCancelled(DatabaseError error) {}
        });

        antrianMenungguListener = manager.getAntrianMenungguRef().orderByChild("waktuMasuk").addChildEventListener(new ChildEventListener() {
            @Override public void onChildAdded(DataSnapshot snapshot, String p) { Platform.runLater(()->updateAntrianDisplay()); }
            @Override public void onChildChanged(DataSnapshot snapshot, String p) { Platform.runLater(()->updateAntrianDisplay()); }
            @Override public void onChildRemoved(DataSnapshot snapshot) { Platform.runLater(()->updateAntrianDisplay()); }
            @Override public void onChildMoved(DataSnapshot snapshot, String p) {}
            @Override public void onCancelled(DatabaseError error) {}
        });

        historyListener = manager.getHistoryRef().addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                Antrian a = snapshot.getValue(Antrian.class);
                // Cek jika antrian *saya* yang masuk history
                if (a != null && a.getNamaKelompok().equals(myUsername)) {
                    Platform.runLater(() -> resetTampilanLokal());
                }
            }
            @Override public void onChildChanged(DataSnapshot snapshot, String p) {}
            @Override public void onChildRemoved(DataSnapshot snapshot) {}
            @Override public void onChildMoved(DataSnapshot snapshot, String p) {}
            @Override public void onCancelled(DatabaseError error) {}
        });

        queueStatusListener = manager.getQueueStatusRef().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Boolean isOpen = snapshot.getValue(Boolean.class);
                if (isOpen != null) {
                    isQueueOpen = isOpen;
                    Platform.runLater(() -> {
                        // Hanya set disable jika belum terdaftar
                        if (myAntrianNomor.equals("---")) {
                            btnAmbilAntrian.setDisable(!isOpen);
                        }
                        if (!isOpen) {
                            statusLabel.setText("Antrian sedang ditutup oleh Asisten.");
                        } else {
                            if (myAntrianNomor.equals("---")) {
                                statusLabel.setText("Antrian dibuka! Silakan ambil antrian.");
                            }
                        }
                    });
                }
            }
            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    private void attachChatListener(String nomorAntrian) {
        detachChatListener();
        txtChatArea.clear();
        chatListener = manager.getChatRef(nomorAntrian).orderByChild("timestamp")
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                        ChatMessage message = snapshot.getValue(ChatMessage.class);
                        if (message != null) {
                            Platform.runLater(() -> appendMessage(message));
                        }
                    }
                    @Override public void onChildChanged(DataSnapshot snapshot, String p) {}
                    @Override public void onChildRemoved(DataSnapshot snapshot) {}
                    @Override public void onChildMoved(DataSnapshot snapshot, String p) {}
                    @Override public void onCancelled(DatabaseError error) {}
                });
    }

    private void detachChatListener() {
        if (chatListener != null && !myAntrianNomor.equals("---") && !myAntrianNomor.isEmpty()) {
            manager.getChatRef(myAntrianNomor).removeEventListener(chatListener);
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
        if (sender.equals("Mahasiswa")) sender = "Anda";

        txtChatArea.appendText(String.format("[%s] %s: %s\n",
                timestampStr,
                sender,
                message.getText()));
    }


    public void stopSync() {
        if (infoListener != null) manager.getInfoRef().removeEventListener(infoListener);
        if (sedangDilayaniListener != null) manager.getSedangDilayaniRef().removeEventListener(sedangDilayaniListener);
        if (antrianMenungguListener != null) manager.getAntrianMenungguRef().removeEventListener(antrianMenungguListener);
        if (historyListener != null) manager.getHistoryRef().removeEventListener(historyListener);
        if (queueStatusListener != null) manager.getQueueStatusRef().removeEventListener(queueStatusListener);
        detachChatListener();
        if (displayTimer != null) displayTimer.stop();
    }

    private void updateAntrianDisplay() {
        StringBuilder sb = new StringBuilder();
        if (displayTimer != null) displayTimer.stop();
        lblWaktuTunggu.setText("");
        setChatVisibility(false);
        detachChatListener();

        manager.getSedangDilayaniRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot sedangDilayaniSnapshot) {
                Antrian sedangDilayani = sedangDilayaniSnapshot.getValue(Antrian.class);
                sb.append("== STATUS ANTRIAN SAAT INI ==\n");

                if (sedangDilayani != null) {
                    sb.append("SEDANG DILAYANI: ").append(sedangDilayani.getNomor()).append(" (").append(sedangDilayani.getNamaKelompok()).append(")\n");

                    // Cek jika KITA yang dipanggil
                    if (sedangDilayani.getNamaKelompok().equals(myUsername)) {
                        statusLabel.setText("ANDA SEDANG DIPANGGIL!");
                        myAntrianNomor = sedangDilayani.getNomor(); // Sinkronkan nomor
                        lblNomorAntrian.setText(myAntrianNomor);

                        waktuTungguBerakhirTimestamp = sedangDilayani.getWaktuTungguBerakhir();
                        long sisaMs = waktuTungguBerakhirTimestamp - System.currentTimeMillis();

                        if (sisaMs > 0) {
                            displayTimer.playFromStart();
                        } else {
                            lblWaktuTunggu.setText("WAKTU TUNGGU HABIS!");
                        }

                        setChatVisibility(true);
                        attachChatListener(myAntrianNomor);
                    }
                } else {
                    sb.append("SEDANG DILAYANI: TIDAK ADA\n");
                }

                manager.getAntrianMenungguRef().orderByChild("waktuMasuk").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot menungguSnapshot) {
                        sb.append("\n== ANTRIAN MENUNGGU (").append(menungguSnapshot.getChildrenCount()).append(" Kelompok) ==\n");
                        int count = 1;
                        for (DataSnapshot child : menungguSnapshot.getChildren()) {
                            Antrian a = child.getValue(Antrian.class);
                            if (a != null) {
                                sb.append(count).append(". ").append(a.getNomor()).append(" (").append(a.getNamaKelompok()).append(")\n");
                                count++;
                            }
                        }
                        txtDaftarAntrian.setText(sb.toString());

                        if (sedangDilayani == null && !menungguSnapshot.exists()) {
                            // Panggil resetTampilanLokal HANYA jika kita tidak sedang dipanggil
                            if(myAntrianNomor.equals("---")) {
                                resetTampilanLokal();
                            }
                        }
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
            }
            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    private void resetTampilanLokal() {
        // Cek juga apakah kita sedang dilayani (untuk mencegah race condition)
        if (myAntrianNomor.equals("---")) {
            return; // Sudah di-reset
        }

        myAntrianNomor = "---";
        lblNomorAntrian.setText("---");
        statusLabel.setText("Sesi selesai atau di-reset. Silakan daftar lagi.");
        lblWaktuTunggu.setText("");
        if (displayTimer != null) displayTimer.stop();
        detachChatListener();
        setChatVisibility(false);
        btnAmbilAntrian.setDisable(!isQueueOpen); // Aktifkan lagi tombol jika antrian buka
    }

    @FXML
    protected void onAmbilAntrian() {
        if (!isQueueOpen) {
            statusLabel.setText("Gagal: Antrian sedang ditutup oleh Asisten.");
            return;
        }

        btnAmbilAntrian.setDisable(true);
        statusLabel.setText("Memproses pendaftaran...");

        // Gunakan myUsername (cth: "Kelompok 1")
        manager.tambahAntrian(myUsername, (antrianBaru, message) -> {
            Platform.runLater(() -> {
                if (antrianBaru != null) {
                    myAntrianNomor = antrianBaru.getNomor();
                    lblNomorAntrian.setText(myAntrianNomor);
                    statusLabel.setText(message); // "Anda berhasil terdaftar!"
                    statusLabel.setStyle("-fx-text-fill: green;");
                } else {
                    statusLabel.setText(message); // "Gagal: Anda sudah ada..."
                    statusLabel.setStyle("-fx-text-fill: red;");
                    // Jangan aktifkan tombol jika gagal karena duplikat ("Gagal: Anda...")
                    if (!message.startsWith("Gagal: Anda")) {
                        btnAmbilAntrian.setDisable(false); // Hanya aktifkan jika error non-duplikat
                    }
                }
            });
        });
    }

    @FXML
    protected void onKirimPesan() {
        String teks = txtPesan.getText();
        if (teks.trim().isEmpty()) {
            return;
        }
        manager.kirimPesan(myAntrianNomor, "Mahasiswa", teks);
        txtPesan.clear();
    }

    @FXML
    protected void onLogout(ActionEvent event) throws IOException {
        stopSync(); // Wajib panggil ini
        SmartQueueApp.loggedInMahasiswaUsername = null; // Hapus sesi

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        SmartQueueApp.loadScene(stage, "mahasiswa-login.fxml", "SmartQueue - Login Mahasiswa");
    }
}