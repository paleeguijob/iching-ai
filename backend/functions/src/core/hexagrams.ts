import { readFileSync } from "node:fs";
import { resolve } from "node:path";

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
}

export const TRIGRAMS: Record<string, { symbol: string; nameCn: string; natureTh: string }> = {
  "111": { symbol: "☰", nameCn: "乾", natureTh: "ฟ้า" },
  "110": { symbol: "☱", nameCn: "兌", natureTh: "ทะเลสาบ" },
  "101": { symbol: "☲", nameCn: "離", natureTh: "ไฟ" },
  "100": { symbol: "☳", nameCn: "震", natureTh: "ฟ้าร้อง" },
  "011": { symbol: "☴", nameCn: "巽", natureTh: "ลม" },
  "010": { symbol: "☵", nameCn: "坎", natureTh: "น้ำ" },
  "001": { symbol: "☶", nameCn: "艮", natureTh: "ภูเขา" },
  "000": { symbol: "☷", nameCn: "坤", natureTh: "ดิน" },
};

export const lowerTrigram = (h: Hexagram) => TRIGRAMS[h.pattern.substring(0, 3)];
export const upperTrigram = (h: Hexagram) => TRIGRAMS[h.pattern.substring(3, 6)];
export const shortNameTh = (h: Hexagram) => h.nameTh.split(" —")[0].trim();
export const meaningTh = (h: Hexagram) => {
  const i = h.nameTh.indexOf("— ");
  return (i >= 0 ? h.nameTh.substring(i + 2) : h.nameTh).trim();
};

export class Scripture {
  private readonly byId = new Map<number, Hexagram>();
  private readonly byPattern = new Map<string, Hexagram>();

  constructor(hexagrams: Hexagram[]) {
    if (hexagrams.length !== 64) throw new Error(`Scripture must contain 64 hexagrams, found ${hexagrams.length}`);
    for (const h of hexagrams) {
      if (h.pattern.length !== 6 || h.lines.length !== 6) throw new Error(`Hexagram ${h.id} is malformed`);
      this.byId.set(h.id, h);
      this.byPattern.set(h.pattern, h);
    }
    if (this.byPattern.size !== 64) throw new Error("Hexagram patterns must be unique");
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

/** Loads data/hexagrams.json once per instance. */
export function loadScripture(path = resolve(__dirname, "../../data/hexagrams.json")): Scripture {
  if (!cached) cached = new Scripture(JSON.parse(readFileSync(path, "utf8")) as Hexagram[]);
  return cached;
}
