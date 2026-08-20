# Price Intelligence

Price Intelligence is a Kotlin Multiplatform app for comparing a shop price with current Amazon and Flipkart prices. The same Room database, business rules, price parser, inventory screens, and Dashboard screens run on Android and iPhone.

## Main features

- Search by product name, barcode, Amazon link, or Flipkart link.
- Scan product barcodes on Android and iPhone.
- Save and edit a local product inventory.
- Compare the shop price with live Amazon and Flipkart prices.
- Keep the latest successful online prices available when the phone is offline.
- Keep the newest 60 successful checks per retailer and show price movement and the lowest saved price.
- Mark prices clearly as live or saved.
- Back up inventory and price history to a portable JSON file and merge it safely on restore.
- Delete inventory products with a four-second Undo option.

## Data and privacy

- Inventory data stays in the app's local Room database.
- Camera access is requested only when the user chooses to scan a barcode.
- The app downloads only the Amazon and Flipkart product pages saved by the user.
- Backup restore adds missing products and their saved history. It does not delete or replace existing products.
- Retailer links are validated and upgraded to HTTPS before use.

## Project folders

- `shared` contains the shared database, repository, validation, price parsing, ViewModels, and Compose UI.
- `androidApp` contains the Android entry point and Android app settings.
- `iosApp` contains the iPhone entry point and Xcode project.
- `shared/schemas` contains exported Room database schemas needed for safe upgrades.
- `.github/workflows` contains Android quality checks and the unsigned iPhone IPA build.

The completed original Android application is stored separately as a read-only reference. It must not be modified.

## Local Android check

Run this from the project folder:

```powershell
.\gradlew.bat :shared:testAndroidHostTest :androidApp:lintDebug :androidApp:assembleDebug :androidApp:assembleRelease
```

## Physical iPhone check

1. Push the completed milestone to GitHub.
2. Open the repository's Actions page.
3. Run **Build Unsigned iOS IPA**.
4. Download the `unsigned-ipa` artifact when the workflow finishes.
5. Install the IPA on the physical iPhone with Sideloadly.
6. Test search, barcode scanning, live price checks, inventory editing, backup, restore, delete, and Undo.

The GitHub workflow runs the shared iPhone tests before building the IPA. This prevents creating an IPA when shared iPhone code does not compile or a shared test fails.

The workflow also caches the Kotlin Native compiler files. A manual milestone run from `master` creates the reusable cache; later milestone runs can restore it instead of downloading the Apple compiler dependencies again.

For a deliberate large milestone, pushing a tag whose name starts with `iphone-checkpoint-` starts the same workflow automatically. Normal code pushes do not create an IPA.
