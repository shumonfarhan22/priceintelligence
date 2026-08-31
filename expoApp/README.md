# Price Intelligence V2

This folder is the isolated Expo/React Native replacement for the current Kotlin Multiplatform app.
It deliberately uses separate Android and iOS identifiers while both implementations are being
tested. The existing app remains the production baseline until the v2 parity gates pass.

## Foundation contract

- Offline data is stored in `price-intelligence-v2.db` with explicit schema migrations.
- The app imports the current Price Intelligence backup formats 1 and 2.
- Import merges missing products and reports duplicates and invalid rows.
- Retailer URLs are restricted to supported Amazon and Flipkart hosts.
- Price history is bounded to the newest 60 observations per product and retailer.
- V2 can export the same portable backup format for migration verification.

## Inventory milestone

- Native Android/iPhone text fields with reliable keyboard dismissal and iPhone input accessory controls.
- Product-name system paste plus an explicit clipboard-backed Paste action.
- Add, edit, validation, grouped search, barcode/link search, and local pull-to-reset.
- Native camera barcode scanning in search and the product editor.
- Individual and multi-selection delete with a five-second Undo window.
- A price calculator whose result is preserved while switching between purchase cost and selling price.
- Haptics are limited to completed scans and bottom notification banners.

The Room `inventory.db` file is never opened or modified. Migration happens through a backup made
by the current app so that Room metadata and the installed app remain safe.

## Checks

From this folder:

```powershell
pnpm install
pnpm typecheck
pnpm test
pnpm doctor
```

Android and iPhone bundles can be checked without creating native projects:

```powershell
pnpm exec expo export --platform android --output-dir dist/android-check
pnpm exec expo export --platform ios --output-dir dist/ios-check
```

The responsive UI can also be inspected against an isolated browser database:

```powershell
pnpm exec expo start --web --localhost
```

The browser preview does not read or modify inventory stored by the Android or iPhone app.

Passing these checks does not replace physical Android and iPhone testing.
