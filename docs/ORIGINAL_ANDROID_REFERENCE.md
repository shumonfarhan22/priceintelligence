# Original Android reference guide

## Purpose and provenance

This guide is the Cloud-safe substitute for the completed Android-only application stored on the user's computer at:

`D:\priceintelligence-android-reference`

The local reference was inspected read-only on 2026-08-20. It was not modified or copied into this repository. This document records its visible design, interactions, data behavior, and useful feature expectations so a Cloud task does not need access to the old source tree.

The current Kotlin Multiplatform app is not required to reproduce old implementation mistakes. It should preserve the product identity and valuable behavior while using the current shared architecture and reliability improvements.

## Product identity

The app is branded **SUPREME Price Intelligence**. Its primary purpose is to help a shop compare its own price with Amazon India and Flipkart.

The original application has two main destinations:

- **Dashboard** for finding products and comparing prices.
- **Inventory** for maintaining the local product catalogue.

The current KMP app keeps the same two-destination structure and the same core brand artwork.

## Visual system

The original uses a permanent dark theme called Supreme Dark or Premium Dark Mode. There is no light or dynamic-colour variant.

| Role | Original value | Intended use |
| --- | --- | --- |
| App background | `#0B0F14` | Deep obsidian full-screen background |
| Card surface | `#1E2128` | Dark charcoal cards and menus |
| Borders/dividers | `#313540` | Subtle grey outlines |
| Primary brand | `#10B981` | Emerald actions, selected navigation, scan brackets, favourable states |
| Primary tint | `#123626` | Dark green chip/container background |
| Secondary accent | `#8B7CF6` | Indigo secondary emphasis |
| Main text | `#F8FAFC` | Crisp white headings and values |
| Muted text | `#94A3B8` | Supporting labels and timestamps |
| Tertiary text | `#5B6472` | Placeholders and disabled content |
| Error | `#F87171` / `#EF4444` | Destructive and failed states |
| Error background | `#3F1717` | Dark red containers |
| Warning | `#F59E0B` | Slow or unstable connection |

Common visual characteristics:

- Rounded cards, usually 12-24 dp corner radii.
- Fine, low-contrast borders rather than heavy shadows.
- Frosted/glass-like translucent surfaces over the obsidian background.
- Emerald for the primary action and selected state.
- Red/rose when an online offer makes the shop price less competitive.
- Green when the shop matches or beats the online price.
- Off-white image panels so product photographs remain readable, with a subtle dark overlay to reduce glare.
- Soft radial colour blooms for success, warning, error, and comparison feedback.
- Bottom navigation and important actions float above system and keyboard insets.

The shared KMP resources contain exact copies of the original `app_logo.png`, `logo_amazon.png`, and `logo_flipkart.png`. Their SHA-256 hashes match the reference assets:

| Asset | SHA-256 |
| --- | --- |
| Supreme app logo | `A7FD2E4705761A8BAD48A4DABF917E7CF1349B8342B19D7361EE79F750283F48` |
| Amazon logo | `EADFE9501B3860D3845FA46A9EB98C505BE490889B9B119C3DCC79148F053F97` |
| Flipkart logo | `F02D09CDF0646F72A411FC0A6B6D6F065E2700642FFAFC47CFED9E88D77A4667` |

Do not replace these assets or redraw them from this written description.

## Original app shell

- The Android launch screen uses the obsidian background with the Supreme logo and branding.
- Dashboard and Inventory are reachable from a floating 72 dp bottom navigation bar.
- Selected navigation content is emerald; unselected content is grey.
- Android Back returns from Inventory to Dashboard. On Dashboard, normal system Back behavior remains available.
- Status and Undo banners float above the navigation bar and move above the keyboard when necessary.
- Banner heights are measured rather than assumed, avoiding overlap when text wraps or font size grows.

The current shared shell uses a polished cross-platform header, connection badge, and bottom destination selector. Treat the current shared shell as authoritative unless a milestone explicitly redesigns it.

## Dashboard reference

### Header and list

- The original Dashboard top bar shows the Supreme logo and the stacked words `SUPREME` and `PRICE INTELLIGENCE`.
- The header compacts while the list scrolls.
- Results are loaded 10 at a time from Room rather than loading the whole inventory.
- The result line reports the number of matches and search time.
- Sort choices are Most Viewed, Alphabetical, and Recent.
- Previous/next pagination controls show the current page and total pages.
- Pull to refresh reloads the current database result set.
- Empty and no-match states explain what the user can do next.

### Search

- A floating search field sits above bottom navigation.
- Search accepts product names, exact numeric barcodes, Amazon links, and Flipkart links.
- Name suggestions appear after a short debounce and can be tapped.
- The keyboard Search action submits the query.
- Clearing an empty query restores the full list.
- A scan action at the end of the search field opens the barcode scanner.
- While search has focus, the list is dimmed and background scrolling is disabled.
- Tapping the dimmed area or completing a search dismisses the keyboard.

### Compact product card

- A compact frosted card contains a 96 dp square product image, product name, and an emerald outlined shop-price chip.
- Tapping the card opens the full product details.
- After a live comparison, the border/bloom can turn rose if an online price is cheaper, or green if the shop matches or beats the available online prices.
- Price meaning must also be stated in text; do not depend only on those colours.

### Product details

- The original details appear as a near-full-screen dark glass dialog.
- The main content uses a bento arrangement: a larger left section for image, name, and shop price; smaller Amazon and Flipkart price panels on the right.
- The product image can be tapped to open a larger dark image viewer.
- Amazon and Flipkart panels show the active price, difference from the shop price, and whether the retailer is lower, higher, matched, checking, or unavailable.
- A successful live result takes priority over the saved last price.
- A saved last price remains visible when a new live result is unavailable.
- Retailer link buttons open the saved product pages externally.
- `Refresh Live Prices` checks the available retailer links.
- During a check the original showed progress, network throughput, last-check time, and request duration. These are presentation details, not requirements to reproduce if they add complexity without helping decisions.

The current KMP details intentionally improve this experience by labeling live versus saved data and showing persistent price history, latest saved price, lowest saved price, movement, recent observations, and time of the latest observation.

### Network feedback

- Green ambient feedback indicates a connection becoming available.
- Persistent red ambient feedback indicates offline state.
- Amber feedback indicates that linked retailer checks returned no prices, such as on a slow or blocked connection.
- Short banners explain `No internet connection` and `Slow or unstable connection`.
- Cached data must remain usable while offline.

## Inventory reference

### Header, search, and grouping

- The normal header says `SUPREME INVENTORY` and displays the total product count.
- Refresh reloads the current inventory or filtered result.
- A menu exposes backup/import and backup/export actions.
- The directory search filters as the user types after a short debounce.
- Products are grouped by the uppercase first word of the product name, which acts as a brand/folder name.
- Group headers show the product count and expand/collapse state.
- Searching temporarily shows matching products regardless of collapsed groups.
- A newly added or edited product is temporarily highlighted, moved to the top, and its group is expanded.

### Inventory rows

- A row shows the product name, shop price, and edit/delete actions.
- Swiping exposes deletion.
- Long press enters multi-selection mode.
- Multi-selection shows the selected count, Select All/Deselect All, and a bulk Delete action.
- Individual and bulk deletion hide items immediately but wait four seconds before permanent deletion.
- A global Undo banner restores the complete pending set.

The current KMP app already supports individual deletion and a four-second multi-item-capable Undo state, but the visible long-press multi-selection and Select All UI are not yet restored. This is a deliberate next-milestone item, not a reason to rewrite existing deletion logic.

### Add/edit form

- A floating emerald Add button opens a full-screen dark glass form.
- The title changes between Add New Product and Edit Product.
- Fields are Product Name, Shop Price, Barcode, optional Amazon URL, and optional Flipkart URL.
- The barcode field has a scan action.
- Clear Form and Save are side by side.
- Product name and a valid positive shop price are required.
- Barcodes are unique.
- Amazon and Flipkart links cannot be reused by a different product.
- A successful add/edit closes the form, refreshes Inventory, expands the relevant group, and highlights the product.
- The form resizes or scrolls above the keyboard.

The current KMP form adds stricter retailer-host validation, HTTPS normalization, a predictable Next/Done focus path, and explicit permission-denied guidance. Preserve those improvements.

### Backup and restore

The original Android app exported the Room database file and imported only missing products. The KMP app intentionally replaces this with portable versioned JSON so Android and iPhone can exchange backups. Current restore merges missing products and their price history without deleting or replacing existing products. Preserve this newer behavior; do not restore the old raw-database backup design.

## Barcode scanner reference

- The scanner is full-screen and requests camera access only when opened.
- A translucent dark overlay leaves a centered square viewfinder.
- Emerald corner brackets frame the barcode area.
- The original Android scanner includes a flash toggle and a short instruction.
- A valid barcode returns to the calling search or form.
- Permission denial must not trap the user; manual barcode entry remains available.

The current KMP scanner works on Android and iPhone through shared scanner UI and platform-safe permission requesters. Platform permission APIs must remain in platform source sets.

## Data and comparison behavior

The original inventory record contains product name, optional barcode, shop price, optional retailer URLs, optional image URL, search count, timestamps, and the latest saved Amazon/Flipkart prices and check times.

The current KMP database extends that model with persistent price-history rows. Important rules are:

- Only finite positive online prices are valid.
- Shop and online prices are compared with a one-paise tolerance.
- Positive shop-minus-online difference means an online retailer is cheaper.
- A live result takes precedence for the current session.
- A failed live check does not erase a previously saved valid price.
- Each successful check is saved; the newest 60 observations per product and retailer are retained.
- Deleting a product cascades its history, and a late network result must not recreate deleted data or crash.
- Viewing/searching a product can increase popularity, but it must not falsely change the product's edit timestamp.

## Capability map at KMP application commit `8f443a5`

| Capability | KMP status | Notes |
| --- | --- | --- |
| Supreme Dark palette and original brand assets | Complete | Same core palette; three important image assets match byte-for-byte |
| Shared Android/iPhone app shell | Complete | Current cross-platform shell is authoritative |
| Room inventory persistence and migrations | Complete | Database version 4 with exported schemas |
| Add/edit/search/validation | Complete | Includes stronger URL and numeric validation |
| Search by name, barcode, and retailer URL | Complete | Shared repository behavior |
| Barcode scanning | Complete | Android and iPhone permission handling |
| Brand grouping and expandable folders | Complete | Shared Inventory UI |
| Individual delete and four-second Undo | Complete | Pending delete state can hold multiple products |
| Visible long-press multi-selection and Select All | Remaining | Restore in a future large milestone using the existing pending set |
| Dashboard paging and three original sort choices | Complete | Recent now correctly means recently edited |
| Product cards and detailed comparison | Complete | Cross-platform implementation |
| Live and saved price distinction | Complete | More explicit than original |
| Amazon and Flipkart scraping | Complete | Platform clients share parsing rules |
| Offline/safe network state | Complete | Lifecycle-safe Android/iPhone monitors |
| Persistent price history and trend insight | Complete improvement | Newest 60 successful checks per retailer |
| Portable backup and restore | Complete improvement | Versioned JSON includes price history and supports older backup format |
| Keyboard dismissal after search | Complete | Dashboard and Inventory |
| Android debug/release build hardening | Complete | Includes release shrinking and dark launch screen |
| Full accessibility audit and state restoration | Remaining refinement | Planned for a future large milestone |

## Intentional improvements that must not regress

Do not blindly port these old implementation details:

- Android-only ViewModels, Context access, CameraX UI, and file pickers cannot enter `commonMain`.
- Raw Room database export is replaced by portable versioned JSON.
- Search/view counts no longer alter `updated_at`; Recent means genuinely recently edited.
- Retailer URLs are normalized, limited to supported hosts, and upgraded to HTTPS.
- Network monitors unregister callbacks and avoid retaining dead screens.
- Scrapers preserve coroutine cancellation and close their clients.
- Invalid `NaN` or infinite legacy prices cannot crash formatting or backup.
- Database migrations and schemas must remain explicit and non-destructive.
- Price checks are bounded and deletion races are handled safely.
- Live and saved data are explicitly labeled.

## Source map for Cloud agents

Use these current KMP files as the implementation authority:

- App shell: `shared/src/commonMain/kotlin/com/supreme/priceintelligence/App.kt`
- Dashboard UI: `shared/src/commonMain/kotlin/com/supreme/priceintelligence/dashboard/`
- Inventory UI: `shared/src/commonMain/kotlin/com/supreme/priceintelligence/inventory/`
- Database/repository/backup: `shared/src/commonMain/kotlin/com/supreme/priceintelligence/data/`
- Price parser and URL rules: `shared/src/commonMain/kotlin/com/supreme/priceintelligence/network/`
- Platform scraper/network/permission implementations: `shared/src/androidMain/` and `shared/src/iosMain/`
- Shared artwork: `shared/src/commonMain/composeResources/drawable/`
- Room schemas: `shared/schemas/`
- Automated tests: `shared/src/commonTest/`
- Android application configuration: `androidApp/`
- iPhone project: `iosApp/`
- CI workflows: `.github/workflows/`
