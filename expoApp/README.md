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

Passing these checks does not replace physical Android and iPhone testing.
