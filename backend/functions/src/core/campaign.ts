import { DAILY_TOPICS } from "./daily";
import type { Lang } from "./lang";

/** The Chinese New Year campaign window. Times are epoch ms; Firestore stores them as ISO strings. */
export interface Campaign {
  id: string;
  year: number;
  startsAt: number;
  endsAt: number;
  pushes: number[];
  enabled: boolean;
  yearContext: string;
  /** English year context for the New Year prompt; left out of English prompts when absent. */
  yearContextEn?: string;
}

export const DEFAULT_CAMPAIGN: Campaign = {
  id: "cny-2027",
  year: 2027,
  startsAt: Date.parse("2027-01-20T00:00:00+07:00"),
  endsAt: Date.parse("2027-02-22T00:00:00+07:00"),
  pushes: [Date.parse("2027-02-05T19:00:00+07:00"), Date.parse("2027-02-06T08:00:00+07:00")],
  enabled: false,
  yearContext: "ปีมะแมธาตุไฟ (丁未) เริ่ม 6 ก.พ. 2027 — กวานี้คือกวาประจำปีของผู้แสวงหา ให้ตีความเป็นแนวทางทั้งปี",
  yearContextEn:
    "Fire Goat year (丁未) begins 6 Feb 2027. This hexagram is the seeker's hexagram for the year: read it as guidance for the whole year.",
};

const toMs = (v: unknown): number | null => {
  if (typeof v !== "string") return null;
  const t = Date.parse(v);
  return Number.isNaN(t) ? null : t;
};

/** Parses the Firestore shape; null (= no campaign) when any field is missing, mistyped or inconsistent. */
export function parseCampaign(raw: unknown): Campaign | null {
  if (typeof raw !== "object" || raw === null) return null;
  const r = raw as Record<string, unknown>;
  const startsAt = toMs(r.startsAt);
  const endsAt = toMs(r.endsAt);
  if (typeof r.id !== "string" || r.id.length === 0) return null;
  if (typeof r.year !== "number" || typeof r.enabled !== "boolean") return null;
  if (typeof r.yearContext !== "string" || r.yearContext.length === 0) return null;
  if (startsAt === null || endsAt === null || endsAt <= startsAt) return null;
  if (!Array.isArray(r.pushes)) return null;
  const pushes = r.pushes.map(toMs);
  if (pushes.some((p) => p === null)) return null;
  const yearContextEn = typeof r.yearContextEn === "string" && r.yearContextEn.trim().length > 0 ? r.yearContextEn : undefined;
  return {
    id: r.id,
    year: r.year,
    startsAt,
    endsAt,
    pushes: pushes as number[],
    enabled: r.enabled,
    yearContext: r.yearContext,
    ...(yearContextEn ? { yearContextEn } : {}),
  };
}

export const campaignActive = (c: Campaign | null, now: number): boolean =>
  c !== null && c.enabled && c.startsAt <= now && now < c.endsAt;

export interface CampaignPush {
  lang: Lang;
  /** Dedupe key: Thai keeps the original "<id>-<n>"; English adds "-en". */
  key: string;
  topic: string;
  title: string;
  body: string;
  ttlMs: number;
  data: { type: "campaign"; campaignId: string };
}

const HOUR_MS = 3_600_000;

const PUSH_COPY = [
  { title: "คืนส่งท้ายปีมะเมีย", body: "ตั้งจิตรับปีมะแม เสี่ยงทายกวาประจำปีของท่านได้แล้ววันนี้" },
  { title: "ซินเจียยู่อี่ ซินนี้ฮวดไช้", body: "ฟ้าเปิดปีใหม่ รับกวาประจำปีมะแม ๒๕๗๐ ของท่าน" },
];

// Our own English copy, never a translation made at send time.
export const PUSH_COPY_EN = [
  { title: "The eve of the new year", body: "Settle your mind for the Year of the Goat. Cast your hexagram for the year today." },
  { title: "Happy Lunar New Year", body: "A new year opens. Receive your hexagram for the Year of the Goat 2027." },
];

/** The campaign pushes whose hour contains `now`: one per language, each to its own topic. Empty when none is due. */
export function dueCampaignPush(c: Campaign | null, now: number): CampaignPush[] {
  if (c === null || !c.enabled) return [];
  const i = c.pushes.findIndex((p) => p <= now && now < p + HOUR_MS);
  if (i < 0 || i >= PUSH_COPY.length) return [];
  const base = { ttlMs: 6 * HOUR_MS, data: { type: "campaign" as const, campaignId: c.id } };
  return [
    { lang: "th", key: `${c.id}-${i}`, topic: DAILY_TOPICS.th, ...PUSH_COPY[i], ...base },
    { lang: "en", key: `${c.id}-${i}-en`, topic: DAILY_TOPICS.en, ...PUSH_COPY_EN[i], ...base },
  ];
}
