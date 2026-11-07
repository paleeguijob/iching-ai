/**
 * The daily-hexagram streak and the themes it unlocked, kept per account so they survive a reinstall
 * or a new device. It is cosmetic, so the client's numbers are trusted within sane bounds.
 * The merge rule matches StreakState.mergedWith in the app's core/domain.
 */
import { bangkokEpochDay } from "./daily";

export const THEMES = ["ink_paper", "bamboo", "cinnabar_dawn", "ink_night", "cinnabar_gold"] as const;
const MAX_DAYS = 100_000;

export interface StreakDoc {
  current: number;
  best: number;
  /** Days since 1970-01-01 at UTC+7 when the streak last counted; null if it never did. */
  lastEpochDay: number | null;
  unlocked: string[];
}

export const EMPTY_STREAK: StreakDoc = { current: 0, best: 0, lastEpochDay: null, unlocked: ["ink_paper"] };

/** The later day's run wins; best and unlocked themes only ever grow. */
export function mergeStreaks(a: StreakDoc, b: StreakDoc): StreakDoc {
  const aDay = a.lastEpochDay ?? -1;
  const bDay = b.lastEpochDay ?? -1;
  const run = aDay > bDay ? a : bDay > aDay ? b : a.current >= b.current ? a : b;
  return {
    current: run.current,
    best: Math.max(a.best, b.best, run.current),
    lastEpochDay: run.lastEpochDay,
    unlocked: THEMES.filter((t) => t === "ink_paper" || a.unlocked.includes(t) || b.unlocked.includes(t)),
  };
}

const isDays = (v: unknown): v is number => typeof v === "number" && Number.isInteger(v) && v >= 0 && v <= MAX_DAYS;

/** Validates a streak sent by the app; null when malformed. A day after tomorrow (Bangkok time) means a broken clock. */
export function parseStreak(body: Record<string, unknown>, nowMillis: number): StreakDoc | null {
  const last = body.lastEpochDay;
  const lastOk = last === null || (isDays(last) && last <= bangkokEpochDay(nowMillis) + 1);
  const { current, best } = body;
  if (!lastOk || !isDays(current) || !isDays(best) || !Array.isArray(body.unlocked)) return null;
  return {
    current,
    best: Math.max(best, current),
    lastEpochDay: last as number | null,
    unlocked: THEMES.filter((t) => t === "ink_paper" || (body.unlocked as unknown[]).includes(t)),
  };
}

/** Reads a stored document defensively: fields may be missing on old or hand-edited data. */
export function normalizeStreak(d: Partial<StreakDoc> | undefined | null): StreakDoc {
  if (!d) return EMPTY_STREAK;
  const n = (v: unknown) => (typeof v === "number" && Number.isInteger(v) && v >= 0 ? Math.min(v, MAX_DAYS) : 0);
  return {
    current: n(d.current),
    best: Math.max(n(d.best), n(d.current)),
    lastEpochDay: typeof d.lastEpochDay === "number" ? d.lastEpochDay : null,
    unlocked: THEMES.filter((t) => t === "ink_paper" || (Array.isArray(d.unlocked) && d.unlocked.includes(t))),
  };
}
