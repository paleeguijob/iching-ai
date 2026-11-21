# Quiet Ink UI Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restyle every screen of the Compose Multiplatform app to the Quiet Ink design (cards, calmer type, flat tab bar, plain reading sheet, compact library with trigram filter) without changing flows or business logic.

**Architecture:** Add design tokens (`ZenPalette.card/cardShadow`, `ZenSpacing`, tuned typography/shapes) and a small set of shared components in `ui/components`, then migrate screens onto them one at a time. The only new logic is pure functions (trigram filter, quota line, short Thai date, sheet header), each unit-tested in `commonTest`.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform + Material3, Koin, kotlin.test.

**Spec:** `docs/superpowers/specs/2026-11-08-quiet-ink-ui-redesign-design.md` (mockups in `.superpowers/brainstorm/23458-1794129859/content/`)

All paths below are relative to `frontend/composeApp/src/commonMain/kotlin/com/iching/ai/` (code) or `frontend/composeApp/src/commonTest/kotlin/com/iching/ai/` (tests) unless they start with `frontend/` or `docs/`.

Commands (run from `frontend/`):
- Unit tests: `./gradlew :composeApp:testAndroidHostTest`
- Compile check: `./gradlew :androidApp:assembleDebug`

## Global Constraints

- Branch `release/v1.3`. No backend changes. No change to navigation, ViewModel behaviour (except Library filter state), or copy beyond what the spec names.
- Existing palette colour values must not change (`ThemeContrastTest.defaultThemeKeepsCurrentColors` keeps passing).
- New card colours: INK_PAPER `#FFFFFF` · BAMBOO `#FBFDF7` · CINNABAR_DAWN `#FFFBF7` · INK_NIGHT `#222833` · CINNABAR_GOLD `#5E1813`. Card shadow: light themes `inkBlack` α0.07, dark themes `Color.Black` α0.35.
- Shapes: small 8dp, medium 12dp, large 16dp. Spacing steps 4·8·12·20·28. Screen side padding 20dp.
- Cinnabar is accent-only: primary-button seal dot, changing-line ring, reading-sheet top rule, selected-tab dot.
- Charm (display font) at most once per screen, for the page title.
- Share card (`feature/share`) keeps its current look; `ScrollRoller` may stay only if the share card uses it.
- No AI-generated reading headline.
- Contrast: `inkBlack`/`card` and `inkSoft`/`card` ≥ 4.5:1, `bambooDeep`/`card` ≥ 3:1, on all 5 themes.

## Review Focus

1. Keyboard open on Home with the pinned CTA — the CTA must ride above the IME and the question field must stay visible (manual check, Task 12).
2. Dark themes (Ink Night, Cinnabar Gold) — shadows vanish, so cards must still separate from the page via the 1dp `outlineVariant` bottom edge (manual check, Task 12).
3. Library filter + query with no matches — must show an empty message, not a blank card (test in Task 8).
4. Scripture-only readings (`InterpretationSource.SCRIPTURE`) — sheet header must read "ตัวบทจากคัมภีร์" with no AI badge (test in Task 7).
5. System font scale 1.3× — tab labels, chips and the quota line wrap without clipping (manual check, Task 12).

---

### Task 1: Design tokens

**Files:**
- Modify: `ui/theme/Palette.kt`, `ui/theme/Theme.kt`
- Create: `ui/theme/ZenSpacing.kt`
- Test: `ui/theme/ThemeContrastTest.kt`

**Interfaces:**
- Produces: `ZenPalette.card: Color`, `ZenPalette.cardShadow: Color`; `ZenColors.Card`, `ZenColors.CardShadow`; `object ZenSpacing { val xs = 4.dp; val sm = 8.dp; val md = 12.dp; val lg = 20.dp; val xl = 28.dp; val screen = 20.dp }`; `MaterialTheme.colorScheme.surfaceContainerLowest == card`.

- [ ] **Step 1: Add failing tests** to `ThemeContrastTest`:
  - `cardTextIsReadableOnEveryTheme`: for every `AppTheme`, `contrast(inkBlack, card) >= 4.5f`, `contrast(inkSoft, card) >= 4.5f`, `contrast(bambooDeep, card) >= 3f`, message `"$theme <pair> is only $ratio:1"`.
  - `cardColoursMatchSpec`: `paletteFor(INK_PAPER).card == Color(0xFFFFFFFF)`, `INK_NIGHT.card == Color(0xFF222833)`, `CINNABAR_GOLD.card == Color(0xFF5E1813)`.
- [ ] **Step 2: Run** `./gradlew :composeApp:testAndroidHostTest --tests '*ThemeContrastTest*'` — expect compile failure (`card` unresolved).
- [ ] **Step 3: Implement.** Add `card` and `cardShadow` to `ZenPalette` (no defaults; set in all five palettes with the Global Constraints values). Add `ZenColors.Card/CardShadow`. In `ZenTheme`: `surfaceContainerLowest = palette.card`; shapes medium 12dp / large 16dp; typography `bodyMedium` lineHeight 27.sp, `labelSmall` letterSpacing 0.8.sp. Create `ZenSpacing.kt`.
- [ ] **Step 4: Run** the same test command — expect PASS (including `defaultThemeKeepsCurrentColors`). If a contrast fails, adjust only the new `card` value for that theme and record it in the spec.
- [ ] **Step 5: Commit** `git commit -m "Add Quiet Ink card tokens, spacing scale and type tweaks"`

### Task 2: Shared components

**Files:**
- Create: `ui/components/ZenCard.kt`, `ui/components/ZenChip.kt`
- Modify: `ui/components/ZenDecor.kt` (buttons, `SectionLabel`, `PaperBackground`)

**Interfaces:**
- Consumes: Task 1 tokens.
- Produces:
  - `@Composable fun ZenCard(modifier: Modifier = Modifier, accentTop: Boolean = false, onClick: (() -> Unit)? = null, contentPadding: PaddingValues = PaddingValues(16.dp), content: @Composable ColumnScope.() -> Unit)` — background `ZenColors.Card`, shape 16dp (with `accentTop`: 3dp `CinnabarRed` top rule, top corners 4dp), shadow 8dp in `CardShadow`, 1dp `outlineVariant` bottom edge.
  - `@Composable fun ZenCardDivider()` — 1dp `outlineVariant` full-width line for rows inside a card.
  - `@Composable fun ZenChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier)` — pill; selected = `InkBlack` fill / `PaperCream` text; unselected = 1dp `outlineVariant` border / `InkSoft` text.
  - `ZenPrimaryButton(text, onClick, modifier, enabled, color, showSeal: Boolean = true)` — height 50dp, shape 14dp, 8dp rounded-square `CinnabarSoft` dot before text when `showSeal`.
  - `ZenSecondaryButton` — shape 12dp, border `outline`.
  - `@Composable fun ZenTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier)` — `labelLarge`, `InkSoft`.
  - `SectionLabel(text, modifier, color)` — same signature, now plain `labelSmall` text, no side strokes.
  - `PaperBackground(modifier, showMountains: Boolean = false, content)` — default flips to `false`; ensō alpha 0.035 → 0.018, mountain alphas halved.

- [ ] **Step 1: Implement** the components above.
- [ ] **Step 2: Find callers relying on the old `showMountains = true` default** (`grep -rn "PaperBackground" composeApp/src/commonMain`) and pass `showMountains = true` explicitly only on Home, Divination and Onboarding.
- [ ] **Step 3: Run** `./gradlew :androidApp:assembleDebug` — expect BUILD SUCCESSFUL.
- [ ] **Step 4: Commit** `git commit -m "Add ZenCard and ZenChip and restyle buttons and section labels"`

### Task 3: Hexagram figure marks

**Files:** Modify `ui/components/HexagramFigure.kt`

**Interfaces:** `HexagramFigure` signature unchanged.

- [ ] **Step 1: Replace `drawChangeMark`** for both old yang and old yin with a ring: stroke `CinnabarSoft`, width `height * 0.18f`, rounded rect around the whole line inset by `-height * 0.35f` (drawn in both `StaticHexagram` and `HexagramLine`). Remove the side mark offset (`markW`) so figures centre on the lines.
- [ ] **Step 2: Change `drawPlaceholder`** to a dashed outline (`PathEffect.dashPathEffect(floatArrayOf(h, h * 0.8f))`, 1.5dp, `outline` colour) instead of a filled faint bar.
- [ ] **Step 3: Run** `./gradlew :androidApp:assembleDebug` — expect success. Update the KDoc line about "dot or cross" to "ring".
- [ ] **Step 4: Commit** `git commit -m "Mark changing lines with a cinnabar ring and draw pending lines dashed"`

### Task 4: Flat tab bar

**Files:** Modify `App.kt` (`ZenTabBar`, and the scaffold/bottom padding that reserves space for the floating pill)

- [ ] **Step 1: Rewrite `ZenTabBar`** as a full-width `Row` with background `PaperMid.copy(alpha = 0.92f)`, 1dp `outlineVariant` top border, `navigationBarsPadding()`, vertical padding 8dp, `Arrangement.SpaceAround`. Each tab: icon 20dp + `labelSmall`; selected = `InkBlack` + 4dp `CinnabarRed` rounded square below label; unselected = `InkFaint`. Keep the `clickable` and `contentDescription`.
- [ ] **Step 2: Remove the extra bottom inset** screens added to clear the floating pill (`grep -rn "padding(bottom" composeApp/src/commonMain/kotlin/com/iching/ai/feature`) so content ends at the bar.
- [ ] **Step 3: Run** `./gradlew :androidApp:assembleDebug` — expect success.
- [ ] **Step 4: Commit** `git commit -m "Replace the floating tab pill with a flat bar"`

### Task 5: Home screen

**Files:** Modify `feature/home/HomeScreen.kt` · Test: `feature/home/GuidanceQuotaTest.kt`

**Interfaces:**
- Produces: `internal fun quotaLine(quota: GuidanceQuota): String` in `HomeScreen.kt`.

- [ ] **Step 1: Write failing tests** in `GuidanceQuotaTest`:
  - `quotaLineAppendsThaiCount`: `quotaLine(GuidanceQuota("ชมโฆษณา รับคำชี้แนะเพิ่มได้อีก", 2, "x"))` == `"ชมโฆษณา รับคำชี้แนะเพิ่มได้อีก ๒ ครั้ง"`.
  - `quotaLineWithoutCountIsLabel`: `quotaLine(GuidanceQuota("นักพรตปลีกวิเวกชั่วคราว", null, null))` == `"นักพรตปลีกวิเวกชั่วคราว"`.
- [ ] **Step 2: Run** `./gradlew :composeApp:testAndroidHostTest --tests '*GuidanceQuotaTest*'` — expect FAIL (unresolved `quotaLine`).
- [ ] **Step 3: Implement `quotaLine`** and restructure `HomeScreen` per spec order: top bar (seal 34→24dp + "อี้จิง" + settings) → `DailyHexagramCard` as a horizontal `ZenCard` (small figure, `SectionLabel("กวาประจำวัน")`, name, streak text kept) → `CampaignHomeCard` as `ZenCard` → Charm title "ถามทางแห่งเต๋า" + one-line subtitle (drop the large 問道 glyph in `BreathingTitle`) → `QuestionField` inside a `ZenCard` (no outline, min height 96dp) → `ZenChip` row → quota: `GuidanceQuotaBar` replaced by centred `bodySmall` `InkFaint` text `quotaLine(...)` with `note` as a second line when non-null. Move `ZenPrimaryButton(startButtonText(deciding))` out of the scroll column into a bottom-aligned slot of the screen `Box`, 12dp above the tab bar, with `imePadding()`; add bottom content padding to the scroll equal to button height + 24dp.
- [ ] **Step 4: Run** tests (expect PASS) and `./gradlew :androidApp:assembleDebug`.
- [ ] **Step 5: Commit** `git commit -m "Restyle Home in Quiet Ink and pin the cast button"`

### Task 6: Coin toss stage

**Files:** Modify `feature/divination/DivinationScreen.kt`, `feature/divination/TossStage.kt`, `ui/components/AncientCoin.kt`

- [ ] **Step 1: Top bar:** add right-aligned `"${revealed.toThaiNumerals()} / ๖"` and a 3dp `LinearProgressIndicator`-style bar (track `outlineVariant`, fill `InkBlack`, progress `revealed / 6f`) under it, side padding 20dp. Show only while casting.
- [ ] **Step 2: Coins:** add a soft drop shadow under each coin (`Modifier.shadow(8.dp, CircleShape, spotColor = ScrollWood)`) and stagger the middle coin's landing by 80ms (existing `delayMillis = i * 90` → middle coin index uses `i * 90 + 80`).
- [ ] **Step 3: Buttons:** "โยนครั้งต่อไป" → `ZenPrimaryButton`; "โยนต่อเนื่องจนครบ" → `ZenTextButton`.
- [ ] **Step 4: Run** `./gradlew :androidApp:assembleDebug`.
- [ ] **Step 5: Commit** `git commit -m "Add cast progress and coin depth to the toss stage"`

### Task 7: Reading sheet

**Files:** Modify `feature/reading/ReadingContent.kt`, `feature/divination/DivinationScreen.kt:262`, `feature/journal/JournalScreens.kt:288`, `feature/divination/ReadingExtras.kt` · Test: create `feature/reading/ReadingSheetTest.kt`

**Interfaces:**
- Produces: `internal data class SheetHeader(val label: String, val showAiBadge: Boolean)`; `internal fun sheetHeader(source: InterpretationSource?): SheetHeader`; `@Composable fun OracleSheet(text: String, isComplete: Boolean, animate: Boolean, source: InterpretationSource?, modifier: Modifier = Modifier)` (replaces `OracleScroll`, same parameters).

- [ ] **Step 1: Write failing tests** in `ReadingSheetTest`:
  - `scriptureSheetHasNoAiBadge`: `sheetHeader(InterpretationSource.SCRIPTURE) == SheetHeader("ตัวบทจากคัมภีร์", false)`.
  - `aiSheetShowsBadge`: for every other `InterpretationSource` value and `null`, `sheetHeader(it) == SheetHeader("คำชี้แนะจากนักพรต", true)`.
- [ ] **Step 2: Run** `./gradlew :composeApp:testAndroidHostTest --tests '*ReadingSheetTest*'` — expect FAIL.
- [ ] **Step 3: Implement.** `OracleSheet` = `ZenCard(accentTop = true)` with header row (label `labelSmall` `CinnabarRed`; when `showAiBadge` a right-aligned pill "AI" 1dp `outlineVariant` border, `InkFaint`), then the existing waiting text / `TypewriterText` / seal reveal logic moved over unchanged. Delete `OracleScroll`; delete `ScrollRoller` unless `feature/share` references it. Wrap `CastSummary` hexagram pair in one `ZenCard`. Restyle `ScriptureSection`/`ScriptureBlock` and `ReadingExtras` blocks with `ZenCard` + `SectionLabel`. Update both call sites.
- [ ] **Step 4: Run** tests (PASS) and `./gradlew :androidApp:assembleDebug`.
- [ ] **Step 5: Commit** `git commit -m "Replace the oracle scroll with a plain reading sheet"`

### Task 8: Library list and trigram filter

**Files:** Modify `feature/library/LibraryViewModels.kt`, `feature/library/LibraryScreens.kt` · Test: create `feature/library/TrigramFilterTest.kt`

**Interfaces:**
- Produces: `internal fun filterByTrigram(hexagrams: List<Hexagram>, trigram: Trigram?): List<Hexagram>`; `LibraryUiState.trigram: Trigram? = null`; `LibraryViewModel.onTrigramChange(trigram: Trigram?)`; screen reads `state.visible` where `val LibraryUiState.visible get() = filterByTrigram(hexagrams, trigram)`.

- [ ] **Step 1: Write failing tests** (build `Hexagram` fixtures with patterns `"111111"` (乾), `"000000"` (坤), `"100010"` (屯: lower 震 `100`, upper 坎 `010`)):
  - `nullTrigramKeepsAll` → all three.
  - `matchesLowerTrigram`: `ZHEN` → only 屯.
  - `matchesUpperTrigram`: `KAN` → only 屯.
  - `matchesEitherPositionOnce`: `QIAN` → only 乾 (listed once).
  - `noMatchIsEmpty`: `LI` → empty list.
- [ ] **Step 2: Run** `./gradlew :composeApp:testAndroidHostTest --tests '*TrigramFilterTest*'` — expect FAIL.
- [ ] **Step 3: Implement** the filter and state. UI: Charm title "คัมภีร์ ๖๔ กวา" + one-line subtitle; search field on `Card` colour, shape 12dp, min height 44dp; horizontal `LazyRow` of `ZenChip` ("ทั้งหมด" + 8 trigrams labelled `"${symbol} ${natureTh}"`); list = one `ZenCard` of rows (Thai numeral in `CinnabarRed` · `HexagramFigure` 30dp wide · `nameCn` + `shortNameTh` · `meaningTh`) separated by `ZenCardDivider`. Empty `visible` → centred `InkSoft` text "ไม่พบกวาที่ตรงกับการค้นหา". Restyle the detail screen with `ZenCard` like Task 7.
- [ ] **Step 4: Run** tests (PASS) and `./gradlew :androidApp:assembleDebug`.
- [ ] **Step 5: Commit** `git commit -m "Compact the library list and add a trigram filter"`

### Task 9: Journal

**Files:** Modify `ui/ThaiDates.kt`, `feature/journal/JournalScreens.kt` · Test: create `ui/ThaiDatesTest.kt`

**Interfaces:**
- Produces: `fun formatThaiShortDate(epochMillis: Long): String` → e.g. `"๖ พ.ย."` (Thai numerals + abbreviated month `ม.ค. ก.พ. มี.ค. เม.ย. พ.ค. มิ.ย. ก.ค. ส.ค. ก.ย. ต.ค. พ.ย. ธ.ค.`).

- [ ] **Step 1: Write failing test** `shortDateUsesThaiDigitsAndAbbreviation`: build millis from `LocalDateTime(2026, 11, 6, 20, 27).toInstant(TimeZone.currentSystemDefault())`, assert `formatThaiShortDate(it) == "๖ พ.ย."`; and `2026-01-15` → `"๑๕ ม.ค."`.
- [ ] **Step 2: Run** `./gradlew :composeApp:testAndroidHostTest --tests '*ThaiDatesTest*'` — expect FAIL.
- [ ] **Step 3: Implement** and restyle the list: remove timeline rail and dots; per month a `SectionLabel(formatThaiMonth(...))` in `BambooDeep` then one `ZenCard` of rows with `ZenCardDivider`. Row: `"${formatThaiShortDate} · ${HH:mm}"` (+ `" · ตัวบทคัมภีร์"` for scripture-only) in `bodySmall` `InkFaint` · question `titleSmall` · `"震 เจิ้น → 豐 เฟิง"` in `BambooDeep` · small `HexagramFigure` right. Journal detail reuses the Task 7 layout.
- [ ] **Step 4: Run** tests (PASS) and `./gradlew :androidApp:assembleDebug`.
- [ ] **Step 5: Commit** `git commit -m "Group journal entries into monthly cards"`

### Task 10: Settings

**Files:** Modify `feature/settings/SettingsScreen.kt`

- [ ] **Step 1: Restyle** without adding or removing settings: account `ZenCard` at top (24dp→36dp `CinnabarSeal("易")`, membership/coin text from existing state, small `ZenPrimaryButton(showSeal = false)` "อัปเกรด" only when not premium, wired to the existing paywall callback); existing items grouped under `SectionLabel` headers into `ZenCard`s with `ZenCardDivider`; theme picker = row of five 22dp colour dots (`paletteFor(theme).paperCream`), selected dot has a 2dp `InkBlack` ring, `contentDescription` = existing theme name.
- [ ] **Step 2: Run** `./gradlew :composeApp:testAndroidHostTest --tests '*settings*'` and `./gradlew :androidApp:assembleDebug`.
- [ ] **Step 3: Commit** `git commit -m "Group settings into cards with a dot theme picker"`

### Task 11: Remaining screens

**Files:** Modify `feature/onboarding/OnboardingScreen.kt`, `feature/paywall/PaywallScreen.kt`, `feature/home/PreCastSheet.kt`, `feature/home/AskYearScreen.kt`, `feature/daily/DailyScreen.kt`, `feature/year/YearHexagramScreen.kt`, `feature/divination/ListenButton.kt`

- [ ] **Step 1: Migrate** each screen onto `ZenCard`/`ZenChip`/buttons/`SectionLabel`, side padding `ZenSpacing.screen`, Charm only on the page title, cinnabar only as accent. No layout reordering.
- [ ] **Step 2: Check no stragglers:** `grep -rn "RoundedCornerShape(2[0-9]\|OutlinedTextField\|FilterChip" composeApp/src/commonMain` — remaining hits must be justified (e.g. share card) or migrated.
- [ ] **Step 3: Run** full `./gradlew :composeApp:testAndroidHostTest` and `./gradlew :androidApp:assembleDebug`.
- [ ] **Step 4: Commit** `git commit -m "Move onboarding, paywall, daily and year screens onto Quiet Ink"`

### Task 12: Verify, screenshots, version

**Files:** Modify `frontend/androidApp/build.gradle.kts:64-65`, `docs/store/screenshots/01-home.png`…`05-journal.png`, `docs/RELEASE.md`

- [ ] **Step 1: Run on an Android emulator** and walk Home, cast, reading, library (+filter), journal, settings, paywall in all 5 themes. Check each Review Focus item: IME with pinned CTA; card edges visible on Ink Night and Cinnabar Gold; library empty state; scripture-only sheet header; font scale 1.3× (`adb shell settings put system font_scale 1.3`, reset to 1.0 after).
- [ ] **Step 2: Fix** anything found, re-run tests and build, commit.
- [ ] **Step 3: Recapture** the five store screenshots (1080×2160, Ink Night as today) over the existing files.
- [ ] **Step 4: Bump** `versionCode = 130`, `versionName = "1.3.0"`; add a v1.3 section to `docs/RELEASE.md` noting the redesign and new screenshots.
- [ ] **Step 5: Commit** `git commit -m "Bump to 1.3.0 (130) with Quiet Ink screenshots"`
