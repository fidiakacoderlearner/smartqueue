package com.smartqueue.model;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

@IgnoreExtraProperties
public class SlotKelompok {

    @Exclude
    private String namaKelompok;
    public boolean isRegistered;

    // Field ini akan diisi oleh Firebase (cth: ",123,456,")
    public String nimList;
    public String namaList;

    // === PERBAIKAN BUG #3 & #4 ===
    // Properti JavaFX untuk di-observe oleh TableView
    @Exclude
    private final StringProperty statusProperty = new SimpleStringProperty();
    @Exclude
    private final StringProperty nimListDisplayProperty = new SimpleStringProperty();
    @Exclude
    private final StringProperty namaListProperty = new SimpleStringProperty();
    // === AKHIR PERBAIKAN ===

    public SlotKelompok() {
        // Konstruktor kosong
        updateStatusProperty();
        updateNimListDisplayProperty(); // Inisialisasi
        this.namaListProperty.set(this.namaList); // Inisialisasi
    }

    // --- Getters & Setters untuk Firebase ---
    public boolean getIsRegistered() { return isRegistered; }
    public void setIsRegistered(boolean isRegistered) {
        this.isRegistered = isRegistered;
        updateStatusProperty(); // Perbarui properti status
    }

    public String getNimList() { return nimList; }
    public void setNimList(String nimList) {
        this.nimList = nimList;
        updateNimListDisplayProperty(); // Perbarui properti NIM
    }

    public String getNamaList() { return namaList; }
    public void setNamaList(String namaList) {
        this.namaList = namaList;
        this.namaListProperty.set(namaList); // Perbarui properti Nama
    }

    // --- Getters & Setters untuk JavaFX TableView ---

    @Exclude
    public String getNamaKelompok() { return namaKelompok; }
    @Exclude
    public void setNamaKelompok(String namaKelompok) { this.namaKelompok = namaKelompok; }

    @Exclude
    public String getStatus() { return statusProperty.get(); }
    @Exclude
    public StringProperty statusProperty() {
        updateStatusProperty();
        return statusProperty;
    }

    // === PERBAIKAN BARU BUG #3 & #4 ===
    @Exclude
    public StringProperty nimListProperty() {
        updateNimListDisplayProperty(); // Pastikan terformat
        return nimListDisplayProperty;
    }

    @Exclude
    public StringProperty namaListProperty() {
        this.namaListProperty.set(this.namaList); // Pastikan update
        return namaListProperty;
    }

    @Exclude
    private void updateStatusProperty() {
        statusProperty.set(isRegistered ? "Terdaftar" : "Belum Daftar");
    }

    /**
     * Helper untuk memformat NIM (Menghilangkan koma di awal/akhir)
     */
    @Exclude
    private void updateNimListDisplayProperty() {
        if (nimList == null || nimList.isEmpty()) {
            nimListDisplayProperty.set("");
            return;
        }
        String formatted = nimList;
        if (formatted.startsWith(",")) {
            formatted = formatted.substring(1);
        }
        if (formatted.endsWith(",")) {
            formatted = formatted.substring(0, formatted.length() - 1);
        }
        nimListDisplayProperty.set(formatted);
    }
    // === AKHIR PERBAIKAN ===


    /**
     * Dipanggil oleh listener untuk memperbarui data di TableView
     */
    @Exclude
    public void updateFrom(SlotKelompok updatedSlot) {
        // Panggil setter agar properti JavaFX ikut ter-update
        this.setIsRegistered(updatedSlot.getIsRegistered());
        this.setNimList(updatedSlot.getNimList());
        this.setNamaList(updatedSlot.getNamaList());
    }
}