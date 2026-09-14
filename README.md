# Athaya Printer

Aplikasi Android native (Kotlin + Jetpack Compose) untuk mencetak **Nota** dan
**Alamat** ke thermal printer via ESC/POS raster image. Tanpa database,
tanpa backend, tanpa login — semua data hanya di memory selama
input → preview → print.

## Status

**Phase 1 selesai**: struktur project, package layout, data model,
`PrinterProfile` abstraction, dan UI dasar (Home, Nota, Address,
Printer Settings) sudah ada. Rendering bitmap (Phase 2), rotasi nota
(Phase 3), ESC/POS encoder (Phase 4), dan koneksi Bluetooth (Phase 5)
menyusul secara bertahap.

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
  printer/    PrinterProfile + PrinterManager abstraction
  escpos/     Encoder ESC/POS (Phase 4)
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
