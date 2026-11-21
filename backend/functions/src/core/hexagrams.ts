import { existsSync, readFileSync } from "node:fs";
import { resolve } from "node:path";
import type { Lang } from "./lang";

/**
 * One of the 64 hexagrams. [pattern] is read bottom to top (index 0 = line 1), "1" = yang.
 * Same shape as the app's hexagrams.json, which scripts/copy-data.mjs copies into data/.
 */
export interface Hexagram {
  id: number;
  nameCn: string;
  pinyin: string;
  nameTh: string;
  pattern: string;
  keywords: string[];
  judgment: string;
  image: string;
  description: string;
  lines: string[];
  allLines?: string | null;
  /** Pre-written message for the daily hexagram and its push. Not part of the reading prompt. */
  dailyMessage: string;
  /** Pre-written new-year reading for the Chinese New Year campaign. Not part of the reading prompt. */
  yearMessage: YearMessage;
  /** Set to "en" on a hexagram produced by Scripture.forLang("en"): its text fields are English. */
  lang?: Lang;
}

/** One entry of hexagrams.en.json. Same shape as the app's overlay file. */
export interface HexagramEn {
  id: number;
  nameEn: string;
  meaningEn: string;
  keywordsEn: string[];
  judgmentEn: string;
  imageEn: string;
  descriptionEn: string;
  linesEn: string[];
  allLinesEn: string | null;
  dailyMessageEn: string;
  yearMessageEn: YearMessage | null;
}

export interface HexagramsEnFile {
  version: number;
  trigrams?: Record<string, { natureEn: string }>;
  hexagrams: HexagramEn[];
}

/** Year-ahead reflection for one hexagram. Limits: overview 200, work/love/money 80, blessing 60 characters. */
export interface YearMessage {
  overview: string;
  work: string;
  love: string;
  money: string;
  blessing: string;
}

export const TRIGRAMS: Record<string, { symbol: string; nameCn: string; natureTh: string; natureEn: string }> = {
  "111": { symbol: "☰", nameCn: "乾", natureTh: "ฟ้า", natureEn: "Heaven" },
  "110": { symbol: "☱", nameCn: "兌", natureTh: "ทะเลสาบ", natureEn: "Lake" },
  "101": { symbol: "☲", nameCn: "離", natureTh: "ไฟ", natureEn: "Fire" },
  "100": { symbol: "☳", nameCn: "震", natureTh: "ฟ้าร้อง", natureEn: "Thunder" },
  "011": { symbol: "☴", nameCn: "巽", natureTh: "ลม", natureEn: "Wind" },
  "010": { symbol: "☵", nameCn: "坎", natureTh: "น้ำ", natureEn: "Water" },
  "001": { symbol: "☶", nameCn: "艮", natureTh: "ภูเขา", natureEn: "Mountain" },
  "000": { symbol: "☷", nameCn: "坤", natureTh: "ดิน", natureEn: "Earth" },
};

export const lowerTrigram = (h: Hexagram) => TRIGRAMS[h.pattern.substring(0, 3)];
export const upperTrigram = (h: Hexagram) => TRIGRAMS[h.pattern.substring(3, 6)];
export const shortNameTh = (h: Hexagram) => h.nameTh.split(" —")[0].trim();
export const meaningTh = (h: Hexagram) => {
  const i = h.nameTh.indexOf("— ");
  return (i >= 0 ? h.nameTh.substring(i + 2) : h.nameTh).trim();
};

/** Nature of a trigram in the language of [h]. */
export const trigramNature = (t: { natureTh: string; natureEn: string }, h: Hexagram) => (h.lang === "en" ? t.natureEn : t.natureTh);

/**
 * Same text fields as the app: nameTh becomes "{nameEn} — {meaningEn}", the rest come from the *En fields.
 * Returns null when the file does not cover all 64 hexagrams with complete text (never a half-English scripture).
 */
const YEAR_FIELDS = ["overview", "work", "love", "money", "blessing"] as const;
const completeYear = (m: YearMessage | null | undefined): m is YearMessage =>
  !!m && YEAR_FIELDS.every((k) => typeof m[k] === "string" && m[k].trim().length > 0);

function overlayEnglish(thai: Hexagram[], en: HexagramsEnFile | null | undefined): Hexagram[] | null {
  if (!en || !Array.isArray(en.hexagrams)) return null;
  const byId = new Map(en.hexagrams.map((e) => [e.id, e]));
  const out: Hexagram[] = [];
  for (const h of thai) {
    const e = byId.get(h.id);
    if (
      !e || !e.nameEn || !e.meaningEn || !e.judgmentEn || !e.imageEn || !e.descriptionEn || !e.dailyMessageEn ||
      !Array.isArray(e.keywordsEn) || !Array.isArray(e.linesEn) || e.linesEn.length !== 6 || e.linesEn.some((l) => !l) ||
      (h.allLines && !e.allLinesEn) ||
      (h.yearMessage && !completeYear(e.yearMessageEn))
    ) {
      return null;
    }
    out.push({
      ...h,
      nameTh: `${e.nameEn} — ${e.meaningEn}`,
      keywords: e.keywordsEn,
      judgment: e.judgmentEn,
      image: e.imageEn,
      description: e.descriptionEn,
      lines: e.linesEn,
      allLines: e.allLinesEn,
      dailyMessage: e.dailyMessageEn,
      yearMessage: e.yearMessageEn as YearMessage,
      lang: "en",
    });
  }
  return out;
}

export class Scripture {
  private readonly byId = new Map<number, Hexagram>();
  private readonly byPattern = new Map<string, Hexagram>();
  private english: Scripture | null | undefined;

  constructor(hexagrams: Hexagram[], private readonly en: HexagramsEnFile | null = null) {
    if (hexagrams.length !== 64) throw new Error(`Scripture must contain 64 hexagrams, found ${hexagrams.length}`);
    for (const h of hexagrams) {
      if (h.pattern.length !== 6 || h.lines.length !== 6) throw new Error(`Hexagram ${h.id} is malformed`);
      this.byId.set(h.id, h);
      this.byPattern.set(h.pattern, h);
    }
    if (this.byPattern.size !== 64) throw new Error("Hexagram patterns must be unique");
  }

  /** The scripture in [lang]; English falls back to this (Thai) one unless the English file is complete. */
  forLang(lang: Lang): Scripture {
    if (lang !== "en") return this;
    if (this.english === undefined) {
      const merged = overlayEnglish([...this.byId.values()], this.en);
      this.english = merged ? new Scripture(merged) : null;
    }
    return this.english ?? this;
  }

  get(id: number): Hexagram {
    const h = this.byId.get(id);
    if (!h) throw new Error(`No hexagram #${id}`);
    return h;
  }

  fromPattern(pattern: string): Hexagram {
    const h = this.byPattern.get(pattern);
    if (!h) throw new Error(`No hexagram with pattern ${pattern}`);
    return h;
  }
}

let cached: Scripture | null = null;

/**
 * Loads data/hexagrams.json once per instance, with data/hexagrams.en.json beside it when that exists
 * (a missing or unreadable English file leaves English requests on Thai).
 */
export function loadScripture(
  path = resolve(__dirname, "../../data/hexagrams.json"),
  enPath = path.replace(/\.json$/, ".en.json"),
): Scripture {
  if (!cached) {
    let en: HexagramsEnFile | null = null;
    if (existsSync(enPath)) {
      try {
        en = JSON.parse(readFileSync(enPath, "utf8")) as HexagramsEnFile;
      } catch {
        en = null;
      }
    }
    cached = new Scripture(JSON.parse(readFileSync(path, "utf8")) as Hexagram[], en);
  }
  return cached;
}
