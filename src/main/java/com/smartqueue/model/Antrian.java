package com.smartqueue.model;

import com.google.firebase.database.IgnoreExtraProperties;
import java.time.LocalDateTime;

@IgnoreExtraProperties
public class Antrian {
    public String nomor;
    public String namaKelompok;
    public String status;
    public String waktuMasuk;
    public String waktuSelesai;
    public long waktuTungguBerakhir; // BARU: Stempel waktu UNIX (ms)

    public Antrian() {
        // Konstruktor kosong WAJIB untuk Firebase
    }

    // Konstruktor untuk membuat antrian baru
    public Antrian(String nomor, String namaKelompok) {
        this.nomor = nomor;
        this.namaKelompok = namaKelompok;
        this.status = "Menunggu";
        this.waktuMasuk = LocalDateTime.now().toString();
        this.waktuSelesai = null;
        this.waktuTungguBerakhir = 0; // BARU: Default 0
    }

    // Getters
    public String getNomor() { return nomor; }
    public String getNamaKelompok() { return namaKelompok; }
    public String getStatus() { return status; }
    public String getWaktuMasuk() { return waktuMasuk; }
    public String getWaktuSelesai() { return waktuSelesai; }
    public long getWaktuTungguBerakhir() { return waktuTungguBerakhir; } // BARU
}