package com.smartqueue;

import com.smartqueue.controller.AsistenController;
import com.smartqueue.controller.FirebaseManager;
import com.smartqueue.controller.MahasiswaController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class SmartQueueApp extends Application {

    // Pastikan bootstrap3.css ada di folder resources/com/smartqueue
    public static final String BOOTSTRAP_CSS = SmartQueueApp.class.getResource("bootstrap3.css").toExternalForm();

    // BARU: Variabel statis untuk menyimpan sesi login
    public static String loggedInMahasiswaUsername = null;

    @Override
    public void start(Stage stage) throws IOException {
        // 1. Inisialisasi Database Firebase (satu kali saat aplikasi start)
        FirebaseManager.getInstance();

        // 2. Memuat Role Selector sebagai titik awal
        loadScene(stage, "role-selector.fxml", "SmartQueue - Pilih Akses");
    }

    @Override
    public void stop() {
        // (Opsional, tapi praktik yang baik)
    }

    /**
     * Method helper untuk memuat scene baru
     * dan menangani penutupan jendela (melepas listener).
     */
    public static void loadScene(Stage stage, String fxmlFile, String title) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(SmartQueueApp.class.getResource(fxmlFile));
        Scene scene = new Scene(fxmlLoader.load());

        // Mengambil controller yang baru dimuat
        Object controller = fxmlLoader.getController();

        // PENTING: Menangani penutupan jendela (stopSync)
        if (controller instanceof AsistenController) {
            stage.setOnCloseRequest(e -> ((AsistenController) controller).stopSync());
        } else if (controller instanceof MahasiswaController) {
            stage.setOnCloseRequest(e -> ((MahasiswaController) controller).stopSync());
        }
        // Kita juga harus menangani stopSync di MahasiswaRegisterController
        // Tapi kita biarkan dulu untuk saat ini

        // Menerapkan CSS
        scene.getStylesheets().add(BOOTSTRAP_CSS);
        stage.setTitle(title);
        stage.setScene(scene);
        stage.show();
    }

}