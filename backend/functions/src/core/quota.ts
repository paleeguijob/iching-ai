/**
 * Pure quota rules. The Firestore layer loads the docs, calls these inside a transaction and
 * writes the returned docs back, so the rules are testable without a database.
 *
 * Order of charging for a reading: premium daily allowance → free daily reading (0 by default since
 * v1.1: free users pay with an ad or a coin) → rewarded-ad credits → coins. A failed model call refunds exactly what was charged.
 */

import { Campaign, campaignActive, DEFAULT_CAMPAIGN, parseCampaign } from "./campaign";

export interface RuntimeConfig {
  /** Global kill-switch: false → every reading comes back as raw scripture. */
  aiEnabled: boolean;
  /** When set, every call uses this model (e.g. switch everyone to Flash-Lite if costs spike). */
  forceModel: string | null;
  freeModel: string;
  premiumModel: string;
  freeDailyReadings: number;
  premiumDailyReadings: number;
  adRewardsPerDay: number;
  followUpsPerReading: number;
  /** Free readings allowed per device per day, against farming fresh anonymous accounts. */
  freeReadingsPerDevice: number;
  minSecondsBetweenReadings: number;
  freeJournalDays: number;
  /** Reject requests without a valid App Check token (turn off only while debugging). */
  enforceAppCheck: boolean;
  /** Kill-switch for the 07:00 daily hexagram push. */
  dailyPushEnabled: boolean;
  /** Chinese New Year campaign window; null when the stored config is malformed (= no campaign). */
  campaign: Campaign | null;
  /** Feature switch for Siam Si; off until the sets are reviewed. */
  siamsiEnabled: boolean;
  /** Sets the app may offer, with the version of each file (bumped to make apps download it again). */
  siamsiSets: SiamSiSetRef[];
  /** Siam Si draws a free account gets per day, one rewarded ad each. Counted by the app. */
  siamsiAdDrawsPerDay: number;
  siamsiPremiumPerDay: number;
}

export interface SiamSiSetRef {
  id: string;
  version: number;
}

export const DEFAULT_CONFIG: RuntimeConfig = {
  aiEnabled: true,
  forceModel: null,
  // Pinned: "-latest" aliases can move to a pricier model without a code change.
  freeModel: "gemini-3.1-flash-lite",
  premiumModel: "gemini-3.5-flash-lite",
  freeDailyReadings: 0,
  premiumDailyReadings: 10,
  adRewardsPerDay: 2,
  followUpsPerReading: 3,
  freeReadingsPerDevice: 0,
  minSecondsBetweenReadings: 8,
  freeJournalDays: 7,
  enforceAppCheck: true,
  dailyPushEnabled: true,
  campaign: DEFAULT_CAMPAIGN,
  siamsiEnabled: false,
  siamsiSets: [
    { id: "guanyin100", version: 1 },
    { id: "jiazi60", version: 1 },
  ],
  siamsiAdDrawsPerDay: 1,
  siamsiPremiumPerDay: 10,
};

const isSetRef = (v: unknown): v is SiamSiSetRef =>
  typeof v === "object" && v !== null &&
  typeof (v as SiamSiSetRef).id === "string" && (v as SiamSiSetRef).id.length > 0 &&
  typeof (v as SiamSiSetRef).version === "number";

/** Merges the Firestore config/runtime doc over the defaults, ignoring fields of the wrong type. */
export function mergeConfig(raw: Record<string, unknown> | undefined): RuntimeConfig {
  const out: RuntimeConfig = { ...DEFAULT_CONFIG };
  if (!raw) return out;
  for (const key of Object.keys(DEFAULT_CONFIG) as (keyof RuntimeConfig)[]) {
    const v = raw[key];
    const def = DEFAULT_CONFIG[key];
    if (key === "campaign") {
      if (v !== undefined) out.campaign = parseCampaign(v);
    } else if (key === "siamsiSets") {
      if (Array.isArray(v) && v.every(isSetRef)) out.siamsiSets = v.map((r) => ({ id: r.id, version: r.version }));
    } else if (key === "forceModel") {
      if (v === null || (typeof v === "string" && v.length > 0)) out.forceModel = v as string | null;
    } else if (v !== undefined && typeof v === typeof def) {
      (out as unknown as Record<string, unknown>)[key] = v;
    }
  }
  return out;
}

export interface DailyUsage {
  day: string;
  free: number;
  premium: number;
  /** Ad credits earned today (each one = one extra reading). */
  adEarned: number;
  /** Ad credits spent today. */
  adUsed: number;
}

export interface UserDoc {
  premiumUntil: number | null;
  coins: number;
  usage: DailyUsage;
  lastReadingAt: number | null;
}

export interface DeviceDoc {
  usage: { day: string; free: number; adEarned: number };
}

export type Charge = "premium" | "free" | "ad" | "coin";

export const emptyUsage = (day: string): DailyUsage => ({ day, free: 0, premium: 0, adEarned: 0, adUsed: 0 });

export function normalizeUser(raw: Partial<UserDoc> | undefined, day: string): UserDoc {
  const usage = raw?.usage && raw.usage.day === day ? { ...emptyUsage(day), ...raw.usage } : emptyUsage(day);
  return {
    premiumUntil: typeof raw?.premiumUntil === "number" ? raw.premiumUntil : null,
    coins: Math.max(0, Math.floor(raw?.coins ?? 0)),
    usage,
    lastReadingAt: typeof raw?.lastReadingAt === "number" ? raw.lastReadingAt : null,
  };
}

export function normalizeDevice(raw: Partial<DeviceDoc> | undefined, day: string): DeviceDoc {
  const u = raw?.usage;
  return { usage: u && u.day === day ? { day, free: u.free ?? 0, adEarned: u.adEarned ?? 0 } : { day, free: 0, adEarned: 0 } };
}

export const isPremium = (u: UserDoc, now: number) => u.premiumUntil !== null && u.premiumUntil > now;

export interface QuotaStatus {
  premium: boolean;
  premiumUntil: number | null;
  coins: number;
  /** Readings available right now without watching an ad or spending a coin. */
  includedLeft: number;
  adCreditsLeft: number;
  /** How many more ads may still be watched today. */
  adsWatchableLeft: number;
  readingsLeft: number;
  followUpsPerReading: number;
  journalDays: number | null;
  aiEnabled: boolean;
  campaign: { id: string; year: number; active: boolean; endsAt: number } | null;
  siamsi: { enabled: boolean; sets: SiamSiSetRef[]; drawsPerDay: number };
}

export function status(user: UserDoc, device: DeviceDoc | null, cfg: RuntimeConfig, now: number): QuotaStatus {
  const premium = isPremium(user, now);
  const u = user.usage;
  const deviceFreeLeft = device ? Math.max(0, cfg.freeReadingsPerDevice - device.usage.free) : cfg.freeDailyReadings;
  const includedLeft = premium
    ? Math.max(0, cfg.premiumDailyReadings - u.premium)
    : Math.min(Math.max(0, cfg.freeDailyReadings - u.free), deviceFreeLeft);
  const adCreditsLeft = Math.max(0, u.adEarned - u.adUsed);
  const deviceAdLeft = device ? Math.max(0, cfg.adRewardsPerDay - device.usage.adEarned) : cfg.adRewardsPerDay;
  const adsWatchableLeft = premium ? 0 : Math.min(Math.max(0, cfg.adRewardsPerDay - u.adEarned), deviceAdLeft);
  return {
    premium,
    premiumUntil: user.premiumUntil,
    coins: user.coins,
    includedLeft,
    adCreditsLeft,
    adsWatchableLeft,
    readingsLeft: includedLeft + adCreditsLeft + user.coins,
    followUpsPerReading: premium ? cfg.followUpsPerReading : 0,
    journalDays: premium ? null : cfg.freeJournalDays,
    aiEnabled: cfg.aiEnabled,
    campaign: cfg.campaign && {
      id: cfg.campaign.id,
      year: cfg.campaign.year,
      active: campaignActive(cfg.campaign, now),
      endsAt: cfg.campaign.endsAt,
    },
    siamsi: {
      enabled: cfg.siamsiEnabled,
      sets: cfg.siamsiSets,
      drawsPerDay: premium ? cfg.siamsiPremiumPerDay : cfg.siamsiAdDrawsPerDay,
    },
  };
}

export type ChargeDecision =
  | { ok: true; charge: Charge; model: string; user: UserDoc; device: DeviceDoc | null }
  | { ok: false; reason: "quota" | "rate_limited" | "ai_disabled"; retryAfterSeconds?: number };

/** Decides what pays for one AI reading and returns the updated docs. */
export function chargeReading(
  user: UserDoc,
  device: DeviceDoc | null,
  cfg: RuntimeConfig,
  now: number,
): ChargeDecision {
  if (!cfg.aiEnabled) return { ok: false, reason: "ai_disabled" };
  const limited = rateLimit(user, cfg, now);
  if (limited) return limited;
  const premium = isPremium(user, now);
  const u = { ...user.usage };
  const d = device ? { usage: { ...device.usage } } : null;
  let charge: Charge;
  let coins = user.coins;

  if (premium && u.premium < cfg.premiumDailyReadings) {
    charge = "premium";
    u.premium++;
  } else if (!premium && u.free < cfg.freeDailyReadings && (!d || d.usage.free < cfg.freeReadingsPerDevice)) {
    charge = "free";
    u.free++;
    if (d) d.usage.free++;
  } else if (u.adEarned > u.adUsed) {
    charge = "ad";
    u.adUsed++;
  } else if (coins > 0) {
    charge = "coin";
    coins--;
  } else {
    return { ok: false, reason: "quota" };
  }
  return { ok: true, charge, model: modelFor(charge, cfg), user: { ...user, coins, usage: u, lastReadingAt: now }, device: d };
}

const modelFor = (charge: Charge, cfg: RuntimeConfig) =>
  cfg.forceModel ?? (charge === "premium" || charge === "coin" ? cfg.premiumModel : cfg.freeModel);

/** Free retries per charge after the connection dropped mid-reading; further drops pay again. */
export const MAX_FREE_RESUMES = 2;

/** Seconds until another reading is allowed, or null when it is allowed now. */
export const retryAfterSeconds = (user: UserDoc, cfg: RuntimeConfig, now: number): number | null => {
  if (user.lastReadingAt === null) return null;
  const elapsed = (now - user.lastReadingAt) / 1000;
  return elapsed >= cfg.minSecondsBetweenReadings ? null : Math.ceil(cfg.minSecondsBetweenReadings - elapsed);
};

const rateLimit = (user: UserDoc, cfg: RuntimeConfig, now: number): ChargeDecision | null => {
  const wait = retryAfterSeconds(user, cfg, now);
  return wait === null ? null : { ok: false, reason: "rate_limited", retryAfterSeconds: wait };
};

/** Regenerates a reading whose [charge] was already taken: same limits and model, nothing charged. */
export function resumeReading(user: UserDoc, charge: Charge, cfg: RuntimeConfig, now: number): ChargeDecision {
  if (!cfg.aiEnabled) return { ok: false, reason: "ai_disabled" };
  const limited = rateLimit(user, cfg, now);
  if (limited) return limited;
  return { ok: true, charge, model: modelFor(charge, cfg), user: { ...user, lastReadingAt: now }, device: null };
}

/** Gives back what [chargeReading] took when the model failed. */
export function refundReading(user: UserDoc, device: DeviceDoc | null, charge: Charge): { user: UserDoc; device: DeviceDoc | null } {
  const u = { ...user.usage };
  let coins = user.coins;
  const d = device ? { usage: { ...device.usage } } : null;
  switch (charge) {
    case "premium":
      u.premium = Math.max(0, u.premium - 1);
      break;
    case "free":
      u.free = Math.max(0, u.free - 1);
      if (d) d.usage.free = Math.max(0, d.usage.free - 1);
      break;
    case "ad":
      u.adUsed = Math.max(0, u.adUsed - 1);
      break;
    case "coin":
      coins++;
      break;
  }
  // Clear the rate-limit stamp so the seeker can retry straight away.
  return { user: { ...user, coins, usage: u, lastReadingAt: null }, device: d };
}

export type AdDecision = { ok: true; user: UserDoc; device: DeviceDoc | null } | { ok: false; reason: "premium" | "limit" };

export function grantAdReward(user: UserDoc, device: DeviceDoc | null, cfg: RuntimeConfig, now: number): AdDecision {
  if (isPremium(user, now)) return { ok: false, reason: "premium" };
  if (user.usage.adEarned >= cfg.adRewardsPerDay) return { ok: false, reason: "limit" };
  if (device && device.usage.adEarned >= cfg.adRewardsPerDay) return { ok: false, reason: "limit" };
  return {
    ok: true,
    user: { ...user, usage: { ...user.usage, adEarned: user.usage.adEarned + 1 } },
    device: device ? { usage: { ...device.usage, adEarned: device.usage.adEarned + 1 } } : null,
  };
}

/** Calendar day in Thailand (UTC+7, no DST), used as the quota reset boundary at local midnight. */
export function bangkokDay(now: number): string {
  return new Date(now + 7 * 3600_000).toISOString().slice(0, 10);
}
