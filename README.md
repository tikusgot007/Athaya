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

**Phase 4 selesai**: validasi renderer (dimensi bitmap aktual, rotasi
100×60→60×100mm, tidak ada distorsi/kehilangan pixel, printable width
tidak pernah melebihi `printableDots`), lalu `MonochromeConverter`
(threshold configurable), `EscPosCommands`, dan `EscPosEncoder`
(bit-packing raster `GS v 0`, feed, cut kondisional lewat
`PrinterProfile.supportsCut`). Bluetooth (Phase 5) menyusul.

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
  printer/    PrinterProfile + PrinterManager abstraction + shared exceptions
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
