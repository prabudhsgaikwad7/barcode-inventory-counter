# Barcode Inventory Counter (Android)

Kotlin + Jetpack Compose inventory counting app with barcode scan, mock API lookup, local Room storage, batch submit, and CSV export.

## Features

- Scan barcodes (CameraX + ML Kit) or type a barcode manually
- Mock product API (name + expected quantity) via OkHttp interceptor — no backend setup
- Enter actual quantity and expiry date (`YYYY-MM-DD`)
- List rows highlight quantity differences (match / over / short)
- Duplicate pending barcodes merge (actual qty summed, earliest expiry kept)
- Room database for offline counts
- Submit all pending counts through mock submit API
- Pending / Submitted / All tabs
- Export all records to CSV (share sheet)
- Expired products blocked on save; expired submitted rows flagged in the list
- Unit tests under `app/src/test`

## Mock catalog barcodes

| Barcode         | Product                 | Expected qty |
|-----------------|-------------------------|--------------|
| 1234567890123   | Organic Milk 1L         | 48           |
| 9876543210987   | Whole Wheat Bread       | 24           |
| 5556667778889   | Canned Tomatoes         | 120          |
| 1112223334445   | Hand Sanitizer 500ml    | 60           |

## Run in Android Studio

1. **Install Android Studio** (Ladybug or newer recommended) with:
   - Android SDK 35
   - JDK 17 (Embedded JDK in Android Studio is fine)

2. **Open the project**
   - Android Studio → **File → Open**
   - Select this folder: `Barcode Inventory Counter`
   - Wait for Gradle sync to finish (first sync downloads dependencies).

3. **Device**
   - **Physical phone (recommended for scanning):** Enable Developer Options → USB debugging, connect USB, allow debugging.
   - **Emulator:** Device Manager → Create Virtual Device → Pixel class device → API 34/35 system image.

4. **Run**
   - Select the `app` run configuration and your device/emulator.
   - Click **Run** (green triangle) or press `Shift+F10`.
   - Grant **Camera** when prompted (needed for scan).

5. **Unit tests**
   - Right-click `app/src/test/java` → **Run 'Tests in …'**
   - Or terminal (after Gradle wrapper is generated): `./gradlew test`

## Gradle wrapper (if missing)

If Android Studio asks for Gradle wrapper files, use **File → Sync Project with Gradle Files**.  
From a machine with Gradle installed you can also run in the project root:

```bash
gradle wrapper
```

## Project structure

```
app/src/main/java/com/inventory/barcodecounter/
  data/          Room + Retrofit mock API
  domain/        Models + merge policy
  ui/            Compose screens + ViewModel
  util/          CSV export
```

## Notes

- Submit marks all **pending** rows as **submitted** locally after the mock API succeeds.
- Re-counting a barcode that is already **submitted** is blocked until you clear app data or uninstall (one row per barcode in the database).
