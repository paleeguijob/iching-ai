# I Ching Reading Cards Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** I Ching readings show one short hermit paragraph plus three cards (ดวงเด่น / ควรระวัง / คำแนะนำ), while app versions already on Play keep getting paragraphs.

**Architecture:** The app sends `format: "cards"` on `/divine`. With that flag the backend's prompt asks for a short paragraph followed by marked sections. The marked text streams and is saved unchanged. One domain parser splits it for the screen, the journal and speech, and text with no markers renders as before.

**Tech Stack:** Firebase Functions (TypeScript, node:test), Kotlin Multiplatform + Compose.

**Spec:** `docs/superpowers/specs/2026-11-20-iching-reading-cards-design.md`

## Global Constraints

- Section markers, in this order, exactly: `[ดวงเด่น]`, `[ควรระวัง]`, `[คำแนะนำ]`; the summary stays `[สรุป]` (`SUMMARY_MARKER`), last.
- Paragraph about 40–70 words; each section 1–2 sentences; persona, safety rules 1–7 and the AI-honesty rule unchanged.
- No `format` (or any other value) → the prompt is byte-for-byte what `buildReadingPrompt` builds today.
- Follow-ups and Siam Si prompts unchanged.
- Saved `interpretation` / `aiText` keep the markers; nothing migrates old readings.

## Review Focus

- A seeker types `[ควรระวัง]` in their question → it must not reach the prompt as a marker (sanitized like `[สรุป]`).
- The stream ends mid-marker (`…ดี [ควร`) → no half marker shows on screen.
- The model skips a section or adds text before the first marker only → show what came; no empty card.
- An old journal entry (plain prose, several paragraphs) → renders exactly as today, no cards.
- Listen on a card reading → speaks the headings as words, never the brackets.

---

### Task 1: Backend cards prompt behind `format: "cards"`

**Files:**
- Modify: `backend/functions/src/core/prompt.ts` (`sanitizeQuestion`, `buildReadingPrompt`, new constants)
- Modify: `backend/functions/src/core/api.ts:403-408` (pass the flag)
- Test: `backend/functions/test/core.test.ts` (`describe("prompt")`)

**Interfaces:**
- Produces: `export const READING_SECTIONS = ["ดวงเด่น", "ควรระวัง", "คำแนะนำ"] as const;` and
  `buildReadingPrompt(question, cast, category = null, yearContext?: string, cards = false): Prompt`.

- [ ] **Step 1: Write failing tests**

```ts
it("cards format asks for the three marked sections before the summary", () => {
  const p = buildReadingPrompt("งานใหม่ดีไหม", cast, "work", undefined, true);
  const all = p.systemInstruction + p.userMessage;
  for (const s of READING_SECTIONS) assert.ok(all.includes(`[${s}]`));
  assert.ok(p.userMessage.indexOf("[คำแนะนำ]") < p.userMessage.lastIndexOf(SUMMARY_MARKER));
  assert.ok(p.systemInstruction.startsWith(PERSONA));
});
it("without cards the reading prompt is unchanged", () => {
  const p = buildReadingPrompt("งานใหม่ดีไหม", cast, "work");
  assert.equal(p.systemInstruction, PERSONA);
  assert.ok(!p.userMessage.includes("[ดวงเด่น]"));
});
it("strips section markers from questions", () => {
  assert.ok(!sanitizeQuestion("ช่วยดู [ควรระวัง] หน่อย").includes("[ควรระวัง]"));
});
```

- [ ] **Step 2: Run and see them fail.** Run `npm test` in `backend/functions`. They should fail because `READING_SECTIONS` is not exported.
- [ ] **Step 3: Implement.**
  - Add `ดวงเด่น|ควรระวัง|คำแนะนำ` to the marker regex in `sanitizeQuestion`.
  - Add a `CARDS_RULES` string to `PERSONA`'s system text when `cards` is true. It tells the model to write one short paragraph of about 40–70 words, then each marker on its own line followed by 1–2 sentences, with no other markers, and it overrides the 2-paragraph format line.
  - In `api.ts`, pass `b.format === "cards"`. `b` is the request body already in scope; for a `readingId` retry the flag comes from that request too.
- [ ] **Step 4: Run `npm test`.** Expected: all pass.
- [ ] **Step 5: Commit** with the message `Ask for reading cards when the app supports them`.

### Task 2: Domain parser for card readings

**Files:**
- Create: `frontend/core/domain/src/commonMain/kotlin/com/iching/ai/domain/oracle/ReadingSections.kt`
- Test: `frontend/core/domain/src/commonTest/kotlin/com/iching/ai/domain/oracle/ReadingSectionsTest.kt`

**Interfaces:**
- Produces:
  - `data class ReadingSection(val label: String, val text: String)`
  - `data class ReadingParts(val message: String, val sections: List<ReadingSection>)`
  - `fun parseReading(text: String): ReadingParts`. Labels come without brackets. A trailing partial `[…` with no `]` is dropped. Sections with blank text are dropped. Text with no markers becomes `ReadingParts(text.trim(), emptyList())`.
  - `fun readingForSpeech(text: String): String`. It returns the message, then `"<label>. <text>"` per section, joined by `"\n"`.

- [ ] **Step 1: Write failing tests:**
  - `plainProseHasNoSections`: input is two paragraphs, and the output message equals the input trimmed, with no sections.
  - `splitsMessageAndThreeSections`: input is `"ข้อความ\n[ดวงเด่น] ก\n[ควรระวัง] ข\n[คำแนะนำ] ค"`. The output message is `ข้อความ` and the sections are the three labels with `ก`, `ข`, `ค`.
  - `dropsAHalfMarkerWhileStreaming`: input is `"ข้อความ\n[ดวงเด่น] ก\n[ควร"`. The output has one section, and no output text contains `[`.
  - `skipsEmptyOrMissingSections`: input is `"ข้อความ\n[ดวงเด่น]\n[คำแนะนำ] ค"`. The output has only `คำแนะนำ`.
  - `speechNamesHeadingsWithoutBrackets`: `readingForSpeech` contains `ควรระวัง. ข` and contains no `[`.
- [ ] **Step 2: Run and see them fail.** Run `./gradlew :core:domain:jvmTest` in `frontend`. Expected: compile error.
- [ ] **Step 3: Implement** with a regex over `\[(ดวงเด่น|ควรระวัง|คำแนะนำ)\]`, and strip markup with the existing `stripMarkup`.
- [ ] **Step 4: Run `./gradlew :core:domain:jvmTest`.** Expected: pass.
- [ ] **Step 5: Commit** with the message `Split card readings into message and sections`.

### Task 3: App asks for cards

**Files:**
- Modify: `frontend/core/data/src/commonMain/kotlin/com/iching/ai/data/api/HttpOracleApi.kt:229-236` (`divine`, `interpretAgain`)
- Test: `frontend/core/data/src/commonTest/kotlin/com/iching/ai/data/HttpOracleApiTest.kt`

- [ ] **Step 1: Write a failing test.** Name it `divineAndInterpretAgainAskForCards`. It captures each request body the way `sendFeedbackPostsMessageVersionAndPlatform` does, and asserts that both `/divine` bodies contain `"format":"cards"`.
- [ ] **Step 2: Run it and see it fail.** Run `./gradlew :core:data:testAndroidHostTest`. If that task name does not exist, run `./gradlew :core:data:tasks --all | grep -i test` and use the host test task it lists.
- [ ] **Step 3: Implement.** Add `put("format", "cards")` to both builders. Leave `followUp` and `interpretSiamSi` alone.
- [ ] **Step 4: Run the test again.** Expected: pass.
- [ ] **Step 5: Commit** with the message `Ask the server for reading cards`.

### Task 4: Show cards and speak them

**Files:**
- Modify: `frontend/composeApp/src/commonMain/kotlin/com/iching/ai/feature/reading/ReadingContent.kt` (`OracleSheet`, around line 155)
- Modify: `frontend/composeApp/src/commonMain/kotlin/com/iching/ai/feature/divination/DivinationScreen.kt:289` (Listen text)

**Interfaces:**
- Consumes: `parseReading`, `readingForSpeech`, `ReadingSection` from Task 2.

- [ ] **Step 1: Change `OracleSheet`.**
  - Parse `text` once with `remember(text) { parseReading(text) }`.
  - Show `parts.message` through `TypewriterText` exactly as it does today.
  - Below that, render `ReadingSectionsCard(parts.sections)`, a private composable that copies the row layout of `TopicsCard` in `SiamSiSlip.kt:360`: a 92.dp label column, `labelMedium` InkBlack for the label, `bodySmall` InkSoft for the text, and `ZenCardDivider` between rows, with no dot.
  - Only show it when `sections` is not empty, and wrap it in `AnimatedVisibility` so cards fade in as they arrive.
  - The journal uses `OracleSheet` too, so it gets cards with no change of its own.
- [ ] **Step 2: Fix Listen.** In `DivinationScreen.kt:289`, pass `text = readingForSpeech(state.interpretation)`.
- [ ] **Step 3: Build.** Run `./gradlew :androidApp:assembleDebug`. Expected: BUILD SUCCESSFUL.
- [ ] **Step 4: Check by hand.**
  - Deploy Task 1, then run the debug app on an emulator.
  - Cast a reading. Expected: a short paragraph, then three cards appearing as the text streams, with no `[` visible.
  - Open an old journal entry. Expected: it shows as plain paragraphs.
  - Tap Listen. Expected: the voice reads the headings as words.
- [ ] **Step 5: Commit** with the message `Show I Ching readings as a message and cards`.

## Rollout

1. Deploy functions after Task 1. This is safe for the current Play version, which sends no `format`.
2. Ship a new app build containing Tasks 2–4.
