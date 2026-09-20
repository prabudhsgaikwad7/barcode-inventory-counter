# Android Barcode Inventory Counter - Project Summary Report

---

## Section 1: Summary of Project

The **Android Barcode Inventory Counter** is a production-ready mobile application built using **Kotlin** and **Jetpack Compose**. It simplifies stocktaking by allowing users to scan barcodes, fetch product details from a mock API, enter actual quantities and expiry dates, highlight stock variances, store data locally offline, and submit counts via batch API.

### Key Features & Technical Highlights:
- **Barcode Scanning & Manual Entry**: Uses CameraX and Google ML Kit for fast barcode scanning, with manual text entry fallback.
- **Mock Product API**: Built with Retrofit and OkHttp Interceptor to simulate real backend catalog lookup without server dependency.
- **Expiry Date Management & DatePicker**: Integrated Jetpack Compose `DatePickerDialog` and quick preset chips (`+30d`, `+90d`, `+1y`, `Today`) for quick date selection.
- **Dynamic Difference Badging**: Visually displays stock variance as `Matched (0)`, `Over (+X)`, or `Short (-X)` using Material3 `OutlinedCard` components.
- **Smart Duplicate Merging**: Automatically merges duplicate pending counts for the same barcode (summing actual quantities and retaining the earliest expiry date). Blocks edits on submitted rows.
- **Offline Persistence & Submission**: Powered by Room SQLite database with flow observation, batch submission, and CSV export.
- **Validation & Testing**: Prevents saving expired items and includes unit test coverage across domain logic, repository operations, and ViewModel state.

---

## Section 2: GitHub Link

**GitHub Repository URL:**  
[https://github.com/prabudhsgaikwad7/barcode-inventory-counter](https://github.com/prabudhsgaikwad7/barcode-inventory-counter)

---

## Section 3: Summary of AI_USAGE.md & Link

**AI Usage Document Link:**  
[https://github.com/prabudhsgaikwad7/barcode-inventory-counter/blob/main/AI_USAGE.md](https://github.com/prabudhsgaikwad7/barcode-inventory-counter/blob/main/AI_USAGE.md)

### Summary of AI Collaboration:
- **AI Tools Used**: Google Antigravity (Gemini 3.6 Flash), Android Studio IDE, Git.
- **Key Prompts**: 5 structured prompts covering initial architecture, DB optimization, unlisted barcode & DatePicker support, unit test expansion, and GitHub deployment.
- **AI-Generated Components**: Room DAO indexing queries, Jetpack Compose HomeScreen & item cards, and Repository & ViewModel unit test suites.
- **AI Mistake #1 Identified**: Full-table in-memory DB filtering during count save operations.  
  *Fix*: Replaced with indexed SQL `findByBarcode` query in Room DAO for $O(1)$ efficiency.
- **AI Mistake #2 Identified**: Form lockout when scanning/entering unlisted barcodes not in the mock catalog.  
  *Fix*: Enabled fallback editable text fields for product name and expected quantity on catalog lookup 404 responses.

---

## Section 4: Video and Screenshots

> *Note: Place your video link and screenshots below before sharing.*

### 1. Project Demonstration Video
`[ Insert Video Demo Link Here (e.g., Google Drive / Loom / YouTube) ]`

### 2. Application Screenshots
- `[ Insert Screenshot 1: Home Screen & Barcode Scanning ]`
- `[ Insert Screenshot 2: DatePicker & Expiry Selection ]`
- `[ Insert Screenshot 3: Matched / Over / Short Badges & Expired Alerts ]`
- `[ Insert Screenshot 4: Pending / Submitted Records Tabs ]`

### 3. Unit Test Execution Screenshots
- `[ Insert Screenshot 5: Android Studio Unit Test Results ]`
