/**
 * HTTP API for the app, written against small interfaces so it runs under Cloud Functions in
 * production and against in-memory fakes in tests. src/index.ts wires the Firebase versions.
 *
 * Every request carries:  Authorization: Bearer <Firebase ID token>
 *                         X-Firebase-AppCheck: <App Check token>
 *                         X-Device-Id: <stable per-install id>   (hashed before storage)
 */
import { createHash } from "node:crypto";
import { Cast, castHexagram, Coin, LineValue, resolveLines } from "./cast";
import { estimateCostThb, GeminiError, Usage } from "./gemini";
import { Scripture } from "./hexagrams";
import {
  buildFollowUpPrompt,
  buildReadingPrompt,
  Category,
  FollowUpTurn,
  MAX_FOLLOW_UP_CHARS,
  parseCategory,
  Prompt,
  sanitizeQuestion,
} from "./prompt";
import {
  Charge,
  ChargeDecision,
  chargeReading,
  DeviceDoc,
  grantAdReward,
  QuotaStatus,
  refundReading,
  RuntimeConfig,
  status,
  UserDoc,
} from "./quota";
import { checkSafety } from "./safety";

// ---------- transport ----------

export interface HttpReq {
  method: string;
  path: string;
  body: unknown;
  query: Record<string, unknown>;
  get(header: string): string | undefined;
}

export interface HttpRes {
  status(code: number): HttpRes;
  json(body: unknown): void;
  setHeader(name: string, value: string): void;
  write(chunk: string): boolean;
  end(): void;
  flushHeaders?(): void;
  on(event: "close", cb: () => void): void;
  headersSent: boolean;
  writableEnded: boolean;
}

export class HttpError extends Error {
  constructor(readonly status: number, readonly code: string, message = code) {
    super(message);
  }
}

// ---------- storage ----------

export interface StoredReading {
  id: string;
  question: string;
  category: Category | null;
  lines: LineValue[];
  coins: Coin[][];
  primaryId: number;
  relatingId: number | null;
  aiText: string | null;
  source: "ai" | "scripture";
  model: string | null;
  tokensIn: number;
  tokensOut: number;
  charge: Charge | null;
  followUps: (FollowUpTurn & { at: number })[];
  followUpCount: number;
  outcome: "yes" | "no" | "partly" | null;
  outcomeNote: string | null;
  createdAt: number;
}

export interface QuotaTxResult<T> {
  user?: UserDoc;
  device?: DeviceDoc | null;
  result: T;
}

export interface Store {
  /** Reads users/{uid} and devices/{deviceHash} (when given) in one transaction and writes back what [fn] returns. */
  quotaTx<T>(uid: string, deviceHash: string | null, fn: (user: UserDoc, device: DeviceDoc | null) => QuotaTxResult<T>): Promise<T>;
  createReading(uid: string, reading: Omit<StoredReading, "id">): Promise<string>;
  getReading(uid: string, id: string): Promise<StoredReading | null>;
  updateReading(uid: string, id: string, patch: Partial<Omit<StoredReading, "id">>): Promise<void>;
  /** Atomically bumps followUpCount when below [limit]; returns false when the limit is reached. */
  reserveFollowUp(uid: string, id: string, limit: number): Promise<boolean>;
  releaseFollowUp(uid: string, id: string): Promise<void>;
  appendFollowUp(uid: string, id: string, turn: FollowUpTurn & { at: number }): Promise<void>;
  listReadings(uid: string, limit: number): Promise<StoredReading[]>;
  addReport(report: { uid: string; readingId: string; reason: string; note: string; createdAt: number }): Promise<void>;
  setPremiumUntil(uid: string, premiumUntil: number | null): Promise<void>;
  /** Adds coins once per transaction id; returns false if this transaction was already granted. */
  grantCoins(uid: string, transactionId: string, coins: number): Promise<boolean>;
  deleteUser(uid: string): Promise<void>;
}

export interface Entitlements {
  premiumUntil: number | null;
  /** Consumable coin purchases, by store transaction id. */
  coinPurchases: { transactionId: string; productId: string }[];
}

export interface Deps {
  verifyIdToken(token: string): Promise<{ uid: string }>;
  verifyAppCheck(token: string): Promise<void>;
  store: Store;
  scripture: Scripture;
  config(): Promise<RuntimeConfig>;
  streamModel(prompt: Prompt, model: string, signal: AbortSignal): AsyncGenerator<string, Usage>;
  /** RevenueCat lookup; null when RevenueCat is not configured. */
  fetchEntitlements: ((uid: string) => Promise<Entitlements>) | null;
  deleteAuthUser(uid: string): Promise<void>;
  deletePurchaser: ((uid: string) => Promise<void>) | null;
  now(): number;
  log: { info(msg: string, data?: object): void; warn(msg: string, data?: object): void; error(msg: string, data?: object): void };
  /** Skip App Check (emulator only). */
  skipAppCheck?: boolean;
}

export const COINS_PER_PACK: Record<string, number> = { iching_coins_10: 10 };

// ---------- helpers ----------

const hashDevice = (raw: string | undefined): string | null => {
  const id = raw?.trim();
  if (!id || id.length < 8 || id.length > 200) return null;
  return createHash("sha256").update(`iching-device:${id}`).digest("hex").slice(0, 40);
};

class Sse {
  private closed = false;
  constructor(private readonly res: HttpRes) {
    res.status(200);
    res.setHeader("Content-Type", "text/event-stream; charset=utf-8");
    res.setHeader("Cache-Control", "no-cache, no-transform");
    res.setHeader("X-Accel-Buffering", "no");
    res.flushHeaders?.();
    res.on("close", () => (this.closed = true));
  }
  get isClosed() {
    return this.closed || this.res.writableEnded;
  }
  send(event: string, data: unknown) {
    if (!this.isClosed) this.res.write(`event: ${event}\ndata: ${JSON.stringify(data)}\n\n`);
  }
  end() {
    if (!this.res.writableEnded) this.res.end();
  }
}

const body = (req: HttpReq): Record<string, unknown> =>
  req.body && typeof req.body === "object" ? (req.body as Record<string, unknown>) : {};

function castFromStored(scripture: Scripture, r: StoredReading): Cast {
  return resolveLines(scripture, r.lines, r.coins);
}

const castEvent = (readingId: string, cast: Cast, question: string) => ({
  readingId,
  question,
  lines: cast.lines,
  coins: cast.coins,
  primaryId: cast.primary.id,
  relatingId: cast.relating?.id ?? null,
});

// ---------- the API ----------

export function createApi(deps: Deps) {
  async function authenticate(req: HttpReq, cfg: RuntimeConfig): Promise<string> {
    const auth = req.get("authorization") ?? "";
    const token = auth.startsWith("Bearer ") ? auth.slice(7).trim() : "";
    if (!token) throw new HttpError(401, "unauthenticated");
    if (cfg.enforceAppCheck && !deps.skipAppCheck) {
      const appCheck = req.get("x-firebase-appcheck");
      if (!appCheck) throw new HttpError(401, "app_check_missing");
      await deps.verifyAppCheck(appCheck).catch(() => {
        throw new HttpError(401, "app_check_invalid");
      });
    }
    try {
      return (await deps.verifyIdToken(token)).uid;
    } catch {
      throw new HttpError(401, "unauthenticated");
    }
  }

  const readStatus = (uid: string, deviceHash: string | null, cfg: RuntimeConfig): Promise<QuotaStatus> =>
    deps.store.quotaTx(uid, deviceHash, (user, device) => ({ result: status(user, device, cfg, deps.now()) }));

  /** Streams an AI reading for an existing cast, charging quota first and refunding on failure. */
  async function interpret(sse: Sse, uid: string, deviceHash: string | null, cfg: RuntimeConfig, reading: StoredReading, cast: Cast) {
    const decision = await deps.store.quotaTx<ChargeDecision>(uid, deviceHash, (user, device) => {
      const d = chargeReading(user, device, cfg, deps.now());
      return d.ok ? { user: d.user, device: d.device, result: d } : { result: d };
    });
    if (!decision.ok) {
      sse.send("limited", {
        reason: decision.reason,
        retryAfterSeconds: decision.retryAfterSeconds ?? null,
        status: await readStatus(uid, deviceHash, cfg),
      });
      return;
    }

    const prompt = buildReadingPrompt(reading.question, cast, reading.category);
    const abort = new AbortController();
    const watcher = setInterval(() => sse.isClosed && abort.abort(), 500);
    let text = "";
    try {
      const stream = deps.streamModel(prompt, decision.model, abort.signal);
      let step = await stream.next();
      while (!step.done) {
        text += step.value;
        sse.send("delta", { text: step.value });
        step = await stream.next();
      }
      const usage = step.value;
      await deps.store.updateReading(uid, reading.id, {
        aiText: text.trim(),
        source: "ai",
        model: decision.model,
        tokensIn: usage.tokensIn,
        tokensOut: usage.tokensOut,
        charge: decision.charge,
      });
      deps.log.info("reading_cost", {
        uid,
        model: decision.model,
        charge: decision.charge,
        tokensIn: usage.tokensIn,
        tokensOut: usage.tokensOut,
        costThb: Number(estimateCostThb(decision.model, usage).toFixed(4)),
      });
      sse.send("done", { model: decision.model, status: await readStatus(uid, deviceHash, cfg) });
    } catch (e) {
      // The seeker left mid-stream after receiving words: keep what they saw and the charge.
      if (sse.isClosed && text.trim().length > 0) {
        await deps.store.updateReading(uid, reading.id, { aiText: text.trim(), source: "ai", model: decision.model, charge: decision.charge });
        return;
      }
      await deps.store.quotaTx(uid, deviceHash, (user, device) => {
        const back = refundReading(user, device, decision.charge);
        return { user: back.user, device: back.device, result: null };
      });
      const code = e instanceof GeminiError ? e.code : "internal";
      deps.log.warn("reading_failed", { uid, code, message: (e as Error).message });
      sse.send("error", { code, refunded: true });
    } finally {
      clearInterval(watcher);
    }
  }

  async function divine(req: HttpReq, res: HttpRes, uid: string, deviceHash: string | null, cfg: RuntimeConfig) {
    const b = body(req);
    const existingId = typeof b.readingId === "string" ? b.readingId : null;
    let reading: StoredReading;
    let cast: Cast;

    if (existingId) {
      const found = await deps.store.getReading(uid, existingId);
      if (!found) throw new HttpError(404, "reading_not_found");
      reading = found;
      cast = castFromStored(deps.scripture, found);
    } else {
      const question = sanitizeQuestion(b.question);
      const safety = checkSafety(question);
      if (safety.kind === "crisis") {
        const sse = new Sse(res);
        deps.log.info("safety_redirect", { uid, topic: safety.topic });
        sse.send("safety", { topic: safety.topic, message: safety.message });
        sse.end();
        return;
      }
      cast = castHexagram(deps.scripture);
      const data: Omit<StoredReading, "id"> = {
        question,
        category: parseCategory(b.category),
        lines: cast.lines,
        coins: cast.coins,
        primaryId: cast.primary.id,
        relatingId: cast.relating?.id ?? null,
        aiText: null,
        source: "scripture",
        model: null,
        tokensIn: 0,
        tokensOut: 0,
        charge: null,
        followUps: [],
        followUpCount: 0,
        outcome: null,
        outcomeNote: null,
        createdAt: deps.now(),
      };
      reading = { id: await deps.store.createReading(uid, data), ...data };
    }

    const sse = new Sse(res);
    sse.send("cast", castEvent(reading.id, cast, reading.question));
    if (reading.aiText) {
      // Already interpreted (e.g. the app retried after a dropped connection): replay, never re-charge.
      sse.send("delta", { text: reading.aiText });
      sse.send("done", { model: reading.model, status: await readStatus(uid, deviceHash, cfg) });
    } else {
      await interpret(sse, uid, deviceHash, cfg, reading, cast);
    }
    sse.end();
  }

  async function followUp(req: HttpReq, res: HttpRes, uid: string, deviceHash: string | null, cfg: RuntimeConfig) {
    const b = body(req);
    const id = typeof b.readingId === "string" ? b.readingId : "";
    const message = sanitizeQuestion(b.message, MAX_FOLLOW_UP_CHARS);
    if (!id || !message) throw new HttpError(400, "bad_request");
    const st = await readStatus(uid, deviceHash, cfg);
    if (!st.premium) throw new HttpError(403, "premium_required");
    if (!cfg.aiEnabled) throw new HttpError(503, "ai_disabled");
    const reading = await deps.store.getReading(uid, id);
    if (!reading || !reading.aiText) throw new HttpError(404, "reading_not_found");

    const sse = new Sse(res);
    const safety = checkSafety(message);
    if (safety.kind === "crisis") {
      sse.send("safety", { topic: safety.topic, message: safety.message });
      sse.end();
      return;
    }
    if (!(await deps.store.reserveFollowUp(uid, id, cfg.followUpsPerReading))) {
      sse.send("limited", { reason: "follow_up_limit", retryAfterSeconds: null, status: st });
      sse.end();
      return;
    }
    const model = cfg.forceModel ?? cfg.freeModel;
    const prompt = buildFollowUpPrompt(reading.question, castFromStored(deps.scripture, reading), reading.aiText, reading.followUps, message);
    const abort = new AbortController();
    const watcher = setInterval(() => sse.isClosed && abort.abort(), 500);
    let text = "";
    try {
      const stream = deps.streamModel(prompt, model, abort.signal);
      let step = await stream.next();
      while (!step.done) {
        text += step.value;
        sse.send("delta", { text: step.value });
        step = await stream.next();
      }
      await deps.store.appendFollowUp(uid, id, { q: message, a: text.trim(), at: deps.now() });
      deps.log.info("follow_up_cost", {
        uid,
        model,
        tokensIn: step.value.tokensIn,
        tokensOut: step.value.tokensOut,
        costThb: Number(estimateCostThb(model, step.value).toFixed(4)),
      });
      sse.send("done", { model, remaining: Math.max(0, cfg.followUpsPerReading - reading.followUpCount - 1) });
    } catch (e) {
      if (sse.isClosed && text.trim()) {
        await deps.store.appendFollowUp(uid, id, { q: message, a: text.trim(), at: deps.now() });
      } else {
        await deps.store.releaseFollowUp(uid, id);
        sse.send("error", { code: e instanceof GeminiError ? e.code : "internal", refunded: true });
      }
    } finally {
      clearInterval(watcher);
    }
    sse.end();
  }

  async function syncPurchases(uid: string) {
    if (!deps.fetchEntitlements) throw new HttpError(503, "purchases_not_configured");
    const ent = await deps.fetchEntitlements(uid);
    await deps.store.setPremiumUntil(uid, ent.premiumUntil);
    for (const p of ent.coinPurchases) {
      const coins = COINS_PER_PACK[p.productId];
      if (coins) await deps.store.grantCoins(uid, p.transactionId, coins);
    }
  }

  const toClientReading = (r: StoredReading) => ({
    id: r.id,
    question: r.question,
    category: r.category,
    lines: r.lines,
    primaryId: r.primaryId,
    relatingId: r.relatingId,
    aiText: r.aiText,
    source: r.source,
    followUps: r.followUps,
    outcome: r.outcome,
    outcomeNote: r.outcomeNote,
    createdAt: r.createdAt,
  });

  /** Single entry point: routes by method + path. */
  return async function handle(req: HttpReq, res: HttpRes): Promise<void> {
    try {
      const cfg = await deps.config();
      const uid = await authenticate(req, cfg);
      const deviceHash = hashDevice(req.get("x-device-id"));
      const route = `${req.method.toUpperCase()} ${req.path.replace(/\/+$/, "") || "/"}`;
      const b = body(req);

      if (route === "POST /divine") return await divine(req, res, uid, deviceHash, cfg);
      if (route === "POST /follow-up") return await followUp(req, res, uid, deviceHash, cfg);

      if (route === "GET /status") {
        res.status(200).json(await readStatus(uid, deviceHash, cfg));
        return;
      }
      if (route === "POST /ad-reward") {
        const granted = await deps.store.quotaTx<{ granted: boolean; reason?: string }>(uid, deviceHash, (user, device) => {
          const g = grantAdReward(user, device, cfg, deps.now());
          return g.ok ? { user: g.user, device: g.device, result: { granted: true as const } } : { result: { granted: false as const, reason: g.reason } };
        });
        res.status(200).json({ ...granted, status: await readStatus(uid, deviceHash, cfg) });
        return;
      }
      if (route === "POST /report") {
        const readingId = typeof b.readingId === "string" ? b.readingId : "";
        const reason = typeof b.reason === "string" ? b.reason.slice(0, 40) : "other";
        const note = typeof b.note === "string" ? b.note.slice(0, 500) : "";
        if (!readingId) throw new HttpError(400, "bad_request");
        await deps.store.addReport({ uid, readingId, reason, note, createdAt: deps.now() });
        res.status(200).json({ ok: true });
        return;
      }
      if (route === "GET /readings") {
        const st = await readStatus(uid, deviceHash, cfg);
        if (!st.premium) throw new HttpError(403, "premium_required");
        const limit = Math.min(500, Math.max(1, Number(req.query.limit) || 200));
        res.status(200).json({ readings: (await deps.store.listReadings(uid, limit)).map(toClientReading) });
        return;
      }
      const outcome = route.match(/^POST \/readings\/([A-Za-z0-9_-]{1,64})\/outcome$/);
      if (outcome) {
        const value = b.outcome;
        if (value !== null && value !== "yes" && value !== "no" && value !== "partly") throw new HttpError(400, "bad_request");
        if (!(await deps.store.getReading(uid, outcome[1]))) throw new HttpError(404, "reading_not_found");
        await deps.store.updateReading(uid, outcome[1], {
          outcome: value,
          outcomeNote: typeof b.note === "string" ? b.note.slice(0, 2000) : null,
        });
        res.status(200).json({ ok: true });
        return;
      }
      if (route === "POST /purchases/sync") {
        await syncPurchases(uid);
        res.status(200).json(await readStatus(uid, deviceHash, cfg));
        return;
      }
      if (route === "DELETE /account") {
        await deps.store.deleteUser(uid);
        if (deps.deletePurchaser) await deps.deletePurchaser(uid).catch((e) => deps.log.warn("revenuecat_delete_failed", { uid, e: String(e) }));
        await deps.deleteAuthUser(uid);
        deps.log.info("account_deleted", { uid });
        res.status(200).json({ ok: true });
        return;
      }
      throw new HttpError(404, "not_found");
    } catch (e) {
      const err = e instanceof HttpError ? e : new HttpError(500, "internal");
      if (!(e instanceof HttpError)) deps.log.error("api_error", { message: (e as Error)?.message, stack: (e as Error)?.stack });
      if (!res.headersSent) res.status(err.status).json({ error: err.code });
      else if (!res.writableEnded) {
        res.write(`event: error\ndata: ${JSON.stringify({ code: err.code })}\n\n`);
        res.end();
      }
    }
  };
}

// ---------- RevenueCat ----------

export interface RevenueCatEvent {
  type?: string;
  app_user_id?: string;
  original_app_user_id?: string;
  aliases?: string[];
  transferred_from?: string[];
  transferred_to?: string[];
}

/** Firebase uids the webhook event refers to (RevenueCat anonymous ids are skipped). */
export function uidsFromEvent(event: RevenueCatEvent): string[] {
  const ids = new Set<string>();
  const add = (id?: string) => id && !id.startsWith("$RCAnonymousID") && ids.add(id);
  add(event.app_user_id);
  add(event.original_app_user_id);
  (event.transferred_from ?? []).forEach(add);
  (event.transferred_to ?? []).forEach(add);
  return [...ids];
}

/**
 * Webhook: RevenueCat's own advice is to re-read the subscriber on every event rather than
 * interpreting each event type, so ordering and retries cannot leave a wrong state.
 */
export function createRevenueCatWebhook(deps: Pick<Deps, "store" | "fetchEntitlements" | "log">, authSecret: () => string) {
  return async function handle(req: HttpReq, res: HttpRes): Promise<void> {
    const expected = authSecret();
    if (!expected || req.get("authorization") !== `Bearer ${expected}`) {
      res.status(401).json({ error: "unauthorized" });
      return;
    }
    const event = (body(req).event ?? {}) as RevenueCatEvent;
    if (event.type === "TEST" || !deps.fetchEntitlements) {
      res.status(200).json({ ok: true });
      return;
    }
    try {
      for (const uid of uidsFromEvent(event)) {
        const ent = await deps.fetchEntitlements(uid);
        await deps.store.setPremiumUntil(uid, ent.premiumUntil);
        for (const p of ent.coinPurchases) {
          const coins = COINS_PER_PACK[p.productId];
          if (coins) await deps.store.grantCoins(uid, p.transactionId, coins);
        }
      }
      deps.log.info("revenuecat_event", { type: event.type, uids: uidsFromEvent(event) });
      res.status(200).json({ ok: true });
    } catch (e) {
      deps.log.error("revenuecat_webhook_failed", { message: (e as Error).message });
      // Non-2xx makes RevenueCat retry later.
      res.status(500).json({ error: "retry" });
    }
  };
}

interface RcSubscriber {
  subscriber?: {
    entitlements?: Record<string, { expires_date?: string | null }>;
    non_subscriptions?: Record<string, { id?: string; store_transaction_id?: string }[]>;
  };
}

export const PREMIUM_ENTITLEMENT = "premium";
const FOREVER = Date.UTC(2100, 0, 1);

export function parseEntitlements(json: RcSubscriber, now: number): Entitlements {
  const premium = json.subscriber?.entitlements?.[PREMIUM_ENTITLEMENT];
  let premiumUntil: number | null = null;
  if (premium) {
    premiumUntil = premium.expires_date ? Date.parse(premium.expires_date) : FOREVER;
    if (!Number.isFinite(premiumUntil) || premiumUntil <= now) premiumUntil = null;
  }
  const coinPurchases: Entitlements["coinPurchases"] = [];
  for (const [productId, list] of Object.entries(json.subscriber?.non_subscriptions ?? {})) {
    for (const p of list) {
      const transactionId = p.store_transaction_id || p.id;
      if (transactionId) coinPurchases.push({ transactionId, productId });
    }
  }
  return { premiumUntil, coinPurchases };
}

/** GET https://api.revenuecat.com/v1/subscribers/{uid} with the secret API key. */
export function revenueCatClient(apiKey: () => string, now: () => number, fetchImpl: typeof fetch = fetch) {
  const headers = () => ({ Authorization: `Bearer ${apiKey()}`, "Content-Type": "application/json" });
  return {
    async fetchEntitlements(uid: string): Promise<Entitlements> {
      const r = await fetchImpl(`https://api.revenuecat.com/v1/subscribers/${encodeURIComponent(uid)}`, { headers: headers() });
      if (!r.ok) throw new Error(`RevenueCat HTTP ${r.status}`);
      return parseEntitlements((await r.json()) as RcSubscriber, now());
    },
    async deleteSubscriber(uid: string): Promise<void> {
      const r = await fetchImpl(`https://api.revenuecat.com/v1/subscribers/${encodeURIComponent(uid)}`, { method: "DELETE", headers: headers() });
      if (!r.ok && r.status !== 404) throw new Error(`RevenueCat HTTP ${r.status}`);
    },
  };
}

