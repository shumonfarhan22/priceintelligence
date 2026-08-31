# Expo V2 migration

Price Intelligence V2 is a parallel Expo/React Native implementation under `expoApp/`. The Kotlin
Multiplatform application remains authoritative and installable until every parity and physical
device gate below passes.

## Why this is parallel

Rewriting the installed app in place would combine framework risk with database risk. V2 therefore
uses temporary side-by-side identifiers during development:

- Android: `com.supreme.priceintelligence.v2`
- iPhone: `com.supreme.priceintelligence.priceintelligence.v2`

No V2 code opens the Room `inventory.db`. The current app exports its versioned JSON backup and V2
imports that file into `price-intelligence-v2.db`. This keeps the current installation recoverable
throughout testing.

## Migration order

1. Foundation (implemented): first-party Expo modules, explicit SQLite migrations, backup import/export, tests.
2. Inventory (implemented, physical-device gate pending): grouping, search, add/edit/delete, Undo,
   camera barcode scan, native text input, paste, and keyboard behavior.
3. Comparison: product discovery, Amazon/Flipkart checks, saved/live/unavailable/failed states.
4. Price movement: bounded history, charts, retailer/time filters, alerts and refresh rules.
5. Personalization: Supreme Dark baseline, light theme, typography, accessibility, live preview.
6. Hardening: backup round trips, offline behavior, cancellation/race tests, large inventories.

## Daily-work acceptance gates

Each workflow must pass on a physical Android phone and iPhone, not only in a simulator or bundle:

- Launch repeatedly without a crash and recover cleanly after process termination.
- Type and paste quickly in every text and number field without lost characters or layout jumps.
- Open and dismiss the keyboard by touch, Return/Search, back, and modal close.
- Scan barcodes with one success notification and the intended haptic only.
- Add, edit, delete, and Undo without losing or duplicating inventory.
- Compare prices with unambiguous retailer and saved/live/failure labels.
- Scroll large inventory, comparison, and movement lists without visible frame drops.
- Import a real current-app backup and export a V2 backup with matching products and history.
- Work offline for all local inventory and history operations.
- Render correctly in dark/light mode and with larger text.

## Cutover rule

The production identifiers must not change and the KMP app must not be retired until the user has
accepted one coherent V2 milestone on both physical platforms and a final backup round trip has been
verified. The production switch will be a separate, explicitly approved milestone.
