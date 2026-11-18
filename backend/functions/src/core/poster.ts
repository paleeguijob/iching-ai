/**
 * One time-boxed poster: an artwork popup on the app's home, optionally with a small card on the daily page.
 * Times are epoch ms; Firestore stores ISO strings. The image is a bare file name under Hosting's content/posters/.
 */
export type PosterAudience = "free" | "all";
export type PosterLink = "paywall" | "daily" | "year" | "library" | "journal";

export interface PosterCard {
  label: string;
  title: string;
  cta: string;
  showEndDate: boolean;
}

export interface Poster {
  id: string;
  enabled: boolean;
  startsAt: number;
  endsAt: number;
  image: string;
  alt: string;
  audience: PosterAudience;
  link: PosterLink | null;
  card: PosterCard | null;
}

/** What /status sends: only while the poster runs, and never a free-only poster to Premium. */
export interface StatusPoster {
  id: string;
  image: string;
  alt: string;
  audience: PosterAudience;
  link: PosterLink | null;
  card: PosterCard | null;
  endsAt: number;
}

const LINKS: readonly string[] = ["paywall", "daily", "year", "library", "journal"];
// A bare file name: no slashes, so no "..", absolute path or URL can point the app elsewhere.
const IMAGE = /^[A-Za-z0-9][A-Za-z0-9._-]*\.(webp|png|jpg|jpeg)$/;
// Caps keep the daily-page card from overflowing a small screen; alt is read aloud by TalkBack.
const MAX_ALT = 120;
const MAX_LABEL = 20;
const MAX_TITLE = 40;
const MAX_CTA = 12;

const toMs = (v: unknown): number | null => {
  if (typeof v !== "string") return null;
  const t = Date.parse(v);
  return Number.isNaN(t) ? null : t;
};

const text = (v: unknown, max: number): v is string => typeof v === "string" && v.length > 0 && v.length <= max;

function parseCard(raw: unknown): PosterCard | null {
  if (typeof raw !== "object" || raw === null) return null;
  const r = raw as Record<string, unknown>;
  if (!text(r.label, MAX_LABEL) || !text(r.title, MAX_TITLE) || !text(r.cta, MAX_CTA)) return null;
  if (typeof r.showEndDate !== "boolean") return null;
  return { label: r.label, title: r.title, cta: r.cta, showEndDate: r.showEndDate };
}

/** Parses the Firestore shape; null (= no poster) when any field is missing, mistyped, too long or inconsistent. */
export function parsePoster(raw: unknown): Poster | null {
  if (typeof raw !== "object" || raw === null) return null;
  const r = raw as Record<string, unknown>;
  if (typeof r.id !== "string" || !/^\S+$/.test(r.id)) return null;
  if (typeof r.enabled !== "boolean") return null;
  const startsAt = toMs(r.startsAt);
  const endsAt = toMs(r.endsAt);
  if (startsAt === null || endsAt === null || endsAt <= startsAt) return null;
  if (typeof r.image !== "string" || !IMAGE.test(r.image)) return null;
  if (!text(r.alt, MAX_ALT)) return null;
  if (r.audience !== "free" && r.audience !== "all") return null;
  let link: PosterLink | null = null;
  if (r.link !== undefined && r.link !== null) {
    if (typeof r.link !== "string" || !LINKS.includes(r.link)) return null;
    link = r.link as PosterLink;
  }
  let card: PosterCard | null = null;
  if (r.card !== undefined && r.card !== null) {
    card = parseCard(r.card);
    // A card that leads nowhere is no use.
    if (card === null || link === null) return null;
  }
  return { id: r.id, enabled: r.enabled, startsAt, endsAt, image: r.image, alt: r.alt, audience: r.audience, link, card };
}

export function posterFor(p: Poster | null, premium: boolean, now: number): StatusPoster | null {
  if (p === null || !p.enabled || now < p.startsAt || now >= p.endsAt) return null;
  if (p.audience === "free" && premium) return null;
  return { id: p.id, image: p.image, alt: p.alt, audience: p.audience, link: p.link, card: p.card, endsAt: p.endsAt };
}
