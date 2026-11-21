import type { Lang } from "./lang";
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
  /** English card copy; all three or none. Without them the English card is left out. */
  labelEn?: string;
  titleEn?: string;
  ctaEn?: string;
}

export interface Poster {
  id: string;
  enabled: boolean;
  startsAt: number;
  endsAt: number;
  image: string;
  alt: string;
  /** English artwork and its alt text; both or neither. Without them English seekers see no poster. */
  imageEn?: string;
  altEn?: string;
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
  card: { label: string; title: string; cta: string; showEndDate: boolean } | null;
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
  const card: PosterCard = { label: r.label, title: r.title, cta: r.cta, showEndDate: r.showEndDate };
  // The English copy is optional and only counts when complete; a bad English field never voids the Thai card.
  if (text(r.labelEn, MAX_LABEL) && text(r.titleEn, MAX_TITLE) && text(r.ctaEn, MAX_CTA)) {
    card.labelEn = r.labelEn;
    card.titleEn = r.titleEn;
    card.ctaEn = r.ctaEn;
  }
  return card;
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
  const english =
    typeof r.imageEn === "string" && IMAGE.test(r.imageEn) && text(r.altEn, MAX_ALT)
      ? { imageEn: r.imageEn, altEn: r.altEn }
      : {};
  return { id: r.id, enabled: r.enabled, startsAt, endsAt, image: r.image, alt: r.alt, ...english, audience: r.audience, link, card };
}

/** The poster in [lang]. English needs its own artwork (imageEn + altEn); without it there is no poster for English seekers. */
export function posterFor(p: Poster | null, premium: boolean, now: number, lang: Lang = "th"): StatusPoster | null {
  if (p === null || !p.enabled || now < p.startsAt || now >= p.endsAt) return null;
  if (p.audience === "free" && premium) return null;
  const base = { id: p.id, audience: p.audience, link: p.link, endsAt: p.endsAt };
  if (lang !== "en") {
    const card = p.card && { label: p.card.label, title: p.card.title, cta: p.card.cta, showEndDate: p.card.showEndDate };
    return { id: base.id, image: p.image, alt: p.alt, audience: base.audience, link: base.link, card, endsAt: base.endsAt };
  }
  if (!p.imageEn || !p.altEn) return null;
  const c = p.card;
  const card = c && c.labelEn && c.titleEn && c.ctaEn ? { label: c.labelEn, title: c.titleEn, cta: c.ctaEn, showEndDate: c.showEndDate } : null;
  return { id: base.id, image: p.imageEn, alt: p.altEn, audience: base.audience, link: base.link, card, endsAt: base.endsAt };
}
