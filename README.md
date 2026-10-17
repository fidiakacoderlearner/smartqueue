# SmartQueue

SmartQueue adalah aplikasi desktop berbasis JavaFX untuk mengelola sistem antrian asistensi. Aplikasi ini memungkinkan mahasiswa untuk mengantri asistensi secara real-time, serta asisten untuk mengelola antrian, slot kelompok, informasi asistensi, dan fitur-fitur pendukung lainnya.

## 📌 Informasi Proyek

Proyek ini merupakan **Tugas Proyek Kelompok** pada Mata Kuliah **Pemrograman Berbasis Objek (PBO)**, Semester 3.  

Dibuat untuk memenuhi tugas kelompok mata kuliah Pemrograman Berbasis Objek (PBO) Tahun Akademik 2025/2026, Kelas TI-B, Program Studi Teknik Informatika, Universitas Riau.

Aplikasi ini dibangun dengan menerapkan konsep-konsep Pemrograman Berbasis Objek (OOP) seperti *Encapsulation*, *Inheritance*, *Polymorphism*, dan *Abstraction*, serta memanfaatkan integrasi dengan Firebase Realtime Database untuk melakukan sinkronisasi data secara *real-time*.

## ✨ Fitur

- **Pemilihan Akses** – Memilih peran (role) sebagai Mahasiswa atau Asisten saat pertama kali membuka aplikasi.
- **Sistem Antrian Real-Time** – Menampilkan status antrian secara terpisah (Menunggu, Sedang Dilayani, dan Riwayat Antrian).
- **Autentikasi Mahasiswa** – Tersedia fitur login dan registrasi akun mahasiswa.
- **Panel Asisten** – Asisten dapat mengelola antrian, mengatur informasi asistensi, membuka/menutup antrian, serta mengatur estimasi waktu tunggu.
- **Kelola Mahasiswa** – Fitur pengelolaan data mahasiswa oleh pihak asisten.
- **Sistem Chat** – Memfasilitasi komunikasi antara mahasiswa dan asisten terkait proses asistensi.
- **Manajemen Slot Kelompok** – Pengaturan slot untuk pelaksanaan asistensi kelompok.
- **Pengaturan Admin** – Konfigurasi pertanyaan keamanan (recovery question) dan pengaturan lainnya.
- **Sinkronisasi Real-Time** – Seluruh data antrian dan informasi terpusat dan tersinkronisasi secara real-time melalui Firebase Realtime Database.

## 🛠️ Teknologi yang Digunakan

- [Java 21](https://www.oracle.com/java/technologies/downloads/#java21) – Bahasa pemrograman utama
- [JavaFX 21](https://openjfx.io/) – Framework untuk membangun antarmuka GUI berbasis desktop
- [Maven](https://maven.apache.org/) – Build automation dan dependency management
- [Firebase Admin SDK 9.2.0](https://firebase.google.com/docs/admin/setup) – Integrasi dengan Firebase Realtime Database
- [Gson 2.9.0](https://github.com/google/gson) – Library untuk parsing dan serialisasi JSON
- [BootstrapFX 0.4.0](https://github.com/kordamp/bootstrapfx) – Styling UI menggunakan Bootstrap
- [SLF4J 2.0.7](https://www.slf4j.org/) – Logging sederhana untuk aplikasi

## 📁 Struktur Proyek

```text
SmartQueue/
├── src/
│   ├── main/
│   │   ├── java/com/smartqueue/
│   │   │   ├── controller/   # Controller JavaFX (logika aplikasi)
│   │   │   ├── model/        # Model/Entitas (Antrian, Mahasiswa, ChatMessage, dll.)
│   │   │   ├── Launcher.java # Main class untuk JavaFX non-modular
│   │   │   └── SmartQueueApp.java
│   │   └── resources/
│   │       └── com/smartqueue/  # File FXML, CSS, aset, dan konfigurasi
├── .mvn/wrapper/            # Maven Wrapper
├── mvnw / mvnw.cmd          # Script Maven Wrapper
├── pom.xml                  # Konfigurasi Maven
└── README.md                # Dokumentasi proyek
```

## 📋 Prasyarat

Sebelum menjalankan proyek ini, pastikan Anda telah menginstal perangkat lunak berikut:

- [Java Development Kit (JDK) 21](https://www.oracle.com/java/technologies/downloads/#java21)
- [Git](https://git-scm.com/) (opsional, untuk cloning repository)

## 🚀 Cara Menjalankan

Proyek ini menggunakan **Maven Wrapper**, sehingga tidak diperlukan instalasi Maven terpisah.

### Menjalankan dalam Mode Development

**Linux/macOS:**

```bash
./mvnw clean javafx:run
```

**Windows (Command Prompt/PowerShell):**

```cmd
mvnw.cmd clean javafx:run
```

## 📦 Cara Build Menjadi JAR

Untuk membuild aplikasi menjadi file `.jar` yang dapat dijalankan secara mandiri:

```bash
./mvnw clean package
```

File JAR hasil build akan tersimpan pada folder `target/`.

## 🔐 Konfigurasi Firebase

Aplikasi ini menggunakan **Firebase Realtime Database** untuk sinkronisasi data secara real-time. Untuk dapat menjalankan proyek ini secara lokal, diperlukan file kredensial Firebase sebagai berikut:

1. Buat project Firebase melalui [Firebase Console](https://console.firebase.google.com/)
2. Generate Service Account Key dalam format `.json`
3. Simpan file tersebut dengan nama `serviceAccountKey.json`
4. Letakkan file tersebut di direktori `src/main/resources/com/smartqueue/serviceAccountKey.json`

> **Catatan Keamanan:** File `serviceAccountKey.json` **tidak di-commit** ke repository ini untuk menjaga keamanan kredensial. Jika Anda ingin menjalankan aplikasi ini, Anda perlu menyediakan file kredensial Firebase milik Anda sendiri.

## 👥 Tim Proyek Kelompok

Anggota Kelompok - Kelas TI-B, Teknik Informatika, Universitas Riau:

1. Afdhal Fidi Ansori – 2407126812
2. Asifa Miranti – 2407110832
3. Muhammad Firmansyah – 2407127220
4. Naufa Alya Nafisa – 2407126391
5. Wan Nabil Muzakhy – 2407127390

## 📝 Catatan

Proyek ini dikembangkan sebagai bagian dari pembelajaran Mata Kuliah **Pemrograman Berbasis Objek (PBO)** untuk menerapkan prinsip-prinsip OOP dalam pengembangan aplikasi desktop berbasis GUI.