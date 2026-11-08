import { existsSync, readdirSync, readFileSync } from "node:fs";
import { join, resolve } from "node:path";

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

/** Loads every set file in [dir] once per instance, keyed by set id. */
export function loadSiamSiSets(dir = resolve(__dirname, "../../data/siamsi")): Map<string, SiamSiSet> {
  const sets = new Map<string, SiamSiSet>();
  if (!existsSync(dir)) return sets;
  for (const file of readdirSync(dir).filter((f) => f.endsWith(".json"))) {
    const set = JSON.parse(readFileSync(join(dir, file), "utf8")) as SiamSiSet;
    sets.set(set.id, set);
  }
  return sets;
}

export function findStick(sets: Map<string, SiamSiSet>, setId: string, n: number): { set: SiamSiSet; stick: SiamSiStick } | null {
  const set = sets.get(setId);
  const stick = set?.sticks.find((s) => s.number === n);
  return set && stick ? { set, stick } : null;
}
