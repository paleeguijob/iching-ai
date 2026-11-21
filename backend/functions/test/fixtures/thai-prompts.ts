import { resolve } from "node:path";
import { resolveLines } from "../../src/core/cast";
import { loadScripture } from "../../src/core/hexagrams";
import { buildFollowUpPrompt, buildReadingPrompt, buildSiamSiPrompt, Prompt, scriptureContext, siamsiContext } from "../../src/core/prompt";
import { findStick, loadSiamSiSets } from "../../src/core/siamsi";

/** The Thai prompts the snapshot covers. Called with no language argument, which must stay Thai. */
export function thaiPrompts(): Record<string, Prompt> {
  const scripture = loadScripture(resolve(__dirname, "../../data/hexagrams.json"));
  const sets = loadSiamSiSets(resolve(__dirname, "../../data/siamsi"));
  const cast = resolveLines(scripture, [9, 7, 8, 7, 8, 7]);
  const still = resolveLines(scripture, [7, 8, 7, 8, 7, 8]);
  const allSix = resolveLines(scripture, [9, 9, 9, 9, 9, 9]);
  const slip = findStick(sets, "guanyin100", 17)!;
  const jiazi = findStick(sets, "jiazi60", 5)!;
  const first = "ข้อความ\n[ดวงเด่น] ก\n[ควรระวัง] ข\n[คำแนะนำ] ค";
  return {
    reading: buildReadingPrompt("งานใหม่ดีไหม", cast, "work"),
    readingCards: buildReadingPrompt("งานใหม่ดีไหม", cast, "work", undefined, true),
    readingYearCards: buildReadingPrompt("ปีนี้เป็นอย่างไร", allSix, "newyear", "ctx ปีใหม่", true),
    readingEmpty: buildReadingPrompt("", still, null),
    followUp: buildFollowUpPrompt("q", scriptureContext(cast), first, [{ q: "x", a: first }], "แล้วควรเริ่มเมื่อไร"),
    followUpEmpty: buildFollowUpPrompt("", scriptureContext(still), "คำทำนาย", [], "ต่อ"),
    siamsi: buildSiamSiPrompt("งานใหม่ดีไหม", slip.set, slip.stick, "work"),
    siamsiEmpty: buildSiamSiPrompt("", jiazi.set, jiazi.stick, null),
    siamsiFollowUp: buildFollowUpPrompt("q", siamsiContext(slip.set, slip.stick), "คำแปล", [], "แล้วควรเริ่มเมื่อไร"),
  };
}
