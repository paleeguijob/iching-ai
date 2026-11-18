/** A time-boxed promotion shown on the app's home to free accounts. Times are epoch ms; Firestore stores ISO strings. */
export interface Promo {
  id: string;
  enabled: boolean;
  startsAt: number;
  endsAt: number;
  label: string;
  title: string;
  cta: string;
  showEndDate: boolean;
}

/** What /status sends: only while the promo runs, and never to Premium. */
export interface StatusPromo {
  id: string;
  label: string;
  title: string;
  cta: string;
  endsAt: number;
  showEndDate: boolean;
}

// Caps keep the home card from overflowing a small screen.
const MAX_LABEL = 20;
const MAX_TITLE = 40;
const MAX_CTA = 12;

const toMs = (v: unknown): number | null => {
  if (typeof v !== "string") return null;
  const t = Date.parse(v);
  return Number.isNaN(t) ? null : t;
};

const text = (v: unknown, max: number): v is string => typeof v === "string" && v.length > 0 && v.length <= max;

/** Parses the Firestore shape; null (= no promo) when any field is missing, mistyped, too long or inconsistent. */
export function parsePromo(raw: unknown): Promo | null {
  if (typeof raw !== "object" || raw === null) return null;
  const r = raw as Record<string, unknown>;
  const startsAt = toMs(r.startsAt);
  const endsAt = toMs(r.endsAt);
  if (typeof r.id !== "string" || r.id.length === 0) return null;
  if (typeof r.enabled !== "boolean" || typeof r.showEndDate !== "boolean") return null;
  if (!text(r.label, MAX_LABEL) || !text(r.title, MAX_TITLE) || !text(r.cta, MAX_CTA)) return null;
  if (startsAt === null || endsAt === null || endsAt <= startsAt) return null;
  return {
    id: r.id,
    enabled: r.enabled,
    startsAt,
    endsAt,
    label: r.label,
    title: r.title,
    cta: r.cta,
    showEndDate: r.showEndDate,
  };
}

export function promoFor(p: Promo | null, premium: boolean, now: number): StatusPromo | null {
  if (p === null || !p.enabled || premium || now < p.startsAt || now >= p.endsAt) return null;
  return { id: p.id, label: p.label, title: p.title, cta: p.cta, endsAt: p.endsAt, showEndDate: p.showEndDate };
}
