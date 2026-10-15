package com.smartqueue.controller;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.*;
import com.smartqueue.model.Antrian;
import com.smartqueue.model.ChatMessage;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class FirebaseManager {

    private static FirebaseManager instance;
    private final FirebaseDatabase database;

    private final DatabaseReference antrianMenungguRef;
    private final DatabaseReference historyRef;
    private final DatabaseReference infoRef;
    private final DatabaseReference sedangDilayaniRef;
    private final DatabaseReference counterRef;
    private final DatabaseReference adminRef;
    private final DatabaseReference queueStatusRef;
    private final DatabaseReference waktuTungguRef;
    private final DatabaseReference chatRef;
    private final DatabaseReference slotRef;
    private final DatabaseReference accountRef;


    private static final String FIREBASE_KEY_FILE = "serviceAccountKey.json";
    private static final String DATABASE_URL = "https://smartqueue-proyek-akhir-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private FirebaseManager() {
        try {
            InputStream serviceAccount = FirebaseManager.class.getResourceAsStream("/com/smartqueue/" + FIREBASE_KEY_FILE);
            if (serviceAccount == null) {
                throw new IOException("File kunci Firebase '" + FIREBASE_KEY_FILE + "' tidak ditemukan di path: /com/smartqueue/");
            }
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl(DATABASE_URL)
                    .build();
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
            database = FirebaseDatabase.getInstance();
            antrianMenungguRef = database.getReference("antrian/menunggu");
            historyRef = database.getReference("antrian/history");
            infoRef = database.getReference("info/asistensi");
            sedangDilayaniRef = database.getReference("antrian/sedangDilayani");
            counterRef = database.getReference("antrian/counter");
            adminRef = database.getReference("admin");
            queueStatusRef = database.getReference("info/isQueueOpen");
            waktuTungguRef = database.getReference("info/waktuTungguDetik");
            chatRef = database.getReference("chat");
            slotRef = database.getReference("admin/slots");
            accountRef = database.getReference("mahasiswa_accounts");

            // ============ PERBAIKAN BUG CACHE STALE ============
            // Perintahkan Firebase SDK untuk selalu menjaga path ini
            // tetap sinkron dan tidak terlalu mengandalkan cache basi.
            slotRef.keepSynced(true);
            accountRef.keepSynced(true);
            // ============ AKHIR PERBAIKAN ============

            initializeDefaultData();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Gagal menginisialisasi Firebase Admin SDK", e);
        }
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    private void initializeDefaultData() {
        infoRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snapshot) { if (!snapshot.exists()) infoRef.setValueAsync("Info asistensi belum diatur."); }
            @Override public void onCancelled(DatabaseError error) {}
        });
        counterRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snapshot) { if (!snapshot.exists()) counterRef.setValueAsync(1); }
            @Override public void onCancelled(DatabaseError error) {}
        });
        adminRef.child("username").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snapshot) { if (!snapshot.exists()) adminRef.child("username").setValueAsync("asisten"); }
            @Override public void onCancelled(DatabaseError error) {}
        });
        adminRef.child("password").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snapshot) { if (!snapshot.exists()) adminRef.child("password").setValueAsync("asisten123"); }
            @Override public void onCancelled(DatabaseError error) {}
        });
        adminRef.child("pertanyaan").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snapshot) { if (!snapshot.exists()) adminRef.child("pertanyaan").setValueAsync("Belum diatur"); }
            @Override public void onCancelled(DatabaseError error) {}
        });
        adminRef.child("jawaban").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snapshot) { if (!snapshot.exists()) adminRef.child("jawaban").setValueAsync(""); }
            @Override public void onCancelled(DatabaseError error) {}
        });
        queueStatusRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) queueStatusRef.setValueAsync(false);
            }
            @Override
            public void onCancelled(DatabaseError error) {}
        });
        waktuTungguRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) waktuTungguRef.setValueAsync(30);
            }
            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    // --- PINTU AKSES (Listeners) ---
    public DatabaseReference getAntrianMenungguRef() { return antrianMenungguRef; }
    public DatabaseReference getHistoryRef() { return historyRef; }
    public DatabaseReference getSedangDilayaniRef() { return sedangDilayaniRef; }
    public DatabaseReference getInfoRef() { return infoRef; }
    public DatabaseReference getQueueStatusRef() { return queueStatusRef; }
    public DatabaseReference getWaktuTungguRef() { return waktuTungguRef; }
    public DatabaseReference getChatRef(String nomorAntrian) {
        return chatRef.child(nomorAntrian).child("messages");
    }
    public DatabaseReference getSlotRef() { return slotRef; }


    // --- Callback Interfaces ---
    public interface AuthCallback { void onSuccess(); void onFailure(String error); }
    public interface OperationCallback { void onComplete(boolean success, String message); }
    public interface AntrianCallback { void onComplete(Antrian antrian, String message); }
    public interface PertanyaanCallback { void onResult(String pertanyaan); }

    // --- OPERASI TULIS (Antrian) ---
    public void tambahAntrian(String usernameKelompok, AntrianCallback callback) {
        // 1. Cek duplikat di 'sedangDilayani'
        sedangDilayaniRef.orderByChild("namaKelompok").equalTo(usernameKelompok).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    callback.onComplete(null, "Gagal: Anda (" + usernameKelompok + ") sedang dilayani.");
                    return;
                }
                // 2. Cek duplikat di 'antrianMenunggu'
                antrianMenungguRef.orderByChild("namaKelompok").equalTo(usernameKelompok).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            callback.onComplete(null, "Gagal: Anda (" + usernameKelompok + ") sudah ada dalam antrian.");
                            return;
                        }
                        // 3. Cek 'history' untuk status "Selesai"
                        historyRef.orderByChild("namaKelompok").equalTo(usernameKelompok).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot historySnapshot) {
                                if (historySnapshot.exists()) {
                                    boolean sudahSelesai = false;
                                    for (DataSnapshot child : historySnapshot.getChildren()) {
                                        Antrian historyAntrian = child.getValue(Antrian.class);
                                        if (historyAntrian != null && "Selesai".equals(historyAntrian.getStatus())) {
                                            sudahSelesai = true;
                                            break;
                                        }
                                    }
                                    if (sudahSelesai) {
                                        callback.onComplete(null, "Gagal: Anda (" + usernameKelompok + ") sudah menyelesaikan asistensi.");
                                        return;
                                    }
                                }
                                // 4. Jika lolos semua 3 cek, lanjutkan pendaftaran
                                jalankanTransaksiCounter(usernameKelompok, callback);
                            }
                            @Override
                            public void onCancelled(DatabaseError error) {
                                callback.onComplete(null, "Error: " + error.getMessage());
                            }
                        });
                    }
                    @Override
                    public void onCancelled(DatabaseError error) {
                        callback.onComplete(null, "Error: " + error.getMessage());
                    }
                });
            }
            @Override
            public void onCancelled(DatabaseError error) {
                callback.onComplete(null, "Error: " + error.getMessage());
            }
        });
    }

    private void jalankanTransaksiCounter(String usernameKelompok, AntrianCallback callback) {
        counterRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                Integer currentCounter = mutableData.getValue(Integer.class);
                if (currentCounter == null) currentCounter = 1;
                mutableData.setValue(currentCounter + 1);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean committed, DataSnapshot dataSnapshot) {
                if (committed) {
                    Integer newCounter = dataSnapshot.getValue(Integer.class);
                    String nomor = String.format("A%02d", newCounter - 1);
                    // Gunakan usernameKelompok sebagai namaKelompok
                    Antrian antrianBaru = new Antrian(nomor, usernameKelompok);

                    antrianMenungguRef.child(nomor).setValue(antrianBaru, (dbError, dbRef) -> {
                        if (dbError == null) {
                            callback.onComplete(antrianBaru, "Anda berhasil terdaftar!");
                        } else {
                            callback.onComplete(null, "Gagal menyimpan data antrian.");
                        }
                    });
                } else {
                    callback.onComplete(null, "Gagal mendapatkan nomor antrian.");
                }
            }
        });
    }

    public void panggilBerikutnya() {
        waktuTungguRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot waktuSnapshot) {
                Integer detik = waktuSnapshot.getValue(Integer.class);
                if (detik == null) detik = 30;
                long waktuBerakhir = System.currentTimeMillis() + (detik * 1000);
                prosesPanggilBerikutnya(waktuBerakhir);
            }
            @Override
            public void onCancelled(DatabaseError error) {
                long waktuBerakhir = System.currentTimeMillis() + 30000;
                prosesPanggilBerikutnya(waktuBerakhir);
            }
        });
    }

    private void prosesPanggilBerikutnya(long waktuBerakhir) {
        sedangDilayaniRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Map<String, Object> dataSelesai = (Map<String, Object>) snapshot.getValue();
                    dataSelesai.put("status", "Dilewati"); // Perbaikan bug dari sebelumnya
                    dataSelesai.put("waktuSelesai", LocalDateTime.now().toString());
                    historyRef.child(dataSelesai.get("nomor").toString()).setValueAsync(dataSelesai);
                    chatRef.child(dataSelesai.get("nomor").toString()).removeValueAsync();
                }
                Query antrianPertama = antrianMenungguRef.orderByChild("waktuMasuk").limitToFirst(1);
                antrianPertama.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (!dataSnapshot.hasChildren()) {
                            sedangDilayaniRef.removeValueAsync();
                            return;
                        }
                        DataSnapshot antrianBaruSnapshot = dataSnapshot.getChildren().iterator().next();
                        Map<String, Object> antrianBaruData = (Map<String, Object>) antrianBaruSnapshot.getValue();
                        antrianBaruData.put("status", "Aktif");
                        antrianBaruData.put("waktuTungguBerakhir", waktuBerakhir);
                        sedangDilayaniRef.setValueAsync(antrianBaruData);
                        antrianMenungguRef.child(antrianBaruSnapshot.getKey()).removeValueAsync();
                        String nomorAntrianBaru = antrianBaruSnapshot.getKey();
                        long detik = (waktuBerakhir - System.currentTimeMillis()) / 1000;
                        if (detik < 0) detik = 0;
                        ChatMessage welcomeMsg = new ChatMessage("Sistem", "Chat dibuka. Sisa waktu " + detik + " detik.");
                        getChatRef(nomorAntrianBaru).push().setValueAsync(welcomeMsg);
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {}
                });
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    private void pindahKeHistory(String status) {
        sedangDilayaniRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Map<String, Object> data = (Map<String, Object>) snapshot.getValue();
                    String nomorAntrian = data.get("nomor").toString();
                    data.put("status", status);
                    data.put("waktuSelesai", LocalDateTime.now().toString());
                    historyRef.child(nomorAntrian).setValueAsync(data);
                    sedangDilayaniRef.removeValueAsync();
                    chatRef.child(nomorAntrian).removeValueAsync();
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }
    public void selesaiMelayani() { pindahKeHistory("Selesai"); }
    public void lewatiAntrian() { pindahKeHistory("Dilewati"); }
    public void revisiAntrian() { pindahKeHistory("Revisi"); }
    public void resetAntrian() {
        antrianMenungguRef.removeValueAsync();
        historyRef.removeValueAsync();
        sedangDilayaniRef.removeValueAsync();
        counterRef.setValueAsync(1);
        chatRef.removeValueAsync();
    }
    public void kirimPesan(String nomorAntrian, String sender, String text) {
        if (nomorAntrian == null || nomorAntrian.isEmpty() || text == null || text.trim().isEmpty()) {
            return;
        }
        ChatMessage message = new ChatMessage(sender, text.trim());
        getChatRef(nomorAntrian).push().setValueAsync(message);
    }
    public void setInfoAsistensi(String info) { infoRef.setValueAsync(info); }
    public void setQueueOpen(boolean isOpen) { queueStatusRef.setValueAsync(isOpen); }
    public void setWaktuTunggu(int detik, OperationCallback callback) {
        if (detik < 10) {
            callback.onComplete(false, "Waktu tunggu minimal 10 detik.");
            return;
        }
        waktuTungguRef.setValue(detik, (error, ref) -> {
            if (error == null) {
                callback.onComplete(true, "Waktu tunggu berhasil diatur ke " + detik + " detik.");
            } else {
                callback.onComplete(false, "Gagal memperbarui database.");
            }
        });
    }

    // --- FUNGSI MANAJEMEN AKUN ---
    public void tambahSlotKelompok(String namaKelompok, OperationCallback callback) {
        if (namaKelompok == null || namaKelompok.trim().isEmpty()) {
            callback.onComplete(false, "Nama kelompok tidak boleh kosong.");
            return;
        }
        String sanitizedKey = namaKelompok.trim();
        slotRef.child(sanitizedKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    callback.onComplete(false, "Gagal: Slot untuk '" + sanitizedKey + "' sudah ada.");
                } else {
                    Map<String, Object> newSlot = new HashMap<>();
                    newSlot.put("isRegistered", false);
                    newSlot.put("nimList", "");
                    newSlot.put("namaList", "");
                    slotRef.child(sanitizedKey).setValue(newSlot, (error, ref) -> {
                        if (error == null) {
                            callback.onComplete(true, "Slot '" + sanitizedKey + "' berhasil dibuat.");
                        } else {
                            callback.onComplete(false, "Gagal menyimpan ke database: " + error.getMessage());
                        }
                    });
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {
                callback.onComplete(false, error.getMessage());
            }
        });
    }
    public void hapusSlotDanAkun(String namaKelompok, OperationCallback callback) {
        String sanitizedKey = namaKelompok.trim();
        slotRef.child(sanitizedKey).removeValue((error, ref) -> {
            if (error == null) {
                accountRef.child(sanitizedKey).removeValueAsync();
                callback.onComplete(true, "Slot dan Akun '" + sanitizedKey + "' berhasil dihapus.");
            } else {
                callback.onComplete(false, "Gagal menghapus slot: " + error.getMessage());
            }
        });
    }
    public void resetSlot(String namaKelompok, OperationCallback callback) {
        String sanitizedKey = namaKelompok.trim();
        Map<String, Object> resetData = new HashMap<>();
        resetData.put("isRegistered", false);
        resetData.put("nimList", "");
        resetData.put("namaList", "");
        slotRef.child(sanitizedKey).updateChildren(resetData, (error, ref) -> {
            if (error == null) {
                accountRef.child(sanitizedKey).removeValueAsync();
                callback.onComplete(true, "Slot '" + sanitizedKey + "' berhasil di-reset.");
            } else {
                callback.onComplete(false, "Gagal me-reset slot: " + error.getMessage());
            }
        });
    }

    // --- LOGIKA REGISTRASI MAHASISWA BARU ---
    public void registrasiMahasiswa(String slotNama, String password, List<String> nims, List<String> namas, OperationCallback callback) {

        // 1. Validasi NIM duplikat dalam input mereka sendiri
        for (String nim : nims) {
            if (java.util.Collections.frequency(nims, nim) > 1) {
                callback.onComplete(false, "Gagal: NIM " + nim + " dimasukkan lebih dari satu kali.");
                return;
            }
        }

        // 2. Cek apakah NIM sudah terdaftar di kelompok LAIN
        slotRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot allSlotsSnapshot) {
                String nimTerduplikasi = "";
                for (DataSnapshot slot : allSlotsSnapshot.getChildren()) {
                    // Jangan cek slot kita sendiri
                    if (slot.getKey().equals(slotNama)) {
                        continue;
                    }

                    String nimListString = slot.child("nimList").getValue(String.class);
                    if (nimListString != null && !nimListString.isEmpty()) {
                        for (String nimToCheck : nims) {
                            if (nimListString.contains("," + nimToCheck + ",")) {
                                nimTerduplikasi = nimToCheck;
                                break;
                            }
                        }
                    }
                    if (!nimTerduplikasi.isEmpty()) break;
                }

                if (!nimTerduplikasi.isEmpty()) {
                    callback.onComplete(false, "Gagal: NIM " + nimTerduplikasi + " sudah terdaftar di kelompok lain.");
                    return;
                }

                // 3. Jika NIM aman, lanjutkan ke proses Transaksi
                jalankanTransaksiRegistrasi(slotNama, password, nims, namas, callback);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onComplete(false, "Error database: " + error.getMessage());
            }
        });
    }


    // ============ INI ADALAH METODE TRANSAKSI YANG DIPERBAIKI ============
    private void jalankanTransaksiRegistrasi(String slotNama, String password, List<String> nims, List<String> namas, OperationCallback callback) {

        DatabaseReference targetSlotRef = slotRef.child(slotNama);

        targetSlotRef.runTransaction(new Transaction.Handler() {

            // Variabel untuk menyimpan alasan pasti kenapa transaksi abort
            private String abortReason = "TERDAFTAR"; // Default

            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {

                // Cek 1: Apakah slot-nya ada?
                if (mutableData.getValue() == null) {
                    abortReason = "TIDAK_ADA";
                    return Transaction.abort();
                }

                // Cek 2: Apakah datanya valid?
                Boolean isRegistered = mutableData.child("isRegistered").getValue(Boolean.class);
                if (isRegistered == null) {
                    // Seharusnya tidak pernah terjadi jika slot dibuat dgn benar
                    abortReason = "DATA_KORUP";
                    return Transaction.abort();
                }

                // Cek 3: Apakah sudah terdaftar?
                if (isRegistered) {
                    abortReason = "TERDAFTAR"; // Ini adalah kegagalan yang "normal"
                    return Transaction.abort();
                }

                // --- Jika lolos semua cek ---
                String nimListString = "," + String.join(",", nims) + ",";
                String namaListString = String.join(", ", namas);

                mutableData.child("isRegistered").setValue(true);
                mutableData.child("nimList").setValue(nimListString);
                mutableData.child("namaList").setValue(namaListString);

                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean committed, DataSnapshot dataSnapshot) {

                if (committed) {
                    // Transaksi sukses
                    Map<String, Object> accountData = new HashMap<>();
                    accountData.put("password", password);
                    accountData.put("nimList", dataSnapshot.child("nimList").getValue(String.class));
                    accountData.put("namaList", dataSnapshot.child("namaList").getValue(String.class));

                    accountRef.child(slotNama).setValue(accountData, (error, ref) -> {
                        if (error == null) {
                            callback.onComplete(true, "Registrasi " + slotNama + " berhasil!");
                        } else {
                            callback.onComplete(false, "Gagal menyimpan akun: " + error.getMessage());
                        }
                    });

                } else if (databaseError != null) {
                    // Terjadi error database (koneksi, permission, dll)
                    callback.onComplete(false, "Error database: " + databaseError.getMessage());

                } else {
                    // Transaksi di-abort secara manual oleh doTransaction
                    // Kita cek alasan kenapa di-abort
                    switch(abortReason) {
                        case "TERDAFTAR":
                            callback.onComplete(false, "Gagal: Slot '" + slotNama + "' sudah terdaftar oleh orang lain. Silakan pilih slot berbeda.");
                            break;
                        case "TIDAK_ADA":
                            callback.onComplete(false, "Gagal: Slot '" + slotNama + "' tidak ditemukan di database.");
                            break;
                        case "DATA_KORUP":
                            callback.onComplete(false, "Gagal: Data untuk slot '" + slotNama + "' korup. Hubungi Asisten.");
                            break;
                        default:
                            callback.onComplete(false, "Gagal: Registrasi dibatalkan untuk slot '" + slotNama + "'.");
                    }
                }
            }
        });
    }
    // ============ AKHIR DARI METODE YANG DIPERBAIKI ============


    // --- FUNGSI AUTENTIKASI ---

    public void autentikasiAsisten(String username, String password, AuthCallback callback) {
        adminRef.child("username").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot usernameSnapshot) {
                if (!usernameSnapshot.exists()) {
                    callback.onFailure("Data admin (username) tidak ditemukan.");
                    return;
                }

                String dbUsername = usernameSnapshot.getValue(String.class);

                if (dbUsername == null || !dbUsername.equals(username)) {
                    callback.onFailure("Username atau Password salah.");
                    return;
                }

                adminRef.child("password").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot passwordSnapshot) {
                        String dbPassword = passwordSnapshot.getValue(String.class);
                        if (dbPassword != null && dbPassword.equals(password)) {
                            callback.onSuccess();
                        } else {
                            callback.onFailure("Username atau Password salah.");
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError error) {
                        callback.onFailure(error.getMessage());
                    }
                });
            }
            @Override
            public void onCancelled(DatabaseError error) {
                callback.onFailure(error.getMessage());
            }
        });
    }

    public void autentikasiMahasiswa(String username, String password, AuthCallback callback) {
        if (username.trim().isEmpty() || password.isEmpty()) {
            callback.onFailure("Username dan Password tidak boleh kosong.");
            return;
        }

        accountRef.child(username.trim()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    callback.onFailure("Akun untuk '" + username + "' tidak ditemukan.");
                    return;
                }

                String dbPassword = snapshot.child("password").getValue(String.class);
                if (dbPassword != null && dbPassword.equals(password)) {
                    callback.onSuccess();
                } else {
                    callback.onFailure("Password yang Anda masukkan salah.");
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {
                callback.onFailure("Error database: " + error.getMessage());
            }
        });
    }

    public void gantiPassword(String oldPassword, String newPassword, OperationCallback callback) {
        adminRef.child("password").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String currentPassword = snapshot.getValue(String.class);
                if (currentPassword != null && currentPassword.equals(oldPassword)) {
                    snapshot.getRef().setValue(newPassword, (error, ref) -> {
                        if (error == null) {
                            callback.onComplete(true, "Password berhasil diperbarui.");
                        } else {
                            callback.onComplete(false, "Gagal memperbarui password di database.");
                        }
                    });
                } else {
                    callback.onComplete(false, "Password lama yang Anda masukkan salah.");
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {
                callback.onComplete(false, error.getMessage());
            }
        });
    }

    public void gantiUsername(String newUsername, OperationCallback callback) {
        adminRef.child("username").setValue(newUsername, (error, ref) -> {
            if (error == null) {
                callback.onComplete(true, "Username berhasil diperbarui.");
            } else {
                callback.onComplete(false, "Gagal memperbarui username.");
            }
        });
    }

    public void getPertanyaanKeamanan(PertanyaanCallback callback) {
        adminRef.child("pertanyaan").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String pertanyaan = snapshot.getValue(String.class);
                callback.onResult(pertanyaan != null ? pertanyaan : "Belum diatur");
            }
            @Override
            public void onCancelled(DatabaseError error) {
                callback.onResult(null);
            }
        });
    }

    public void setPertanyaanKeamanan(String pertanyaan, String jawaban, OperationCallback callback) {
        if (pertanyaan.isEmpty() || jawaban.isEmpty()) {
            callback.onComplete(false, "Pertanyaan dan Jawaban tidak boleh kosong.");
            return;
        }
        adminRef.child("pertanyaan").setValueAsync(pertanyaan);
        adminRef.child("jawaban").setValueAsync(jawaban);
        callback.onComplete(true, "Pertanyaan Keamanan berhasil diperbarui.");
    }

    public void verifikasiJawabanKeamanan(String jawaban, OperationCallback callback) {
        adminRef.child("jawaban").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String dbJawaban = snapshot.getValue(String.class);
                if (dbJawaban != null && dbJawaban.equalsIgnoreCase(jawaban)) {
                    callback.onComplete(true, "Jawaban benar.");
                } else {
                    callback.onComplete(false, "Jawaban keamanan salah.");
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {
                callback.onComplete(false, error.getMessage());
            }
        });
    }

    public void resetPasswordBaru(String passwordBaru, OperationCallback callback) {
        if (passwordBaru.length() < 6) {
            callback.onComplete(false, "Password baru minimal 6 karakter.");
            return;
        }
        adminRef.child("password").setValue(passwordBaru, (error, ref) -> {
            if (error == null) {
                callback.onComplete(true, "Password berhasil di-reset.");
            } else {
                callback.onComplete(false, "Gagal mereset password.");
            }
        });
    }
}