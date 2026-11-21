import type { RuntimeConfig } from "./quota";

/** The seeker's language, sent by the app in the X-App-Lang header and kept on each reading. */
export type Lang = "th" | "en";

/** "en" → English; anything else (missing, unknown) → Thai. */
export function parseLang(raw: unknown): Lang {
  return raw === "en" ? "en" : "th";
}

/** Language for a new request: Thai whenever English is switched off in the runtime config. */
export function requestLang(header: string | undefined, cfg: Pick<RuntimeConfig, "englishEnabled">): Lang {
  return cfg.englishEnabled ? parseLang(header) : "th";
}
