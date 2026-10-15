package com.smartqueue;

import javafx.application.Application;

/**
 * Ini adalah titik masuk (Main Class) yang benar untuk aplikasi JavaFX non-modular.
 * File ini hanya memiliki satu tugas: meluncurkan file SmartQueueApp.
 */
public class Launcher {
    public static void main(String[] args) {
        // Panggil Application.launch() dengan menunjuk ke kelas App utama Anda
        Application.launch(SmartQueueApp.class, args);
    }
}