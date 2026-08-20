# Price Intelligence agent guide

Read this file before changing the project. Then read, in order:

1. `docs/PROJECT_STATUS.md`
2. `docs/ORIGINAL_ANDROID_REFERENCE.md`
3. `README.md`
4. The current source files and recent Git history

The current repository is the authoritative Kotlin Multiplatform implementation. The completed original Android application is a separate, read-only reference. A cloud environment cannot see that local project, so `docs/ORIGINAL_ANDROID_REFERENCE.md` records the parts of it that matter.

## Product contract

Price Intelligence is an offline-first app for comparing a shop's price with current Amazon India and Flipkart prices. Keep work within that purpose.

The product must continue to provide:

- A local inventory with add, edit, search, barcode scan, grouping, deletion, and Undo.
- Search by product name, barcode, Amazon link, or Flipkart link.
- Clear comparison of the shop price with Amazon and Flipkart.
- Explicit labels for live prices, saved prices, unavailable prices, and failed checks.
- Persistent bounded price history and useful price movement information.
- A portable backup that preserves inventory and price history.
- Safe Android and iPhone behavior from shared code wherever practical.

Do not add accounts, advertising, analytics, a remote backend, unrelated shopping features, or new retailer integrations unless the user explicitly expands the scope.

## Reference and design rules

- Never modify the original Android reference. On the user's local computer its path is `D:\priceintelligence-android-reference`.
- In Cloud, do not assume the original source exists. Use `docs/ORIGINAL_ANDROID_REFERENCE.md` instead.
- Preserve the established Supreme Dark visual identity. The current shared UI is the authoritative visual baseline when it differs from the old Android-only implementation.
- Reuse the shared theme and bundled brand assets. Do not replace the Supreme, Amazon, or Flipkart artwork with generated substitutes.
- Preserve the core palette: obsidian background `#0B0F14`, charcoal surface `#1E2128`, border `#313540`, emerald primary `#10B981`, indigo accent `#8B7CF6`, white text `#F8FAFC`, and slate text `#94A3B8`.
- Price meaning must never rely on colour alone. Include text such as lower, higher, matched, live, or saved.
- Keep touch targets practical, content descriptions meaningful, keyboard dismissal reliable, and layouts usable with larger text.
- Prefer a polished evolution of the current app over a pixel-for-pixel recreation of old Android code.

## Architecture and data safety

- Put database models, repositories, validation, parsing, ViewModels, and Compose UI in `shared` unless behavior is genuinely platform-specific.
- Use `expect`/`actual` or a small injected interface for Android/iPhone differences. Never import Android APIs in `commonMain`.
- Keep the Room database migration path intact. Never use destructive migration or silently discard inventory.
- The current database is version 4 and includes `InventoryItem` plus `PriceHistoryEntry`.
- Keep Room schemas in `shared/schemas` synchronized with any schema change and add an explicit migration.
- Keep at most the newest 60 successful observations per product and retailer unless the product requirements deliberately change.
- Preserve backup format compatibility. A newer app must continue to restore supported older backups safely.
- Validate retailer hosts and HTTPS links. Do not send requests to arbitrary hosts from inventory data.
- Preserve coroutine cancellation and close platform network clients when their owner is disposed.
- Handle deletion/network races without crashing or recreating deleted data.

## Milestone workflow

The user wants large, coherent milestones because each unsigned iPhone IPA takes about ten minutes to build and must be installed manually.

For Cloud implementation work:

1. Start from the requested `master` commit and inspect the worktree and Git history.
2. Work on a separate `codex/` branch. Do not merge it into `master`.
3. Keep one milestone internally coherent; do not scatter unrelated changes across several small checkpoints.
4. Run the applicable tests and build checks.
5. Commit and push the branch, then report the commit, changed behavior, test evidence, and anything that still needs a physical device.
6. Leave merging, checkpoint tagging, and physical-device confirmation to the user/local continuation.

Never create or push an `iphone-checkpoint-*` tag from a Cloud implementation task unless the user explicitly says the milestone passed on a physical Android phone. Normal branch pushes must not trigger an IPA build.

The original app and the current app may be inspected for behavior, but do not copy Android-only architecture into shared code. Preserve current reliability improvements described in the reference guide.

## Verification commands

On Windows with the required JDK available:

```powershell
.\gradlew.bat :shared:testAndroidHostTest :androidApp:lintDebug :androidApp:assembleDebug :androidApp:assembleRelease
```

On Linux or macOS:

```bash
./gradlew :shared:testAndroidHostTest :androidApp:lintDebug :androidApp:assembleDebug :androidApp:assembleRelease
```

On macOS, the shared iPhone compilation/test gate is:

```bash
./gradlew :shared:iosSimulatorArm64Test
```

Do not claim physical Android or iPhone verification from a unit test, simulator compilation, APK build, or unsigned IPA build. Those are separate gates.

## User communication

The user is new to coding. Lead with the result and use short, easy instructions.

If the user must manually edit an existing file, always provide:

1. The exact file path.
2. `Find this code`.
3. `Replace with this code`.

The replacement must include unchanged surrounding lines from the Find block. For a new file, provide the exact folder, filename, and complete content without ellipses.
