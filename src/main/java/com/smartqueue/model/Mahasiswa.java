package com.smartqueue.model;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Mahasiswa {
    public String nama;

    public Mahasiswa() {
        // Konstruktor kosong WAJIB untuk Firebase
    }

    public Mahasiswa(String nama) {
        this.nama = nama;
    }

    public String getNama() {
        return nama;
    }
}