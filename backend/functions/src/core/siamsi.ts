import { existsSync, readdirSync, readFileSync } from "node:fs";
import { join, resolve } from "node:path";
import type { Lang } from "./lang";

/** One slip. Same shape as the app's set files, which scripts/copy-data.mjs copies into data/siamsi/. */
export interface SiamSiStick {
  number: number;
  grade_orig: string | null;
  level: "excellent" | "good" | "fair" | "caution";
  label_orig: string | null;
  poem_orig: string[];
  poem_th: string[];
  story: string;
  summary: string;
  topics: Record<string, string>;
  keywords: string[];
}

export interface SiamSiSet {
  id: string;
  name: string;
  count: number;
  source: { title: string; note: string };
  sticks: SiamSiStick[];
  /** English file ({id}.en.json), kept only when it covers every stick of the set (an empty story is allowed: the Thai sets have none). */
  en?: SiamSiSetEn | null;
}

export interface SiamSiStickEn {
  number: number;
  poem_en: string[];
  story: string;
  summary: string;
  topics: Record<string, string>;
  keywords: string[];
}

export interface SiamSiSetEn {
  id: string;
  version: number;
  name: string;
  source: { note: string };
  sticks: SiamSiStickEn[];
}

export const LEVEL_TH: Record<SiamSiStick["level"], string> = {
  excellent: "ดีมาก",
  good: "ดี",
  fair: "ปานกลาง",
  caution: "ควรระวัง",
};

export const TOPIC_TH: [string, string][] = [
  ["work", "การงาน"],
  ["money", "การเงิน"],
  ["love", "ความรัก"],
  ["health", "สุขภาพ"],
  ["travel", "การเดินทาง"],
  ["lost", "ของหาย"],
  ["dealings", "คดีความ/การติดต่อ"],
];

export const LEVEL_EN: Record<SiamSiStick["level"], string> = {
  excellent: "Very good",
  good: "Good",
  fair: "Fair",
  caution: "Take care",
};

export const TOPIC_EN: [string, string][] = [
  ["work", "Work"],
  ["money", "Money"],
  ["love", "Love"],
  ["health", "Health"],
  ["travel", "Travel"],
  ["lost", "Lost things"],
  ["dealings", "Disputes and dealings"],
];

const isCompleteEnglish = (set: SiamSiSet, en: SiamSiSetEn | null): en is SiamSiSetEn => {
  if (!en || en.id !== set.id || !en.name || !Array.isArray(en.sticks)) return false;
  const byNumber = new Map(en.sticks.map((s) => [s.number, s]));
  return set.sticks.every((s) => {
    const e = byNumber.get(s.number);
    return !!e && e.poem_en?.length > 0 && typeof e.story === "string" && !!e.summary && !!e.topics && Array.isArray(e.keywords);
  });
};

/** Loads every set file in [dir] once per instance, keyed by set id, with its {id}.en.json when complete. */
export function loadSiamSiSets(dir = resolve(__dirname, "../../data/siamsi")): Map<string, SiamSiSet> {
  const sets = new Map<string, SiamSiSet>();
  if (!existsSync(dir)) return sets;
  for (const file of readdirSync(dir).filter((f) => f.endsWith(".json") && !f.endsWith(".en.json"))) {
    const set = JSON.parse(readFileSync(join(dir, file), "utf8")) as SiamSiSet;
    const enFile = join(dir, file.replace(/\.json$/, ".en.json"));
    let en: SiamSiSetEn | null = null;
    if (existsSync(enFile)) {
      try {
        en = JSON.parse(readFileSync(enFile, "utf8")) as SiamSiSetEn;
      } catch {
        en = null;
      }
    }
    set.en = isCompleteEnglish(set, en) ? en : null;
    sets.set(set.id, set);
  }
  return sets;
}

/**
 * A slip in [lang]. English replaces the text like the app does (poem_th ← poem_en, name, story, summary, topics,
 * keywords); without a complete English file for the set the Thai slip comes back. [lang] in the result is the one
 * actually used.
 */
export function findStick(
  sets: Map<string, SiamSiSet>,
  setId: string,
  n: number,
  lang: Lang = "th",
): { set: SiamSiSet; stick: SiamSiStick; lang: Lang } | null {
  const set = sets.get(setId);
  const stick = set?.sticks.find((s) => s.number === n);
  if (!set || !stick) return null;
  const en = lang === "en" ? set.en : null;
  const e = en?.sticks.find((s) => s.number === n);
  if (!en || !e) return { set, stick, lang: "th" };
  return {
    set: { ...set, name: en.name, source: { ...set.source, note: en.source?.note ?? set.source.note } },
    stick: { ...stick, poem_th: e.poem_en, story: e.story, summary: e.summary, topics: e.topics, keywords: e.keywords },
    lang: "en",
  };
}
