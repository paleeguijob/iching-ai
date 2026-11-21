# I Ching reading as a short message plus cards

## Why

Readings still feel like too much to read. Seekers want to scan the answer the way the Siam Si slip
lets them scan its topic cards, while keeping the hermit's voice.

## What the seeker sees

```
อาตมาเห็นกวาเจิ้น ...            ← one short paragraph in character, about 40–70 words

[ดวงเด่น]   what is going well     ← 1–2 sentences each, shown as cards like TopicsCard
[ควรระวัง]  what to watch out for
[คำแนะนำ]   what to do next
```

## Goals / non-goals

- Goal: shorter, scannable I Ching readings; same persona and safety rules.
- Non-goals: follow-ups (stay prose), Siam Si (already has topic cards), per-category headings.
- Anti-goal: users on the app version already on Play seeing raw `[ดวงเด่น]` markers.

## Approaches

- **Picked: one Gemini call, markers in the text, the app splits them into cards.** One request,
  streaming still works, the summary marker already works this way.
- Prompt only, one-line answer plus a short paragraph: cheaper, but still one block of text.
- Plain "ควรระวัง: ..." lines without app changes: fast, but no cards and doesn't match Siam Si.

## Design

- Backend: the reading request takes an optional `format: "cards"`. With it, the prompt asks for the
  short paragraph, then the three marked sections, then `[สรุป]` as today. Without it, the prompt
  is unchanged, so older app versions keep getting paragraphs. Markers in the seeker's question are
  sanitized like the existing ones.
- App: sends `format: "cards"` for I Ching readings. One parser splits the saved text into the
  paragraph and sections; it is used by the reading screen (while streaming too) and the journal.
  Text with no markers renders as today, so old journal entries are unchanged.
- Listen button: reads the paragraph, then each section with its heading spoken first.
- Share card and saved data: unchanged; the saved interpretation keeps the markers.

## Left to the builder

- Card styling (reuse the TopicsCard look), what shows while a section is half streamed,
  and what happens if the model drops a section (show what came, no empty card).
