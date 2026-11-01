import { Scripture, shortNameTh } from "./hexagrams";

export const DAILY_TOPIC = "daily-hexagram-th";
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

export function buildDailyPush(now: number, scripture: Scripture): DailyPush {
  const h = scripture.get(dailyHexagramId(bangkokEpochDay(now)));
  return {
    topic: DAILY_TOPIC,
    title: `${h.nameCn} ${shortNameTh(h)}`,
    body: h.dailyMessage,
    hexagramId: h.id,
    // Dropped rather than delivered late: a morning message makes no sense at night.
    ttlMs: 12 * 3600_000,
  };
}
