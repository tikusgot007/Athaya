# Athaya Printer

Aplikasi Android native (Kotlin + Jetpack Compose) untuk mencetak **Nota** dan
**Alamat** ke thermal printer via ESC/POS raster image. Tanpa database,
tanpa backend, tanpa login — semua data hanya di memory selama
input → preview → print.

## Status

**Phase 1 selesai**: struktur project, package layout, data model,
`PrinterProfile` abstraction, dan UI dasar (Home, Nota, Address,
Printer Settings).

**Phase 2 selesai**: `NotaRenderer` dan `AddressRenderer` (Bitmap/Canvas/Paint,
bukan Compose capture), `mmToPx()`, rotasi nota 90°, template test 58mm
(`NotaTemplateSpec.TEST_58MM`, `AddressTemplateSpec.TEST_58MM`) terpisah dari
template production, preview nyata di NotaScreen & AddressScreen, dan unit
test (`MmToPxTest`, `NotaRendererTest`, `AddressRendererTest`).

**Phase 4 selesai** (+ review kecil): validasi renderer (dimensi bitmap
aktual, rotasi 100×60→60×100mm, tidak ada distorsi/kehilangan pixel,
printable width tidak pernah melebihi `printableDots`), lalu
`MonochromeConverter` (threshold configurable), `EscPosCommands`, dan
`EscPosEncoder` (bit-packing raster `GS v 0` lewat `RasterImageEncoder`
yang pluggable — GS v 0 bukan satu-satunya raster command yang mungkin
nanti — feed, cut kondisional lewat `PrinterProfile.supportsCut`).
`printableDots` selalu dikonfigurasi terpisah dari `paperWidthMm`, tidak
pernah dihitung otomatis dari `paperWidthMm × dpi`.

**Phase 5 selesai**: `BluetoothPrinter` (Bluetooth Classic/RFCOMM, SPP UUID
`00001101-0000-1000-8000-00805F9B34FB`, tanpa BLE) sebagai implementasi
konkret pertama dari `PrinterManager`. `BluetoothPrinter` hanya transport —
ESC/POS tetap sepenuhnya dibentuk di `EscPosEncoder`. Koneksi diserialisasi
lewat satu `Mutex` (tidak ada dua socket/print job yang tabrakan), semua I/O
lewat `Dispatchers.IO` (UI tidak pernah blocking). Permission `BLUETOOTH_CONNECT`
(Android 12+) diminta saat fitur benar-benar dipakai, bukan saat start-up;
`BLUETOOTH_SCAN`/`ACCESS_FINE_LOCATION` sengaja tidak diminta karena MVP
hanya memakai paired/bonded devices (tidak ada active discovery).
`PrinterSettingsScreen` sekarang punya daftar paired devices, Connect/
Disconnect, status koneksi reaktif (`StateFlow`), dan Test Print yang
benar-benar lewat pipeline Bitmap → `EscPosEncoder` → `BluetoothPrinter`
(`TestPrintBitmap`), bukan raw string.

Lihat bagian **Testing Phase 5** dan **Hardware Test Plan** di bawah untuk
apa yang sudah/tidak bisa diverifikasi di sandbox ini.

> **Catatan lingkungan build**: di sandbox development ini, `dl.google.com`
> (host Maven Google yang menyediakan Android Gradle Plugin & seluruh
> artifact AndroidX) diblokir oleh kebijakan jaringan sandbox, sehingga
> `gradle`/`gradlew` tidak bisa menyelesaikan resolusi dependency di sini.
> Build & unit test (termasuk Robolectric-based renderer test) perlu
> dijalankan di Android Studio atau CI dengan akses jaringan normal.

## Membuka project

Buka folder ini di Android Studio (Koala atau lebih baru) — Android Studio
akan membuat `gradlew`/`gradle-wrapper.jar` secara otomatis saat sync
pertama, atau jalankan `gradle wrapper` jika Gradle sudah terpasang lokal.

- Kotlin, AGP 8.5.2, compileSdk 34, minSdk 26.
- UI: Jetpack Compose (Material3) + Navigation Compose.

## Struktur package

```
com.athaya.printer/
  ui/         Layar Compose (Home, Nota, Address, Printer Settings)
  model/      Data class murni (NotaData, NotaItem, AddressData)
  renderer/   PrintRenderer<T> + mmToPx() — sumber kebenaran ukuran fisik
  printer/    PrinterProfile + PrinterManager + BluetoothPrinter +
              BluetoothTransport (seam for testing) + shared exceptions
  escpos/     MonochromeConverter + EscPosCommands + EscPosEncoder
  settings/   PrinterProfileStore (in-memory, tanpa DB)
```

## Prinsip desain

- **Bitmap, bukan PDF**, sebagai jalur printing utama.
- **PrinterProfile configurable** — tidak ada angka 58/80 mm yang di-hardcode
  di renderer atau template. Ganti printer = ganti profile.
- Printer development sekarang: **58 mm**. Target production:
  **Iware XS-80, 80 mm**.
- Nota production **100×60 mm** dirotasi 90° menjadi **60×100 mm** sebelum
  dikirim ke printer (tidak pernah di-scale otomatis ke 58 mm).
- Preview dan hasil cetak memakai renderer yang sama persis.
- Bluetooth Classic/RFCOMM saja untuk MVP (bukan BLE), hanya paired devices
  (tidak ada active discovery), permission diminta saat dipakai bukan saat
  start-up, satu `Mutex` menyerialisasi semua I/O per koneksi.

## Testing Phase 5 (Bluetooth)

`BluetoothPrinter` tidak langsung memanggil `android.bluetooth.*` — semua
panggilan itu lewat interface `BluetoothTransport`, dan
`AndroidBluetoothTransport` adalah satu-satunya implementasi nyata.
`BluetoothPrinterTest` memakai `FakeBluetoothTransport` (in-memory, murni
Kotlin) untuk menguji:

- `printBitmap` saat disconnected → melempar `NotConnected` dengan pesan jelas
- Transisi state `Disconnected` → `Connected` saat connect() sukses
- `connect()` gagal (permission ditolak / device belum paired) → `Error`
  dengan pesan human-readable, tanpa crash
- Double connect **tidak** membuka socket kedua
- `disconnect()` menutup socket & kembali ke `Disconnected`
- `cut()` **tidak** mengirim apa pun ketika `profile.supportsCut == false`,
  dan mengirim `GS V 0` ketika `true`
- Byte yang dikirim `printBitmap()` persis sama dengan
  `EscPosEncoder.encode(bitmap, profile)` — bukti `profile` diteruskan benar
- Dua `printBitmap()` yang dipanggil bersamaan tidak pernah saling
  interleave di output stream (dibuktikan lewat coroutine `async`+`Mutex`)
- Kegagalan write (`IOException`) → `WriteFailed` + socket ditutup

**Yang TIDAK bisa diverifikasi di sandbox ini** (butuh hardware sungguhan):
`AndroidBluetoothTransport` sendiri (BluetoothAdapter/BluetoothSocket nyata),
dan apakah printer fisik 58mm/80mm benar-benar mencetak dengan hasil yang
benar. Seperti Phase 1–4, `gradle`/`gradlew` juga tidak bisa jalan di
sandbox ini karena `dl.google.com` diblokir — semua di atas adalah hasil
review manual kode + Robolectric test yang ditulis (bukan hasil run yang
lolos); jalankan `./gradlew testDebugUnitTest` di Android Studio/CI dengan
jaringan normal untuk verifikasi sesungguhnya.

## Hardware Test Plan (Phase 5)

Belum dijalankan — dokumentasi langkah manual untuk saat ada device + printer fisik:

1. Pair printer 58mm dari Android Settings (bukan dari dalam app).
2. Buka aplikasi Athaya Printer.
3. Buka Pengaturan Printer → beri izin Bluetooth saat diminta (Android 12+).
4. Tekan "Muat daftar printer (paired)", pilih printer 58mm dari daftar.
5. Tekan Connect, tunggu status berubah jadi "Terhubung ke ...".
6. Tekan Test Print, verifikasi struk test keluar dari printer.
7. Ganti ke NotaScreen, isi data nota, preview dengan template Test 58mm
   (56×100mm setelah rotasi), lalu cetak — verifikasi lewat PrinterSettingsScreen
   yang sudah connect (alur cetak nota nyata menyusul saat CETAK di
   NotaScreen/AddressScreen di-wire ke PrinterManager).
8. Ulangi untuk AddressScreen dengan template Test 58mm.
9. Verifikasi hasil fisik:
   - tidak terpotong di sisi kanan/kiri
   - tidak bergeser (margin konsisten)
   - raster tidak terdistorsi (garis lurus tetap lurus, tidak bergelombang)
   - feed jarak wajar sebelum robek
   - cut hanya terjadi jika `supportsCut == true` untuk profile yang dipakai
10. Setelah stabil di 58mm, ulangi seluruh langkah dengan
    `PrinterProfile.productionIwareXs80()` dan template production
    (nota 100×60mm → rotate → 60×100mm; address 80×100mm) begitu
    printer Iware XS-80 tersedia.

Jangan anggap hardware test ini berhasil sampai benar-benar dilakukan di
device + printer fisik — semua klaim "berhasil" pada dokumen ini sejauh ini
terbatas pada kode dan test yang dijalankan tanpa hardware.
