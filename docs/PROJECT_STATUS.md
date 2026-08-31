# Price Intelligence project status

Last updated: 2026-08-31

## Parallel Expo V2 foundation

The `codex/expo-v2-foundation` branch starts an isolated Expo/React Native replacement in
`expoApp/`. It does not replace, modify, or open the installed Kotlin Multiplatform database.

The first foundation milestone provides separate testing identifiers, a versioned SQLite schema,
compatible backup v1/v2 parsing, safe merge import, portable export, bounded retailer history,
shared Supreme assets/theme tokens, and automated compatibility tests. See
`docs/EXPO_V2_MIGRATION.md` for the implementation order and physical-device cutover gates.

The inventory milestone now adds the V2 dashboard and a real SQLite-backed inventory workflow:
grouped search, add/edit validation, native text input and paste, barcode scanning, price calculator,
individual and multi-delete with Undo, local refresh/reset, accessible status banners, and deliberately
limited haptics. Android and iPhone physical-device verification is still required.

The V2 browser preview now runs against its own isolated Expo SQLite web database for responsive UI
inspection. The dashboard, inventory, editor, calculator, backup sheet, populated rows, and banners
were visually checked at 320, 360, and 390 pixel phone widths. The user physically confirmed that the
calculator and camera open correctly on iPhone; delayed scan-complete haptics still require an iPhone
retest because iOS suppresses feedback while the camera remains active.

## Authoritative source state

The latest application milestone on `master` is:

`8f443a5 Add shared price history and release hardening`

This documentation package is intentionally documentation-only. It must not change the app binary, database, or runtime behavior.

The repository remote is:

`shumonfarhan22/priceintelligence`

## Completed application milestones

The KMP project currently includes:

- Shared Room inventory database, repository, migrations, and exported schemas.
- Android and iPhone price scrapers with a shared page parser.
- Lifecycle-safe Android and iPhone network monitors.
- Shared Supreme Dark theme and Android/iPhone app shell.
- Dashboard and Inventory navigation.
- Inventory add, edit, validation, search, grouping, refresh, persistence, deletion, and four-second Undo.
- Dashboard search, autocomplete, barcode scan, sorting, pagination, product cards, product details, individual and visible-page price refresh.
- Explicit live/saved/unavailable price presentation.
- External Amazon/Flipkart link opening.
- Persistent bounded price history with latest, lowest, movement, recent observations, and timestamps.
- Portable JSON backup version 2, price-history backup, safe merge restore, and legacy version 1 fallback.
- Keyboard dismissal after Dashboard and Inventory searches and Next/Done form navigation.
- Android dark splash, edge-to-edge handling, optimized release build, and R8/resource shrinking.
- GitHub Android quality checks and deliberately tagged unsigned iPhone IPA checkpoints.

## Latest verification evidence

The `codex/professional-ui-decision-tools` milestone branch passed this local Windows gate on 20 August 2026:

```powershell
.\gradlew.bat :shared:testAndroidHostTest :androidApp:lintDebug :androidApp:assembleDebug :androidApp:assembleRelease
```

Evidence from the current run:

- Build successful.
- 42 automated tests passed with zero failures or skips.
- Android lint completed with zero issues after aligning the target SDK to API 37.
- Debug APK built.
- Minified release APK built.

This branch adds the professional Supreme Dark app shell, proper Dashboard/Inventory navigation icons, a page-scoped decision snapshot, Best online saving sort, Inventory long-press multi-selection, bulk delete through the existing Undo flow, accessibility announcements/state descriptions, safer recreation state, and a separate Inventory dialog file.

The current branch has not yet been physically tested on Android or iPhone. It remains unmerged and untagged.

### Previous completed checkpoint

Application commit `8f443a5` passed this local Windows gate:

```powershell
.\gradlew.bat :shared:testAndroidHostTest :androidApp:lintDebug :androidApp:assembleDebug :androidApp:assembleRelease
```

Evidence from that run:

- Build successful.
- 36 automated tests passed with zero failures or skips.
- Android lint had zero errors.
- Debug APK built.
- Minified release APK built.

The independent GitHub workflow also completed successfully:

`https://github.com/shumonfarhan22/priceintelligence/actions/runs/32333652099`

The earlier iPhone camera-permission compile failure was fixed in:

`536feb6 Fix iOS camera permission compilation`

The corrected iPhone workflow compiled/tests successfully and produced an IPA:

`https://github.com/shumonfarhan22/priceintelligence/actions/runs/32331176159`

Automated evidence does not replace physical-device evidence.

## Current checkpoint state

Branch `codex/professional-ui-decision-tools` requires physical Android verification before the next iPhone checkpoint tag is created.

The expected Android checks are:

1. Existing inventory and price history still appear after installing the branch build.
2. The Supreme Dark background, header, connection status, and icon-based bottom navigation look correct.
3. The Dashboard decision snapshot matches the visible products and clearly says it may use saved prices.
4. `Best online saving` puts the largest positive saved online gap first.
5. Long-press starts Inventory selection; selecting more products, Select shown, Deselect all, bulk Delete, and Undo work.
6. Add, edit, search, barcode scan, individual delete, product details, retailer links, and price refresh still work.
7. Rotating or briefly backgrounding the app keeps the current destination and useful screen state without a crash.

The exact confirmation expected from the user is:

`Android milestone confirmed`

After that confirmation, the local continuation should:

1. Create one pushed tag named `iphone-checkpoint-*` for this complete milestone.
2. Wait for the **Build Unsigned iOS IPA** workflow.
3. Verify shared iPhone tests, unsigned IPA build, and `unsigned-ipa` artifact.
4. Ask the user to install that one IPA with Sideloadly and test it on a physical iPhone.

Do not create the tag before physical Android confirmation.

## Current large milestone implemented on branch

The following work is implemented on `codex/professional-ui-decision-tools`. It must remain unmerged and untagged until physical Android verification.

### Comparison decision tools

- Add concise Dashboard summary information that helps the user decide whether the shop or an online retailer is best.
- Add a useful sort/filter based on the largest available online saving while keeping the existing sort options.
- Use saved prices safely when live prices are unavailable and label their age/source.
- Test comparison ranking and invalid/missing price behavior.

### Bulk inventory management

- Restore long-press multi-selection from the original app.
- Show the selected count, Select All/Deselect All, and bulk Delete.
- Reuse the existing pending-delete set and one four-second Undo banner for the whole selection.
- Ensure selection remains correct when search results or groups change.

### Accessibility and state

- Add meaningful content descriptions and state descriptions.
- Announce loading, errors, deletion/Undo, and selection changes where Compose supports it cross-platform.
- Do not rely on colour alone for comparison state.
- Keep interactive touch targets practical.
- Preserve useful destination, search, sort, expanded-group, page, and dialog state across ordinary recreation where safe.

### Maintainability and compatibility

- Split oversized Dashboard and Inventory Compose files by coherent responsibility without changing behavior.
- Add focused tests for extracted logic, ViewModel state, backup compatibility, and database migration behavior that the environment can genuinely run.
- Review iPhone deployment compatibility without changing the installed bundle identity.
- Keep Android application identity, iPhone bundle identity, Room database name, and backup compatibility stable.

## Cloud branch acceptance gate

Before reporting the branch ready:

- Inspect `AGENTS.md`, this status file, and `docs/ORIGINAL_ANDROID_REFERENCE.md`.
- Inspect recent Git history and the exact source being changed.
- Keep changes within the planned large milestone.
- Run the shared Android host tests, lint, debug APK, and release APK when the Cloud environment supports the Android SDK.
- If the environment cannot run an Android tool, state that limitation exactly; do not claim it passed.
- Push a `codex/` branch and report its commit.
- Do not merge to `master`.
- Do not create an iPhone checkpoint tag.
- Do not claim a physical phone test.

## Known Cloud boundary

Codex Cloud checks out this GitHub repository. It does not have the user's separate local Android reference folder, Android phone, iPhone, Android Studio installation, Sideloadly session, or local signing state.

The reference guide provides the original app knowledge Cloud needs for safe feature and visual decisions. The current shared source and assets remain the authority for the app that users will actually run.
