# Home Promo Card Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A server-switched Premium promo card (layout P2) under the streak row on home, opening the existing paywall and dismissible per promo.

**Architecture:** `config/runtime.promo` in Firestore is parsed on the server, filtered by time and Premium in `status()`, and sent in `/status`. The app maps it to `AccountStatus.promo`, decides visibility with a pure `promoCard()` (dismissed ids live in `AppSettings`), and `HomeViewModel` exposes it to a new card on `HomeScreen`.

**Tech Stack:** TypeScript Cloud Functions (`node --test`), Kotlin Multiplatform + Compose, DataStore preferences, Koin.

**Spec:** `docs/superpowers/specs/2026-11-17-home-promo-card-design.md`

## Global Constraints

- Promo fields: `id` non-empty · `enabled` boolean · `startsAt`/`endsAt` ISO strings, `endsAt > startsAt` · `label` 1–20 chars · `title` 1–40 chars · `cta` 1–12 chars · `showEndDate` boolean. Anything missing, mistyped or too long = no promo.
- `/status` carries `promo: { id, label, title, cta, endsAt, showEndDate } | null`; non-null only when enabled, `startsAt <= now < endsAt`, and not Premium.
- App shows the card only when `status.promo != null`, `!status.premium`, `now < endsAtMillis`, and the id is not dismissed.
- Dismissed ids: at most 20, oldest dropped first.
- End-date label uses Bangkok time: label + `" · ถึง <d> <short month>"` (Thai numerals), e.g. `"โปรพิเศษ · ถึง ๒๐ พ.ย."`.
- CTA shown as `"<cta> ›"`. Close button contentDescription `"ปิดโปรนี้"`, touch target 48dp.
- Analytics: `promo_shown`, `promo_tapped`, `promo_dismissed`, each with param `promo_id`.
- Card look (P2): white `ZenColors.Card`, 1dp `outlineVariant` border, 14dp corners, no dark fill; 52dp `CinnabarRed` tile with "易" in `PaperCream`; placed below the streak row and campaign line.
- No new dependencies; no paywall changes; no popup.

## Review Focus

1. A promo ending at midnight (`endsAt = 2026-11-21T00:00+07:00`) must read "ถึง ๒๐ พ.ย.", not "๒๑" — Task 4 test `endDateIsTheLastDayShown`.
2. A buyer whose cached status just turned Premium must not see the card even if `promo` is still in that status — Task 4 test `premiumHidesTheCard`.
3. Home left open past `endsAt` must drop the card on the next resume — Task 5 test `cardDisappearsAfterEndOnResume`.
4. Dismissing the same id twice must not duplicate it or push out an older id — Task 3 test `dismissingTwiceKeepsOneEntry`.
5. A title of exactly 40 characters is accepted and 41 is rejected (Thai combining marks count as characters, as in JS `length`) — Task 1 test `titleLengthLimit`.

---

### Task 0: Commit the home layout E and streak work already in the tree

The working tree holds the approved home redesign (layout E, streak row ๑–๗) uncommitted. It must land before promo work so later diffs stay reviewable.

- [ ] **Step 1:** Run `cd frontend && ./gradlew :composeApp:testAndroidHostTest` — expected: BUILD SUCCESSFUL, no failures.
- [ ] **Step 2: Commit**

```bash
git add frontend/composeApp/src/commonMain/kotlin/com/iching/ai/feature/home/ frontend/composeApp/src/commonTest/kotlin/com/iching/ai/feature/home/
git commit -m "Lead home with today's hexagram and a numbered streak row"
```

---

### Task 1: Server — parse `promo` and send it in `/status`

**Files:**
- Create: `backend/functions/src/core/promo.ts`
- Modify: `backend/functions/src/core/quota.ts` (`RuntimeConfig`, `DEFAULT_CONFIG`, `mergeConfig`, `QuotaStatus`, `status`)
- Test: `backend/functions/test/core.test.ts` (new `describe("promo")`)

**Interfaces:**
- Produces: `interface Promo { id; enabled; startsAt: number; endsAt: number; label; title; cta; showEndDate }` · `parsePromo(raw: unknown): Promo | null` · `promoFor(p: Promo | null, premium: boolean, now: number): StatusPromo | null` · `type StatusPromo = { id; label; title; cta; endsAt: number; showEndDate }` · `RuntimeConfig.promo: Promo | null` (default `null`) · `QuotaStatus.promo: StatusPromo | null`

- [ ] **Step 1: Write failing tests** in `describe("promo")`, using a `valid` raw object (`id:"1111-2026"`, `enabled:true`, `startsAt:"2026-11-10T00:00:00+07:00"`, `endsAt:"2026-11-21T00:00:00+07:00"`, `label:"โปรพิเศษ"`, `title:"Premium รายปี ลด ๔๐%"`, `cta:"ดูโปร"`, `showEndDate:true`):
  - `parsesAValidPromo`: `parsePromo(valid)` deep-equals the same values with times as epoch ms.
  - `missingOrMistypedFieldIsNoPromo`: for each key, deleting it → `null`; `enabled:"yes"` → `null`; `label:""` → `null`.
  - `endBeforeStartIsNoPromo`: `endsAt` = `startsAt` → `null`.
  - `titleLengthLimit`: `"ก".repeat(40)` parses; `"ก".repeat(41)` → `null`. Same pattern for label 20/21 and cta 12/13.
  - `statusCarriesPromoOnlyInWindowAndForFree`: with `mergeConfig({ promo: valid })` and a free user, `status(...).promo` is non-null at `startsAt`, null at `startsAt - 1`, null at `endsAt`; null for a Premium user; null when `enabled:false`.
  - `noPromoConfiguredSendsNull`: `status(user, null, mergeConfig(undefined), now).promo === null`.
- [ ] **Step 2:** Run `cd backend/functions && npm test` — expected: FAIL, `parsePromo` not exported.
- [ ] **Step 3: Implement** `promo.ts` following `parseCampaign` in `core/campaign.ts` (reuse its ISO→ms rule; reject non-strings). In `quota.ts`: add `promo: null` to `DEFAULT_CONFIG`; in `mergeConfig` handle `key === "promo"` like `campaign` (`if (v !== undefined) out.promo = parsePromo(v)`); in `status()` set `promo: promoFor(cfg.promo, premium, now)`.
- [ ] **Step 4:** Run `npm test` and `npm run typecheck:core` — expected: all pass.
- [ ] **Step 5: Commit** — `git commit -m "Send a time-boxed promo to free accounts in /status"`

---

### Task 2: App data — read `promo` from `/status`

**Files:**
- Modify: `frontend/core/domain/src/commonMain/kotlin/com/iching/ai/domain/model/Account.kt`
- Modify: `frontend/core/data/src/commonMain/kotlin/com/iching/ai/data/api/HttpOracleApi.kt` (`StatusDto`)
- Test: `frontend/core/data/src/commonTest/kotlin/com/iching/ai/data/HttpOracleApiTest.kt`

**Interfaces:**
- Produces: `data class PromoStatus(val id: String, val label: String, val title: String, val cta: String, val endsAtMillis: Long, val showEndDate: Boolean)` · `AccountStatus.promo: PromoStatus? = null` · `@Serializable internal data class PromoDto(id, label, title, cta, endsAt: Long, showEndDate: Boolean = false)` · `StatusDto.promo: PromoDto? = null`

- [ ] **Step 1: Write failing tests** next to the campaign status tests:
  - `statusReadsPromo`: body `{"premium":false,"promo":{"id":"1111-2026","label":"โปรพิเศษ","title":"ลด","cta":"ดูโปร","endsAt":1795194000000,"showEndDate":true}}` → `PromoStatus("1111-2026","โปรพิเศษ","ลด","ดูโปร",1795194000000L,true)`.
  - `statusWithoutPromoIsNull`: `{"premium":false}` and `{"premium":false,"promo":null}` → `promo == null`.
- [ ] **Step 2:** Run `cd frontend && ./gradlew :core:data:testAndroidHostTest --tests '*HttpOracleApiTest*'` — expected: compile FAIL on `promo`.
- [ ] **Step 3: Implement** the DTO, the domain class and the mapping in `StatusDto.toDomain()`.
- [ ] **Step 4:** Re-run — expected: PASS.
- [ ] **Step 5: Commit** — `git commit -m "Read the home promo from /status"`

---

### Task 3: Settings — remember dismissed promo ids

**Files:**
- Modify: `frontend/core/domain/src/commonMain/kotlin/com/iching/ai/domain/model/Hexagram.kt` (`AppSettings`)
- Modify: `frontend/core/data/src/commonMain/kotlin/com/iching/ai/data/settings/DataStoreSettingsRepository.kt`
- Test: `frontend/core/data/src/androidHostTest/kotlin/com/iching/ai/data/SettingsPersistenceTest.kt`, new `frontend/core/domain/src/commonTest/kotlin/com/iching/ai/domain/DismissedPromosTest.kt`

**Interfaces:**
- Produces: `AppSettings.dismissedPromoIds: List<String> = emptyList()` (oldest first) · `fun AppSettings.withPromoDismissed(id: String): AppSettings` in `Hexagram.kt` · `const val MAX_DISMISSED_PROMOS = 20`

- [ ] **Step 1: Write failing tests**
  - `DismissedPromosTest.addsId`: `AppSettings().withPromoDismissed("a").dismissedPromoIds == listOf("a")`.
  - `DismissedPromosTest.keepsTheNewestTwenty`: dismissing `"p0".."p20"` leaves `"p1".."p20"`.
  - `DismissedPromosTest.dismissingTwiceKeepsOneEntry`: with 20 ids `p0..p19`, dismissing `"p5"` again leaves the list unchanged (still contains `p0`).
  - `SettingsPersistenceTest`: fresh repo → `dismissedPromoIds == emptyList()`; after `update { it.copy(dismissedPromoIds = listOf("a","b")) }` and reopen → `listOf("a","b")` in that order.
- [ ] **Step 2:** Run `./gradlew :core:domain:jvmTest :core:data:testAndroidHostTest` — expected: compile FAIL.
- [ ] **Step 3: Implement** the field and `withPromoDismissed` (append if absent, then `takeLast(MAX_DISMISSED_PROMOS)`). Persist as `stringPreferencesKey("dismissed_promos")` holding the ids joined by `"\n"` (ids never contain newlines; order is kept, unlike a string-set key); missing key → empty list.
- [ ] **Step 4:** Re-run — expected: PASS.
- [ ] **Step 5: Commit** — `git commit -m "Remember which home promos were closed"`

---

### Task 4: App logic — decide and word the card

**Files:**
- Create: `frontend/composeApp/src/commonMain/kotlin/com/iching/ai/feature/home/PromoCard.kt`
- Modify: `frontend/composeApp/src/commonMain/kotlin/com/iching/ai/ui/ThaiDates.kt`
- Test: `frontend/composeApp/src/commonTest/kotlin/com/iching/ai/feature/home/PromoCardTest.kt`

**Interfaces:**
- Consumes: `PromoStatus`, `AccountStatus.promo` (Task 2)
- Produces: `data class PromoCard(val id: String, val label: String, val title: String, val cta: String)` · `fun promoCard(status: AccountStatus, dismissed: List<String>, nowMillis: Long): PromoCard?` · `formatThaiShortDate(epochMillis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault())` (new default param; existing callers unchanged)

- [ ] **Step 1: Write failing tests** with `promo = PromoStatus("1111-2026","โปรพิเศษ","Premium รายปี ลด ๔๐%","ดูโปร", endsAt, showEndDate = true)` where `endsAt = 2026-11-21T00:00+07:00` in ms (`1795194000000`) and `now = endsAt - 86_400_000`:
  - `showsTheCardForFree`: → `PromoCard("1111-2026","โปรพิเศษ · ถึง ๒๐ พ.ย.","Premium รายปี ลด ๔๐%","ดูโปร ›")`.
  - `endDateIsTheLastDayShown`: same as above — the label says ๒๐, the day before the exclusive end.
  - `noEndDateWhenTurnedOff`: `showEndDate = false` → label `"โปรพิเศษ"`.
  - `noPromoNoCard`, `premiumHidesTheCard` (`status.premium = true` with promo set), `dismissedHidesTheCard`, `aNewIdShowsAfterTheOldWasDismissed` (dismissed `["1010-2026"]`), `pastTheEndOnThisDeviceHidesTheCard` (`now = endsAt`).
- [ ] **Step 2:** Run `./gradlew :composeApp:testAndroidHostTest --tests '*PromoCardTest*'` — expected: compile FAIL.
- [ ] **Step 3: Implement** `promoCard()`; the end date is `formatThaiShortDate(endsAtMillis - 1, TimeZone.of("Asia/Bangkok"))`.
- [ ] **Step 4:** Re-run — expected: PASS.
- [ ] **Step 5: Commit** — `git commit -m "Decide when the home promo card shows"`

---

### Task 5: HomeViewModel — expose the card and log it

**Files:**
- Modify: `frontend/composeApp/src/commonMain/kotlin/com/iching/ai/feature/home/HomeViewModel.kt`
- Modify: `frontend/composeApp/src/commonMain/kotlin/com/iching/ai/di/AppModule.kt:103` — replace `viewModelOf(::HomeViewModel)` with `viewModel { HomeViewModel(get(), get(), get(), get(), get(), get()) }`, because `viewModelOf` would try to inject the `() -> Long` clock and ignore its default
- Modify: constructor calls in `HomeViewModelPreCastTest.kt:96`, `HomeViewModelCampaignTest.kt:87`
- Test: new `frontend/composeApp/src/commonTest/kotlin/com/iching/ai/feature/home/HomeViewModelPromoTest.kt`

**Interfaces:**
- Consumes: `promoCard()` (Task 4), `AppSettings.withPromoDismissed` (Task 3), `UpdateSettingsUseCase`
- Produces: constructor param `private val updateSettings: UpdateSettingsUseCase` (appended last) · `val promoCard: StateFlow<PromoCard?>` · `fun onPromoShown(id: String)` · `fun onPromoTapped(id: String)` · `fun onPromoDismissed(id: String)`

- [ ] **Step 1: Write failing tests** (build the VM like `HomeViewModelCampaignTest.viewModel`, with an in-memory `SettingsRepository` backed by a `MutableStateFlow(AppSettings())` and a recording `log`):
  - `showsThePromoFromStatus`: status with promo → `promoCard.value?.id == "1111-2026"`.
  - `dismissHidesAndRemembers`: `onPromoDismissed("1111-2026")` → `promoCard.value == null`, settings hold the id, log has `promo_dismissed` with `promo_id`.
  - `shownIsLoggedOnce`: two `onPromoShown` calls → one `promo_shown`.
  - `tapIsLogged`: `onPromoTapped` → `promo_tapped`.
  - `cardDisappearsAfterEndOnResume`: inject a clock (`nowMillis: () -> Long` constructor param with default `{ Clock.System.now().toEpochMilliseconds() }`, appended after `updateSettings`), advance it past `endsAt`, call `onResumed()` → `promoCard.value == null`.
- [ ] **Step 2:** Run `./gradlew :composeApp:testAndroidHostTest --tests '*HomeViewModelPromoTest*'` — expected: compile FAIL.
- [ ] **Step 3: Implement.** `promoCard` = `combine(session.status, observeSettings().map { it.dismissedPromoIds }, now) { … promoCard(…) }` with `now` a `MutableStateFlow` refreshed in `onResumed()` alongside `today`; use `.onStart { emit(emptyList()) }` on the dismissed flow so a settings source that has not emitted yet does not hold the card back. Replace the existing private `nowMillis()` with the injected clock.
- [ ] **Step 4:** Run `./gradlew :composeApp:testAndroidHostTest` — expected: whole suite PASS.
- [ ] **Step 5: Commit** — `git commit -m "Show and log the home promo card"`

---

### Task 6: HomeScreen — draw the P2 card and verify on a device

**Files:**
- Modify: `frontend/composeApp/src/commonMain/kotlin/com/iching/ai/feature/home/HomeScreen.kt`

**Interfaces:**
- Consumes: `HomeViewModel.promoCard`, `onPromoShown/Tapped/Dismissed` (Task 5), existing `onOpenPaywall`

- [ ] **Step 1: Implement** `@Composable private fun PromoCardRow(card: PromoCard, onClick: () -> Unit, onDismiss: () -> Unit)` per Global Constraints, placed after the campaign line in the scrolling column with `ZenSpacing.md` above. `LaunchedEffect(card.id) { viewModel.onPromoShown(card.id) }`; tap → `onPromoTapped` then `onOpenPaywall()`; × → `onPromoDismissed`. Label `labelSmall`/`CinnabarRed`, title `titleSmall`/`InkBlack` max 2 lines, cta `labelMedium`/`CinnabarRed`.
- [ ] **Step 2:** Run `./gradlew :composeApp:testAndroidHostTest :androidApp:assembleDebug` — expected: BUILD SUCCESSFUL.
- [ ] **Step 3: Verify on the emulator** against the dev Firebase project with `config/runtime.promo` set to the Task 1 `valid` object (window covering today): card shows under the streak row · tap opens the paywall · back, tap × → card gone · force-stop and relaunch → still gone · change `id` → card returns · 720×1280 screen: ask bar still fully visible. Screenshot each.
- [ ] **Step 4: Commit** — `git commit -m "Draw the promo card under the streak row"`

---

### Task 7: Release checklist

**Files:**
- Modify: `docs/RELEASE.md` (new subsection under section 14, v1.4)

- [ ] **Step 1:** Add a "เปิดโปรบนหน้าแรก" checklist: set the Play Console offer → make the matching RevenueCat offering current → check the paywall price on a device → write `config/runtime.promo` with `enabled:false` → check `title` matches the paywall price → flip `enabled:true` → after the end date, switch the RevenueCat offering back. Note that the backend must be deployed (`npm run deploy`) before the first promo.
- [ ] **Step 2: Commit** — `git commit -m "Add the home promo launch checklist"`
