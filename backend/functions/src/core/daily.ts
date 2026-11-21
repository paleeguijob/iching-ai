import { Scripture, shortNameTh } from "./hexagrams";
import type { Lang } from "./lang";

export const DAILY_TOPICS: Record<Lang, string> = { th: "daily-hexagram-th", en: "daily-hexagram-en" };
/** The Thai topic, as before. */
export const DAILY_TOPIC = DAILY_TOPICS.th;
const DAY_MS = 86_400_000;

/** Days since 1970-01-01 at UTC+7. */
export function bangkokEpochDay(now: number): number {
  return Math.floor((now + 7 * 3600_000) / DAY_MS);
}

/** Keep in step with DailyHexagram.kt in the app: both sides must pick the same hexagram. */
export function dailyHexagramId(epochDay: number): number {
  return ((((epochDay * 37 + 11) % 64) + 64) % 64) + 1;
}

export interface DailyPush {
  topic: string;
  title: string;
  body: string;
  hexagramId: number;
  ttlMs: number;
}

/**
 * The morning push in [lang]. Null for English when the English scripture is incomplete (it would fall back
 * to Thai): Thai text is never sent to the English topic.
 */
export function buildDailyPush(now: number, scripture: Scripture, lang: Lang = "th"): DailyPush | null {
  const s = scripture.forLang(lang);
  if (lang === "en" && s.get(1).lang !== "en") return null;
  const h = s.get(dailyHexagramId(bangkokEpochDay(now)));
  return {
    topic: DAILY_TOPICS[lang],
    title: lang === "en" ? `${h.nameCn} ${h.pinyin} · ${shortNameTh(h)}` : `${h.nameCn} ${shortNameTh(h)}`,
    body: h.dailyMessage,
    hexagramId: h.id,
    // Dropped rather than delivered late: a morning message makes no sense at night.
    ttlMs: 12 * 3600_000,
  };
}
