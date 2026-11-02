# SubAI Extractor & Merger — Dokumentasi Batch Subtitle Translation Queue

Aplikasi penerjemah, ekstraktor, dan penggabung subtitle Android (`.srt`, `.vtt`, `.ass`, `.ssa`) terintegrasi dengan AI (Gemini Web Cookie / OpenAI-Compatible API) dan dilengkapi fitur **Batch Subtitle Translation Queue**.

---

## 1. Cara Menggunakan Fitur Batch Translation

1. Buka aplikasi **SubAI Extractor & Merger** dan pilih tab **Batch** (Tab ke-5 pada bilah navigasi bawah/samping).
2. Klik tombol **Pilih Banyak File Subtitle** untuk memilih beberapa file subtitle sekaligus (`.srt`, `.vtt`, `.ass`, atau `.ssa`), atau klik **Muat 4 File Sampel Episode** untuk memuat 4 file contoh siap uji (`episode_01.srt`, `episode_02.srt`, `episode_03.vtt`, `episode_04.ass`).
3. Aplikasi melakukan **praperiksa (pre-flight check)** otomatis terhadap file yang tidak didukung, kosong/rusak, atau duplikat sebelum dimasukkan ke antrean.
4. Pastikan pengaturan bahasa target, model AI, glosarium, dan batas maksimum retry sudah sesuai.
5. Pilih folder tujuan pada bagian **Output Folder Khusus Batch** (atau gunakan folder default `Translated_Subtitles`).
6. Klik **Start Batch** untuk memulai pemrosesan berurutan satu per satu.

---

## 2. Cara Memilih Output Folder & Penanganan Konflik Nama File

- Klik tombol **Choose Output Folder** untuk memilih direktori tujuan khusus batch melalui pemilih folder sistem Android (*Storage Access Framework / OpenDocumentTree*), atau gunakan folder default aplikasi `Translated_Subtitles/`.
- Aplikasi memvalidasi izin tulis (*write access*) pada folder yang dipilih sebelum antrean dimulai.
- **Penyimpanan Atomik Aman (5 Langkah)**:
  1. Menerjemahkan entri subtitle.
  2. Memvalidasi kelengkapan entri dan struktur format subtitle.
  3. Menulis hasil ke file sementara (`.tmp`).
  4. Memverifikasi file sementara berhasil ditulis dan tidak kosong.
  5. Memindahkan/mengubah nama file `.tmp` menjadi file final hanya setelah validasi lulus.
- **Kebijakan Konflik Nama File (`FileConflictPolicy`)**:
  - **Buat Nama Alternatif / `RENAME_WITH_SUFFIX` (Default Aman)**: Menyimpan dengan nama seperti `episode_01 (1).srt` tanpa menimpa file yang sudah ada.
  - **Lewati Jika Sudah Ada / `SKIP_IF_EXISTS`**: Melewati penulisan jika file dengan nama yang sama sudah ada.
  - **Timpa File / `OVERWRITE`**: Mengganti file lama hanya setelah hasil baru lolos validasi penuh.

---

## 3. Cara Mengatur Urutan Antrean

- Sebelum antrean dijalankan, gunakan tombol **Pindah ke Atas (`↑`)**, **Pindah ke Bawah (`↓`)**, atau **Hapus (`🗑`)** pada setiap kartu file untuk mengatur urutan pemrosesan.
- Pemrosesan batch berjalan **100% berurutan satu per satu (sequential)** mulai dari urutan `#1` hingga terakhir, tidak pernah dijalankan secara paralel.

---

## 4. Aturan Validasi Kelengkapan (`BatchTranslationValidator`)

Setiap file wajib melewati pemeriksaan struktur dan kelengkapan sebelum statusnya diizinkan berubah menjadi `Completed`:

1. **Kelengkapan Entri (`missingCueIds`)**: Seluruh ID entri sumber (`1..N`) wajib ada pada hasil terjemahan.
2. **Jumlah Entri**: Jumlah dialog pada file hasil merge harus sama persis dengan jumlah dialog pada file sumber.
3. **Pemeriksaan Teks Kosong atau Terpotong (`emptyOrTruncatedCueIds`)**: Menolak entri kosong, kurung tag `{...}` yang tidak tertutup, atau indikator teks terpotong.
4. **Deteksi Kebocoran Bahasa Sumber (`untranslatedCueIds`)**: Mendeteksi kalimat yang tertinggal dalam bahasa sumber tanpa diterjemahkan (dengan tetap mentoleransi nama diri pendek, angka, tanda baca, dan interjeksi universal seperti `OK`, `Ah!`).
5. **Validitas Timestamp (`invalidTimestampCueIds`)**: Memastikan timecode setiap entri tidak berubah dan sesuai format standar (`.srt` `HH:MM:SS,mmm`, `.vtt` `HH:MM:SS.mmm`, `.ass`/`.ssa` `H:MM:SS.cc`).
6. **Urutan Sekuensial (`outOfOrderCueIds`)**: Memastikan nomor urut dan blok subtitle tetap berurutan.
7. **Keutuhan Header & Tag Format (`structureErrors`)**: Memastikan header `WEBVTT` pada `.vtt`, serta bagian `[Script Info]`, `[Events]`, dan tag override awal (`{\an8}`, `{\pos(...)}`) pada `.ass`/`.ssa` tetap utuh.

---

## 5. Kebijakan Retry di Akhir Antrean (`End-of-Queue Retry`)

- Jika sebuah file gagal validasi atau mengalami kegagalan API/jaringan, hasil parsial **tidak disimpan** sebagai file final.
- Pekerjaan tersebut **tidak diulang langsung di tempat**, melainkan diberi status `Retry Queued` dan **dipindahkan ke bagian paling akhir antrean** (misalnya `episode_02.srt [RETRY #1]`), sehingga file berikutnya yang masih menunggu (`episode_03.srt`, `episode_04.srt`) tetap diproses terlebih dahulu.
- **Perbaikan Terarah (*Targeted Repair*)**: Pada saat giliran retry tiba, sistem selalu membaca struktur asli dari file sumber (`rawSourceContent`), mempertahankan entri yang sudah lolos validasi individual, dan hanya menerjemahkan ulang entri yang hilang/gagal sebelum memvalidasi ulang keseluruhan file.
- **Batas Maksimum Retry**: Dapat diatur (default `5` kali). Jika sebuah file masih gagal setelah mencapai batas maksimum retry, statusnya menjadi `Needs Attention` dan sistem tidak akan mengalami *infinite loop*.

---

## 6. Arti Setiap Status Pekerjaan (`BatchJobStatus`)

| Status | Label Indonesia | Keterangan |
|---|---|---|
| `Queued` | Menunggu | Menunggu giliran diproses sesuai nomor antrean |
| `Processing` | Sedang Diterjemahkan | Sedang diterjemahkan oleh penyedia AI aktif |
| `Validating` | Validasi Kelengkapan | Sedang diperiksa kelengkapan entri, timestamp, dan struktur file |
| `Retry Queued` | Menunggu Retry di Akhir Antrean | Gagal pada percobaan sebelumnya dan telah dipindahkan ke urutan paling akhir antrean (`[RETRY #N]`) |
| `Completed` | Selesai & Lolos Validasi | Lulus seluruh pemeriksaan validasi dan file final telah disimpan dengan aman |
| `Needs Attention` | Perlu Perhatian | Mencapai batas maksimum retry dan memerlukan tindakan pengguna (`Retry Failed Jobs` / `View Error Details`) |
| `Failed` | Gagal Permanen | File tidak dapat diproses karena kesalahan permanen |
| `Cancelled` | Dibatalkan | Dibatalkan oleh pengguna sebelum selesai |

---

## 7. Cara Melakukan Pause, Resume, dan Cancel

- **Pause Queue**: Menjeda antrean dengan aman tanpa merusak pekerjaan yang sedang berjalan atau file yang sudah selesai.
- **Resume Queue**: Melanjutkan antrean dari pekerjaan `Queued` atau `Retry Queued` berikutnya tanpa menerjemahkan ulang file yang sudah berstatus `Completed`.
- **Cancel Batch**: Menghentikan antrean dan menandai sisa pekerjaan yang belum selesai menjadi `Cancelled`, sementara file yang sudah `Completed` dan file sumber asli tetap aman.
- **Pemulihan Otomatis (*Crash/Restart Recovery*)**: Status antrean disimpan secara persisten di penyimpanan lokal. Jika aplikasi tertutup dan dibuka kembali, file `.tmp` yang belum selesai dibersihkan dan pekerjaan yang terputus dikembalikan ke antrean untuk dilanjutkan.

---

## 8. Cara Kerja Notifikasi ke Ponsel (`BatchNotificationHelper`)

- Menggunakan sistem notifikasi native Android (`NotificationChannel` + `NotificationCompat` + izin runtime `POST_NOTIFICATIONS` pada Android 13+).
- Notifikasi **hanya dikirim satu kali ketika seluruh antrean benar-benar selesai** (tidak ada lagi pekerjaan berstatus `Queued`, `Processing`, `Validating`, atau `Retry Queued`):
  1. **Semua Berhasil (`ALL_SUCCEEDED`)**:
     - Judul: `"Batch Translation Completed"`
     - Pesan: `"Semua file subtitle berhasil diterjemahkan dan lolos validasi."`
  2. **Selesai dengan Masalah (`FINISHED_WITH_ISSUES`)**:
     - Judul: `"Batch Translation Finished with Issues"`
     - Pesan: `"Antrean telah selesai diproses, tetapi beberapa file memerlukan perhatian."`
  3. **Dibatalkan (`CANCELLED`)**:
     - Judul: `"Batch Translation Cancelled"`
     - Pesan: `"Proses batch dibatalkan sebelum seluruh file selesai diterjemahkan."`

---

## 9. Keterbatasan Teknis & Catatan Platform

- Pada Android 13+ (API 33+), pengguna perlu menyetujui izin notifikasi (`POST_NOTIFICATIONS`) saat mengaktifkan sakelar notifikasi agar notifikasi muncul di bilah status ponsel.
- Pemilih folder sistem (`OpenDocumentTree`) menyimpan izin URI secara persisten (`takePersistableUriPermission`); jika folder dihapus di luar aplikasi, indikator akses tulis akan memberi peringatan sebelum batch dimulai.

---

## 10. Cara Menjalankan Pengujian Otomatis

Jalankan seluruh pengujian unit dan Robolectric (termasuk **Test A s/d Test O** pada `BatchTranslationQueueTest` dan `AppUpdateCheckerTest`) melalui perintah Gradle berikut:

```bash
gradle :app:testDebugUnitTest
```

---

## 11. Sistem Versioning Otomatis, GitHub Releases, & Cek Pembaruan di Aplikasi

- **Konfigurasi Semantic Versioning (`version.properties`)**:
  - Versi aplikasi dikelola secara terpusat di file `version.properties` (`VERSION_MAJOR`, `VERSION_MINOR`, `VERSION_PATCH`, dan `VERSION_CODE`), dimulai dari **v1.1.0 (Build 2)**.
  - Setiap penambahan fitur baru menaikkan versi *MINOR* (mis. `v1.1.0` -> `v1.2.0`), dan setiap perbaikan bug menaikkan versi *PATCH* (mis. `v1.1.0` -> `v1.1.1`) beserta `VERSION_CODE`.
- **Badge Versi Asli & Pemeriksa Pembaruan di Pengaturan (`ThemeSettingsSheet`)**:
  - Buka ikon **Pengaturan & Tema** di pojok kanan atas aplikasi untuk melihat badge versi aktif (`v1.1.0 (Build 2)`).
  - Klik **Cek Versi Terbaru** untuk memeriksa rilis terbaru secara langsung dari GitHub Releases (`EkoSaputro14/subai-extractor-merger`).
  - Jika tersedia versi yang lebih baru, pengguna dapat langsung menekan tombol **Download APK Terbaru** untuk mengunduh file `.apk` siap pasang melalui Android `DownloadManager`.

