/** The Chinese New Year campaign window. Times are epoch ms; Firestore stores them as ISO strings. */
export interface Campaign {
  id: string;
  year: number;
  startsAt: number;
  endsAt: number;
  pushes: number[];
  enabled: boolean;
  yearContext: string;
}

export const DEFAULT_CAMPAIGN: Campaign = {
  id: "cny-2027",
  year: 2027,
  startsAt: Date.parse("2027-01-20T00:00:00+07:00"),
  endsAt: Date.parse("2027-02-22T00:00:00+07:00"),
  pushes: [Date.parse("2027-02-05T19:00:00+07:00"), Date.parse("2027-02-06T08:00:00+07:00")],
  enabled: false,
  yearContext: "ปีมะแมธาตุไฟ (丁未) เริ่ม 6 ก.พ. 2027 — กวานี้คือกวาประจำปีของผู้แสวงหา ให้ตีความเป็นแนวทางทั้งปี",
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
  return { id: r.id, year: r.year, startsAt, endsAt, pushes: pushes as number[], enabled: r.enabled, yearContext: r.yearContext };
}

export const campaignActive = (c: Campaign | null, now: number): boolean =>
  c !== null && c.enabled && c.startsAt <= now && now < c.endsAt;
