import { strict as assert } from "node:assert";
import { resolve } from "node:path";
import { beforeEach, describe, it } from "node:test";
import {
  createApi,
  createRevenueCatWebhook,
  Deps,
  Entitlements,
  HttpReq,
  HttpRes,
  parseEntitlements,
  QuotaTxResult,
  revenueCatClient,
  Store,
  StoredReading,
  YearHexagram,
} from "../src/core/api";
import { GeminiError, Usage } from "../src/core/gemini";
import { loadScripture } from "../src/core/hexagrams";
import { EMPTY_STREAK, mergeStreaks, StreakDoc } from "../src/core/streak";
import { DEFAULT_CAMPAIGN } from "../src/core/campaign";
import { bangkokDay, DEFAULT_CONFIG, DeviceDoc, normalizeDevice, normalizeUser, RuntimeConfig, UserDoc } from "../src/core/quota";

const scripture = loadScripture(resolve(__dirname, "../data/hexagrams.json"));

class MemoryStore implements Store {
  users = new Map<string, UserDoc>();
  devices = new Map<string, DeviceDoc>();
  readings = new Map<string, StoredReading>();
  reports: unknown[] = [];
  grants = new Set<string>();
  revoked = new Set<string>();
  private seq = 0;
  constructor(private readonly now: () => number) {}

  async quotaTx<T>(uid: string, deviceHash: string | null, fn: (u: UserDoc, d: DeviceDoc | null) => QuotaTxResult<T>) {
    const day = bangkokDay(this.now());
    const out = fn(normalizeUser(this.users.get(uid), day), deviceHash ? normalizeDevice(this.devices.get(deviceHash), day) : null);
    if (out.user) this.users.set(uid, out.user);
    if (out.device && deviceHash) this.devices.set(deviceHash, out.device);
    return out.result;
  }
  async createReading(uid: string, r: Omit<StoredReading, "id">) {
    const id = `r${++this.seq}`;
    this.readings.set(`${uid}/${id}`, { id, ...r });
    return id;
  }
  async getReading(uid: string, id: string) {
    return this.readings.get(`${uid}/${id}`) ?? null;
  }
  async updateReading(uid: string, id: string, patch: Partial<StoredReading>) {
    const r = this.readings.get(`${uid}/${id}`)!;
    this.readings.set(`${uid}/${id}`, { ...r, ...patch });
  }
  async reserveFollowUp(uid: string, id: string, limit: number) {
    const r = this.readings.get(`${uid}/${id}`)!;
    if (r.followUpCount >= limit) return false;
    r.followUpCount++;
    return true;
  }
  async releaseFollowUp(uid: string, id: string) {
    this.readings.get(`${uid}/${id}`)!.followUpCount--;
  }
  async appendFollowUp(uid: string, id: string, turn: { q: string; a: string; at: number }) {
    this.readings.get(`${uid}/${id}`)!.followUps.push(turn);
  }
  async deleteReading(uid: string, id: string) {
    this.readings.delete(`${uid}/${id}`);
  }
  async listReadings(uid: string, _limit: number, since?: number) {
    return [...this.readings.entries()]
      .filter(([k, v]) => k.startsWith(`${uid}/`) && (since == null || v.createdAt >= since))
      .map(([, v]) => v);
  }
  streaks = new Map<string, StreakDoc>();
  async updateStreak(uid: string, fn: (stored: StreakDoc) => StreakDoc) {
    const next = fn(this.streaks.get(uid) ?? EMPTY_STREAK);
    this.streaks.set(uid, next);
    return next;
  }
  async addReport(r: unknown) {
    this.reports.push(r);
  }
  async setPremiumUntil(uid: string, premiumUntil: number | null) {
    if (premiumUntil === null && !this.users.has(uid)) return;
    const u = normalizeUser(this.users.get(uid), bangkokDay(this.now()));
    this.users.set(uid, { ...u, premiumUntil });
  }
  async grantCoins(uid: string, tx: string, coins: number) {
    if (this.grants.has(tx)) return false;
    this.grants.add(tx);
    const u = normalizeUser(this.users.get(uid), bangkokDay(this.now()));
    this.users.set(uid, { ...u, coins: u.coins + coins });
    return true;
  }
  async revokeCoins(uid: string, tx: string, coins: number) {
    if (this.revoked.has(tx)) return false;
    this.revoked.add(tx);
    const granted = this.grants.has(tx);
    this.grants.add(tx);
    const u = this.users.get(uid);
    if (!granted || !u) return false;
    this.users.set(uid, { ...u, coins: Math.max(0, u.coins - coins) });
    return true;
  }
  async mergeUser(from: string, to: string) {
    const coins = this.users.get(from)?.coins ?? 0;
    const target = normalizeUser(this.users.get(to), bangkokDay(this.now()));
    this.users.set(to, { ...target, coins: target.coins + coins });
    for (const [k, v] of [...this.readings.entries()]) if (k.startsWith(`${from}/`)) this.readings.set(`${to}/${v.id}`, v);
    const left = this.streaks.get(from) ?? EMPTY_STREAK;
    await this.updateStreak(to, (stored) => mergeStreaks(stored, left));
    await this.deleteUser(from);
  }
  years = new Map<string, YearHexagram>();
  async getYearHexagram(uid: string, year: number) {
    return this.years.get(`${uid}/${year}`) ?? null;
  }
  async claimYearHexagram(uid: string, candidate: YearHexagram) {
    const key = `${uid}/${candidate.year}`;
    const kept = this.years.get(key) ?? candidate;
    this.years.set(key, kept);
    await this.updateStreak(uid, (s) => ({ ...s, unlocked: [...new Set([...s.unlocked, "cinnabar_gold"])] }));
    return kept;
  }
  async deleteUser(uid: string) {
    this.users.delete(uid);
    this.streaks.delete(uid);
    for (const k of [...this.readings.keys()]) if (k.startsWith(`${uid}/`)) this.readings.delete(k);
  }
}

class FakeRes implements HttpRes {
  code = 0;
  payload: unknown = null;
  chunks: string[] = [];
  headers: Record<string, string> = {};
  headersSent = false;
  writableEnded = false;
  status(code: number) {
    this.code = code;
    return this;
  }
  json(body: unknown) {
    this.payload = body;
    this.headersSent = true;
    this.writableEnded = true;
  }
  setHeader(k: string, v: string) {
    this.headers[k] = v;
  }
  flushHeaders() {
    this.headersSent = true;
  }
  write(c: string) {
    this.headersSent = true;
    this.chunks.push(c);
    return true;
  }
  end() {
    this.writableEnded = true;
  }
  private closers: (() => void)[] = [];
  on(_event: "close", cb: () => void) {
    this.closers.push(cb);
  }
  /** The client's connection drops. */
  disconnect() {
    this.closers.forEach((cb) => cb());
  }
  get events(): { event: string; data: Record<string, unknown> }[] {
    return this.chunks.join("").split("\n\n").filter(Boolean).map((block) => {
      const [e, d] = block.split("\n");
      return { event: e.replace("event: ", ""), data: JSON.parse(d.replace("data: ", "")) };
    });
  }
}

const req = (method: string, path: string, body: unknown = {}, headers: Record<string, string> = {}): HttpReq => {
  const h: Record<string, string> = { authorization: "Bearer token-u1", "x-firebase-appcheck": "ok", "x-device-id": "device-abcdef-1", ...headers };
  return { method, path, body, query: {}, get: (name) => h[name.toLowerCase()] };
};

describe("api", () => {
  let now: number;
  let store: MemoryStore;
  let modelCalls: string[];
  let modelMode: "ok" | "fail" | "drop";
  let modelChunks: string[] | null;
  /** The response whose connection drops mid-stream when modelMode is "drop". */
  let droppingRes: FakeRes | null;
  let cfg: RuntimeConfig;
  let entitlements: Entitlements;
  let deps: Deps;
  let handle: (r: HttpReq, s: HttpRes) => Promise<void>;

  beforeEach(() => {
    now = Date.UTC(2026, 9, 31, 3, 0, 0);
    store = new MemoryStore(() => now);
    modelCalls = [];
    modelMode = "ok";
    modelChunks = null;
    droppingRes = null;
    cfg = { ...DEFAULT_CONFIG, freeDailyReadings: 1, freeReadingsPerDevice: 1 };
    entitlements = { premiumUntil: null, coinPurchases: [] };
    deps = {
      verifyIdToken: async (t) => {
        if (!t.startsWith("token-")) throw new Error("bad");
        return { uid: t.slice(6) };
      },
      verifyAppCheck: async (t) => {
        if (t !== "ok") throw new Error("bad");
      },
      store,
      scripture,
      config: async () => cfg,
      streamModel: async function* (_p, model, signal): AsyncGenerator<string, Usage> {
        modelCalls.push(model);
        if (modelMode === "fail") throw new GeminiError("rate_limited", "busy");
        if (modelChunks) {
          yield* modelChunks;
          return { tokensIn: 1500, tokensOut: 650 };
        }
        yield "ภูเขา";
        if (modelMode === "drop") {
          droppingRes!.disconnect();
          await new Promise((_, reject) => signal.addEventListener("abort", () => reject(new GeminiError("network", "aborted"))));
        }
        yield "ตั้งมั่น";
        return { tokensIn: 1500, tokensOut: 600 };
      },
      fetchEntitlements: async () => entitlements,
      deletePurchaser: async () => {},
      deleteAuthUser: async () => {},
      isAnonymousUser: async () => true,
      now: () => now,
      log: { info() {}, warn() {}, error() {} },
    };
    handle = createApi(deps);
  });

  describe("year hexagram", () => {
    beforeEach(() => {
      cfg = { ...cfg, campaign: { ...DEFAULT_CAMPAIGN, enabled: true }, minSecondsBetweenReadings: 8 };
      now = DEFAULT_CAMPAIGN.startsAt + 3_600_000;
    });

    it("casts the year hexagram once and returns the same one after", async () => {
      const first = new FakeRes();
      await handle(req("POST", "/year-hexagram"), first);
      assert.equal(first.code, 200);
      now += 60_000;
      const second = new FakeRes();
      await handle(req("POST", "/year-hexagram"), second);
      const a = first.payload as YearHexagram;
      const b = second.payload as YearHexagram;
      assert.equal(a.year, 2027);
      assert.equal(a.lines.length, 6);
      assert.equal(b.primaryId, a.primaryId);
      assert.deepEqual(b.lines, a.lines);
      assert.deepEqual(modelCalls, []);
      const st = new FakeRes();
      await handle(req("GET", "/status"), st);
      assert.equal((st.payload as { readingsLeft: number }).readingsLeft, 1);
      assert.ok(store.streaks.get("u1")!.unlocked.includes("cinnabar_gold"));
    });

    it("does not rate limit a reading made right after a year cast", async () => {
      await handle(req("POST", "/year-hexagram"), new FakeRes());
      const res = new FakeRes();
      await handle(req("POST", "/divine", { question: "ปีนี้เป็นอย่างไร" }), res);
      assert.deepEqual(res.events.map((e) => e.event), ["cast", "delta", "delta", "done"]);
    });

    it("refuses a new cast outside the campaign but still returns an existing one", async () => {
      const mine = new FakeRes();
      await handle(req("POST", "/year-hexagram"), mine);
      now = DEFAULT_CAMPAIGN.endsAt + 1000;
      const again = new FakeRes();
      await handle(req("POST", "/year-hexagram"), again);
      assert.equal(again.code, 200);
      assert.deepEqual(again.payload, mine.payload);
      const get = new FakeRes();
      await handle(req("GET", "/year-hexagram"), get);
      assert.deepEqual(get.payload, mine.payload);
      const fresh = new FakeRes();
      await handle(req("POST", "/year-hexagram", {}, { authorization: "Bearer token-u2" }), fresh);
      assert.deepEqual([fresh.code, fresh.payload], [409, { error: "campaign_inactive" }]);
    });

    it("refuses a new cast when there is no valid campaign", async () => {
      cfg = { ...cfg, campaign: null };
      const res = new FakeRes();
      await handle(req("POST", "/year-hexagram"), res);
      assert.deepEqual([res.code, res.payload], [409, { error: "campaign_inactive" }]);
    });

    it("GET returns 404 before a cast", async () => {
      const res = new FakeRes();
      await handle(req("GET", "/year-hexagram"), res);
      assert.deepEqual([res.code, res.payload], [404, { error: "year_hexagram_not_found" }]);
    });

    it("rate limits a new cast right after another reading", async () => {
      store.users.set("u1", { ...normalizeUser(undefined, bangkokDay(now)), lastReadingAt: now - 3000 });
      const res = new FakeRes();
      await handle(req("POST", "/year-hexagram"), res);
      assert.deepEqual([res.code, res.payload], [429, { error: "rate_limited", retryAfterSeconds: 5 }]);
      assert.equal(await store.getYearHexagram("u1", 2027), null);
    });
  });

  it("rejects missing auth and App Check", async () => {
    const r1 = new FakeRes();
    await handle(req("GET", "/status", {}, { authorization: "" }), r1);
    assert.equal(r1.code, 401);
    const r2 = new FakeRes();
    await handle(req("GET", "/status", {}, { "x-firebase-appcheck": "nope" }), r2);
    assert.deepEqual([r2.code, r2.payload], [401, { error: "app_check_invalid" }]);
  });

  it("casts on the server, streams the reading and charges the free quota", async () => {
    const res = new FakeRes();
    await handle(req("POST", "/divine", { question: "งานใหม่ดีไหม", category: "work" }), res);
    const ev = res.events;
    assert.equal(ev[0].event, "cast");
    assert.equal((ev[0].data.lines as number[]).length, 6);
    assert.equal((ev[0].data.coins as number[][]).length, 6);
    assert.deepEqual(ev.filter((e) => e.event === "delta").map((e) => e.data.text), ["ภูเขา", "ตั้งมั่น"]);
    const done = ev.at(-1)!;
    assert.equal(done.event, "done");
    assert.equal((done.data.status as { readingsLeft: number }).readingsLeft, 0);
    assert.deepEqual(modelCalls, [DEFAULT_CONFIG.freeModel]);
    const saved = await store.getReading("u1", ev[0].data.readingId as string);
    assert.equal(saved?.aiText, "ภูเขาตั้งมั่น");
    assert.equal(saved?.category, "work");
  });

  it("keeps the share summary out of the stream and hands it over at the end", async () => {
    modelChunks = ["ภูเขา", "ตั้งมั่น\n[สร", "ุป] ใจนิ่งย่อมเห็นทาง"];
    const res = new FakeRes();
    await handle(req("POST", "/divine", { question: "งานใหม่ดีไหม" }), res);
    const ev = res.events;
    const streamed = ev.filter((e) => e.event === "delta").map((e) => e.data.text).join("");
    assert.equal(streamed.trim(), "ภูเขาตั้งมั่น");
    assert.equal(ev.at(-1)!.data.shareSummary, "ใจนิ่งย่อมเห็นทาง");
    const saved = await store.getReading("u1", ev[0].data.readingId as string);
    assert.equal(saved?.aiText, "ภูเขาตั้งมั่น");
    assert.equal(saved?.shareSummary, "ใจนิ่งย่อมเห็นทาง");

    const replay = new FakeRes();
    await handle(req("POST", "/divine", { readingId: ev[0].data.readingId }), replay);
    assert.equal(replay.events.at(-1)!.data.shareSummary, "ใจนิ่งย่อมเห็นทาง");
  });

  it("finishes a reading normally when the model writes no summary", async () => {
    const res = new FakeRes();
    await handle(req("POST", "/divine", { question: "งานใหม่ดีไหม" }), res);
    const ev = res.events;
    assert.equal(ev.at(-1)!.data.shareSummary, null);
    assert.equal((await store.getReading("u1", ev[0].data.readingId as string))?.aiText, "ภูเขาตั้งมั่น");
  });

  it("when quota is used up it still casts but returns limited instead of calling the model", async () => {
    await handle(req("POST", "/divine", { question: "a" }), new FakeRes());
    now += 60_000;
    const res = new FakeRes();
    await handle(req("POST", "/divine", { question: "b" }), res);
    assert.deepEqual(res.events.map((e) => e.event), ["cast", "limited"]);
    assert.equal(res.events[1].data.reason, "quota");
    assert.equal(modelCalls.length, 1);

    // Watching an ad unlocks the same reading via readingId, without a new cast.
    const ad = new FakeRes();
    await handle(req("POST", "/ad-reward"), ad);
    assert.equal((ad.payload as { granted: boolean }).granted, true);
    now += 60_000;
    const again = new FakeRes();
    await handle(req("POST", "/divine", { readingId: res.events[0].data.readingId }), again);
    assert.deepEqual(again.events.map((e) => e.event), ["cast", "delta", "delta", "done"]);
    assert.deepEqual(again.events[0].data.lines, res.events[0].data.lines);
  });

  it("refunds quota when the model fails", async () => {
    modelMode = "fail";
    const res = new FakeRes();
    await handle(req("POST", "/divine", { question: "a" }), res);
    assert.deepEqual(res.events.map((e) => e.event), ["cast", "error"]);
    const st = new FakeRes();
    await handle(req("GET", "/status"), st);
    assert.equal((st.payload as { readingsLeft: number }).readingsLeft, 1);
  });

  it("logs why a request was rejected, without its credentials", async () => {
    const warnings: { message: string; data: Record<string, unknown> }[] = [];
    deps.log.warn = (message, data) => void warnings.push({ message, data: (data ?? {}) as Record<string, unknown> });
    await handle(req("GET", "/status", {}, { "x-firebase-appcheck": "nope" }), new FakeRes());
    assert.deepEqual(warnings, [{ message: "request_rejected", data: { code: "app_check_invalid", path: "/status" } }]);
  });

  it("logs the model error where the logger cannot overwrite it", async () => {
    const warnings: Record<string, unknown>[] = [];
    deps.log.warn = (_message, data) => void warnings.push((data ?? {}) as Record<string, unknown>);
    modelMode = "fail";
    await handle(req("POST", "/divine", { question: "a" }), new FakeRes());
    assert.equal(warnings[0].detail, "busy");
    assert.equal("message" in warnings[0], false);
  });

  it("answers crisis questions with hotlines, no cast and no charge", async () => {
    const res = new FakeRes();
    await handle(req("POST", "/divine", { question: "ไม่อยากมีชีวิตอยู่แล้ว" }), res);
    assert.deepEqual(res.events.map((e) => e.event), ["safety"]);
    assert.ok((res.events[0].data.message as string).includes("1323"));
    assert.equal(modelCalls.length, 0);
  });

  it("replays an interpreted reading without charging again", async () => {
    const first = new FakeRes();
    await handle(req("POST", "/divine", { question: "a" }), first);
    const replay = new FakeRes();
    await handle(req("POST", "/divine", { readingId: first.events[0].data.readingId }), replay);
    assert.equal(replay.events[1].data.text, "ภูเขาตั้งมั่น");
    assert.equal(modelCalls.length, 1);
  });

  describe("connection dropped after some words", () => {
    const dropOnce = async (body: Record<string, unknown>) => {
      modelMode = "drop";
      droppingRes = new FakeRes();
      await handle(req("POST", "/divine", body), droppingRes);
      modelMode = "ok";
      now += 60_000;
      return droppingRes;
    };
    const freeUsed = () => store.users.get("u1")!.usage.free;
    /** Every other way of paying is ruled out, so a second charge would come back "limited". */
    const retry = async (id: unknown) => {
      const res = new FakeRes();
      await handle(req("POST", "/divine", { readingId: id }), res);
      return res;
    };

    it("does not keep half a reading as the finished one", async () => {
      const first = await dropOnce({ question: "a" });
      const saved = await store.getReading("u1", first.events[0].data.readingId as string);
      assert.equal(saved?.aiText, null);
    });

    it("regenerates the full reading on retry without charging again", async () => {
      const first = await dropOnce({ question: "a" });
      const again = await retry(first.events[0].data.readingId);
      assert.deepEqual(again.events.map((e) => e.event), ["cast", "delta", "delta", "done"]);
      assert.equal(freeUsed(), 1);
      const saved = await store.getReading("u1", first.events[0].data.readingId as string);
      assert.equal(saved?.aiText, "ภูเขาตั้งมั่น");
    });

    it("respects the rate limit on the free retry", async () => {
      const first = await dropOnce({ question: "a" });
      const id = first.events[0].data.readingId;
      now -= 58_000; // two seconds after the charge, inside minSecondsBetweenReadings
      const tooSoon = await retry(id);
      assert.equal(tooSoon.events.at(-1)!.data.reason, "rate_limited");
      assert.equal(modelCalls.length, 1);
      now += 60_000;
      assert.equal((await retry(id)).events.at(-1)!.event, "done");
      assert.equal(freeUsed(), 1);
    });

    it("charges again once the free retries for a reading run out", async () => {
      const first = await dropOnce({ question: "a" });
      const id = first.events[0].data.readingId;
      await dropOnce({ readingId: id });
      await dropOnce({ readingId: id });
      assert.equal(freeUsed(), 1);
      const limited = await retry(id);
      assert.deepEqual(limited.events.map((e) => e.event), ["cast", "limited"]);
      assert.equal(limited.events[1].data.reason, "quota");
    });

    it("refunds a free retry that fails, so the next attempt pays once", async () => {
      const first = await dropOnce({ question: "a" });
      const id = first.events[0].data.readingId;
      modelMode = "fail";
      await retry(id);
      assert.equal(freeUsed(), 0);
      modelMode = "ok";
      now += 60_000;
      const again = await retry(id);
      assert.equal(again.events.at(-1)!.event, "done");
      assert.equal(freeUsed(), 1);
    });
  });

  it("deletes one reading, and only the caller's", async () => {
    const mine = new FakeRes();
    await handle(req("POST", "/divine", { question: "a" }), mine);
    const id = mine.events[0].data.readingId as string;
    const other = new FakeRes();
    await handle(req("DELETE", `/readings/${id}`, {}, { authorization: "Bearer token-u2" }), other);
    assert.equal(other.code, 200);
    assert.notEqual(await store.getReading("u1", id), null);
    const res = new FakeRes();
    await handle(req("DELETE", `/readings/${id}`), res);
    assert.equal(res.code, 200);
    assert.equal(await store.getReading("u1", id), null);
  });

  it("revenuecat webhook finds the Firebase uid among the aliases", async () => {
    entitlements = { premiumUntil: now + 86_400_000, coinPurchases: [] };
    const hook = createRevenueCatWebhook(deps, () => "s3cret");
    const anonymous = "$RCAnonymousID:abc";
    await hook(
      req("POST", "/", { event: { type: "RENEWAL", app_user_id: anonymous, original_app_user_id: anonymous, aliases: [anonymous, "u1"] } }, { authorization: "Bearer s3cret" }),
      new FakeRes(),
    );
    assert.equal(store.users.get("u1")?.premiumUntil, now + 86_400_000);
    assert.equal(store.users.has(anonymous), false);
  });

  it("follow-ups are premium-only and capped per reading", async () => {
    const first = new FakeRes();
    await handle(req("POST", "/divine", { question: "a" }), first);
    const id = first.events[0].data.readingId;
    const denied = new FakeRes();
    await handle(req("POST", "/follow-up", { readingId: id, message: "แล้วควรทำอย่างไร" }), denied);
    assert.deepEqual([denied.code, denied.payload], [403, { error: "premium_required" }]);

    entitlements = { premiumUntil: now + 30 * 86_400_000, coinPurchases: [] };
    await handle(req("POST", "/purchases/sync"), new FakeRes());
    for (let i = 0; i < 3; i++) {
      const r = new FakeRes();
      await handle(req("POST", "/follow-up", { readingId: id, message: `ข้อ ${i}` }), r);
      assert.equal(r.events.at(-1)!.event, "done");
    }
    const over = new FakeRes();
    await handle(req("POST", "/follow-up", { readingId: id, message: "อีก" }), over);
    assert.equal(over.events[0].data.reason, "follow_up_limit");
    assert.equal((await store.getReading("u1", id as string))!.followUps.length, 3);
  });

  it("journal sync gives free accounts their recent days and Premium everything; outcome notes are saved", async () => {
    const first = new FakeRes();
    await handle(req("POST", "/divine", { question: "a" }), first);
    const id = first.events[0].data.readingId;
    const out = new FakeRes();
    await handle(req("POST", `/readings/${id}/outcome`, { outcome: "yes", note: "ได้งานจริง" }), out);
    assert.equal(out.code, 200);
    store.readings.get(`u1/${id}`)!.createdAt = now - 30 * 86_400_000;
    const second = new FakeRes();
    await handle(req("POST", "/divine", { question: "b" }), second);
    const recentId = second.events[0].data.readingId;
    const free = new FakeRes();
    await handle(req("GET", "/readings"), free);
    assert.equal(free.code, 200);
    assert.deepEqual((free.payload as { readings: { id: string }[] }).readings.map((r) => r.id), [recentId]);
    entitlements = { premiumUntil: now + 86_400_000, coinPurchases: [] };
    await handle(req("POST", "/purchases/sync"), new FakeRes());
    const list = new FakeRes();
    await handle(req("GET", "/readings"), list);
    const readings = (list.payload as { readings: { outcome: string; outcomeNote: string }[] }).readings;
    assert.equal(readings[0].outcome, "yes");
    assert.equal(readings[0].outcomeNote, "ได้งานจริง");
  });

  it("keeps the streak per account so a reinstall gets it back", async () => {
    const today = Math.floor((now + 7 * 3_600_000) / 86_400_000);
    const sent = new FakeRes();
    await handle(req("POST", "/streak", { current: 8, best: 8, lastEpochDay: today, unlocked: ["ink_paper", "bamboo", "bogus"] }), sent);
    assert.equal(sent.code, 200);
    const fresh = new FakeRes();
    await handle(req("POST", "/streak", { current: 0, best: 0, lastEpochDay: null, unlocked: [] }), fresh);
    assert.deepEqual((fresh.payload as { streak: unknown }).streak, { current: 8, best: 8, lastEpochDay: today, unlocked: ["ink_paper", "bamboo"] });
  });

  it("refuses a streak from a clock set in the future", async () => {
    const today = Math.floor((now + 7 * 3_600_000) / 86_400_000);
    const res = new FakeRes();
    await handle(req("POST", "/streak", { current: 300, best: 300, lastEpochDay: today + 30, unlocked: [] }), res);
    assert.equal(res.code, 400);
  });

  describe("lapsed premium", () => {
    const premium = async () => {
      const res = new FakeRes();
      await handle(req("GET", "/status"), res);
      assert.equal(res.code, 200);
      return (res.payload as { premium: boolean }).premium;
    };
    let fetches: number;

    beforeEach(async () => {
      entitlements = { premiumUntil: now + 1000, coinPurchases: [] };
      await handle(req("POST", "/purchases/sync"), new FakeRes());
      fetches = 0;
      deps.fetchEntitlements = async () => {
        fetches++;
        return entitlements;
      };
      now += 2000;
    });

    it("keeps premium when the store renewed without telling us", async () => {
      entitlements = { premiumUntil: now + 86_400_000, coinPurchases: [] };
      assert.equal(await premium(), true);
      assert.equal(await premium(), true);
      assert.equal(fetches, 1);
    });

    it("ends premium once, then stops asking the store", async () => {
      entitlements = { premiumUntil: null, coinPurchases: [] };
      assert.equal(await premium(), false);
      assert.equal(await premium(), false);
      assert.equal(fetches, 1);
    });

    it("still answers when the store cannot be reached", async () => {
      deps.fetchEntitlements = async () => {
        throw new Error("RevenueCat HTTP 429");
      };
      assert.equal(await premium(), false);
    });

    it("does not ask the store for someone who never subscribed", async () => {
      store.users.delete("u1");
      await premium();
      assert.equal(fetches, 0);
    });
  });

  it("coin purchases are granted once even if synced twice", async () => {
    entitlements = { premiumUntil: null, coinPurchases: [{ transactionId: "GPA.1", productId: "iching_coins_10" }] };
    await handle(req("POST", "/purchases/sync"), new FakeRes());
    const res = new FakeRes();
    await handle(req("POST", "/purchases/sync"), res);
    assert.equal((res.payload as { coins: number }).coins, 10);
  });

  it("deletes the account", async () => {
    await handle(req("POST", "/divine", { question: "a" }), new FakeRes());
    const res = new FakeRes();
    await handle(req("DELETE", "/account"), res);
    assert.equal(res.code, 200);
    assert.equal(store.readings.size, 0);
  });

  it("revenuecat webhook checks its secret and re-syncs the user", async () => {
    entitlements = { premiumUntil: now + 86_400_000, coinPurchases: [] };
    const hook = createRevenueCatWebhook(deps, () => "s3cret");
    const bad = new FakeRes();
    await hook(req("POST", "/", { event: { type: "INITIAL_PURCHASE", app_user_id: "u1" } }, { authorization: "Bearer x" }), bad);
    assert.equal(bad.code, 401);
    const ok = new FakeRes();
    await hook(req("POST", "/", { event: { type: "INITIAL_PURCHASE", app_user_id: "u1" } }, { authorization: "Bearer s3cret" }), ok);
    assert.equal(ok.code, 200);
    assert.equal(store.users.get("u1")?.premiumUntil, now + 86_400_000);
  });

  it("parses RevenueCat subscriber JSON", () => {
    const e = parseEntitlements(
      {
        subscriber: {
          entitlements: { iching_premium: { expires_date: "2026-12-01T00:00:00Z" } },
          non_subscriptions: { iching_coins_10: [{ id: "a", store_transaction_id: "tx1" }] },
        },
      },
      now,
    );
    assert.equal(e.premiumUntil, Date.parse("2026-12-01T00:00:00Z"));
    assert.deepEqual(e.coinPurchases, [{ transactionId: "tx1", productId: "iching_coins_10" }]);
    assert.equal(parseEntitlements({ subscriber: { entitlements: { iching_premium: { expires_date: "2020-01-01T00:00:00Z" } } } }, now).premiumUntil, null);
  });

  describe("accounts and money", () => {
    const send = async (method: string, path: string, body: unknown = {}, headers: Record<string, string> = {}) => {
      const res = new FakeRes();
      await handle(req(method, path, body, headers), res);
      return res;
    };
    const statusOf = async (uid = "u1") =>
      (await send("GET", "/status", {}, { authorization: `Bearer token-${uid}`, "x-device-id": `device-of-${uid}` })).payload as {
        premium: boolean;
        coins: number;
        readingsLeft: number;
      };
    const coinPack = (transactionId: string) => ({ transactionId, productId: "iching_coins_10" });
    const buyCoins = async (...ids: string[]) => {
      entitlements = { premiumUntil: null, coinPurchases: ids.map(coinPack) };
      await send("POST", "/purchases/sync");
    };
    /** Spends today's free reading so the next one has to be paid with a coin. */
    const useFreeReading = async () => {
      await send("POST", "/divine", { question: "ข้อแรก" });
      now += 60_000;
    };
    const hook = () => createRevenueCatWebhook(deps, () => "s3cret");
    const fireWebhook = async (event: object) => {
      const res = new FakeRes();
      await hook()(req("POST", "/", { event }, { authorization: "Bearer s3cret" }), res);
      return res;
    };

    describe("one account never reaches another", () => {
      const asOther = { authorization: "Bearer token-u2", "x-device-id": "device-of-u2" };

      it("cannot interpret, annotate or list someone else's reading", async () => {
        const mine = await send("POST", "/divine", { question: "เรื่องส่วนตัว" });
        const id = mine.events[0].data.readingId as string;
        assert.equal((await send("POST", "/divine", { readingId: id }, asOther)).code, 404);
        assert.equal((await send("POST", `/readings/${id}/outcome`, { outcome: "yes" }, asOther)).code, 404);
        entitlements = { premiumUntil: now + 86_400_000, coinPurchases: [] };
        await send("POST", "/purchases/sync", {}, asOther);
        assert.equal((await send("POST", "/follow-up", { readingId: id, message: "ขอดูหน่อย" }, asOther)).code, 404);
        const list = await send("GET", "/readings", {}, asOther);
        assert.deepEqual((list.payload as { readings: unknown[] }).readings, []);
        assert.equal((await store.getReading("u1", id))?.outcome, null);
      });

      it("spends only the caller's coins", async () => {
        await buyCoins("GPA.1");
        await send("POST", "/divine", { question: "ฟรี" }, asOther);
        now += 60_000;
        const denied = await send("POST", "/divine", { question: "จ่ายด้วยเหรียญใคร" }, asOther);
        assert.equal(denied.events.at(-1)!.event, "limited");
        assert.equal((await statusOf()).coins, 10);
      });

      it("refuses a forged token on the routes that change money or delete data", async () => {
        let authDeletes = 0;
        deps.deleteAuthUser = async () => void authDeletes++;
        await buyCoins("GPA.1");
        const forged = { authorization: "Bearer forged" };
        assert.equal((await send("POST", "/purchases/sync", {}, forged)).code, 401);
        assert.equal((await send("POST", "/ad-reward", {}, forged)).code, 401);
        assert.equal((await send("DELETE", "/account", {}, forged)).code, 401);
        assert.equal(authDeletes, 0);
        assert.equal((await statusOf()).coins, 10);
      });

      it("refuses a missing App Check token on purchases", async () => {
        const res = new FakeRes();
        const noAppCheck: HttpReq = { method: "POST", path: "/purchases/sync", body: {}, query: {}, get: (n) => (n === "authorization" ? "Bearer token-u1" : undefined) };
        await handle(noAppCheck, res);
        assert.deepEqual([res.code, res.payload], [401, { error: "app_check_missing" }]);
      });
    });

    describe("paying for a reading with coins", () => {
      beforeEach(async () => {
        await buyCoins("GPA.1");
        await useFreeReading();
        modelCalls = [];
      });

      it("takes exactly one coin and uses the paid model", async () => {
        const res = await send("POST", "/divine", { question: "ข้อสอง" });
        assert.equal(res.events.at(-1)!.event, "done");
        assert.equal((await statusOf()).coins, 9);
        assert.deepEqual(modelCalls, [DEFAULT_CONFIG.premiumModel]);
      });

      it("gives the coin back when the model fails", async () => {
        modelMode = "fail";
        const res = await send("POST", "/divine", { question: "ข้อสอง" });
        assert.deepEqual(res.events.at(-1)!.data, { code: "rate_limited", refunded: true });
        assert.equal((await statusOf()).coins, 10);
      });

      it("does not take a second coin when a finished reading is replayed", async () => {
        const first = await send("POST", "/divine", { question: "ข้อสอง" });
        now += 60_000;
        await send("POST", "/divine", { readingId: first.events[0].data.readingId });
        assert.equal((await statusOf()).coins, 9);
      });

      it("does not take a second coin when the retry after a dropped connection succeeds", async () => {
        modelMode = "drop";
        droppingRes = new FakeRes();
        await handle(req("POST", "/divine", { question: "ข้อสอง" }), droppingRes);
        assert.equal((await statusOf()).coins, 9);
        modelMode = "ok";
        now += 60_000;
        const again = await send("POST", "/divine", { readingId: droppingRes.events[0].data.readingId });
        assert.equal(again.events.at(-1)!.event, "done");
        assert.equal((await statusOf()).coins, 9);
      });

      it("takes nothing while the AI is switched off", async () => {
        cfg = { ...cfg, aiEnabled: false };
        const res = await send("POST", "/divine", { question: "ข้อสอง" });
        assert.equal(res.events.at(-1)!.data.reason, "ai_disabled");
        assert.equal((await statusOf()).coins, 10);
      });

      it("takes nothing for a crisis question", async () => {
        await send("POST", "/divine", { question: "ไม่อยากมีชีวิตอยู่แล้ว" });
        assert.equal((await statusOf()).coins, 10);
      });

      it("premium readings leave coins alone until the daily allowance runs out", async () => {
        entitlements = { premiumUntil: now + 86_400_000, coinPurchases: [coinPack("GPA.1")] };
        await send("POST", "/purchases/sync");
        for (let i = 0; i < DEFAULT_CONFIG.premiumDailyReadings; i++) {
          now += 60_000;
          await send("POST", "/divine", { question: `ข้อ ${i}` });
        }
        assert.equal((await statusOf()).coins, 10);
        now += 60_000;
        await send("POST", "/divine", { question: "เกินโควตา" });
        assert.equal((await statusOf()).coins, 9);
      });
    });

    describe("syncing purchases", () => {
      it("reports 503 and changes nothing when RevenueCat is not configured", async () => {
        await buyCoins("GPA.1");
        deps.fetchEntitlements = null;
        assert.deepEqual((await send("POST", "/purchases/sync")).payload, { error: "purchases_not_configured" });
        assert.equal((await statusOf()).coins, 10);
      });

      it("keeps premium and coins when RevenueCat is down, then recovers", async () => {
        entitlements = { premiumUntil: now + 86_400_000, coinPurchases: [coinPack("GPA.1")] };
        await send("POST", "/purchases/sync");
        const working = deps.fetchEntitlements;
        deps.fetchEntitlements = async () => {
          throw new Error("RevenueCat HTTP 500");
        };
        assert.equal((await send("POST", "/purchases/sync")).code, 500);
        assert.deepEqual([(await statusOf()).premium, (await statusOf()).coins], [true, 10]);
        deps.fetchEntitlements = working;
        assert.equal((await send("POST", "/purchases/sync")).code, 200);
        assert.equal((await statusOf()).coins, 10);
      });

      it("credits each pack once, including after a failure halfway through", async () => {
        const grant = store.grantCoins.bind(store);
        let failNext = false;
        store.grantCoins = async (uid, tx, coins) => {
          if (tx === "GPA.2" && failNext) {
            failNext = false;
            throw new Error("firestore unavailable");
          }
          return grant(uid, tx, coins);
        };
        failNext = true;
        entitlements = { premiumUntil: null, coinPurchases: [coinPack("GPA.1"), coinPack("GPA.2")] };
        assert.equal((await send("POST", "/purchases/sync")).code, 500);
        assert.equal((await statusOf()).coins, 10);
        assert.equal((await send("POST", "/purchases/sync")).code, 200);
        assert.equal((await statusOf()).coins, 20);
      });

      it("credits nothing for a product it does not know", async () => {
        entitlements = { premiumUntil: null, coinPurchases: [{ transactionId: "GPA.9", productId: "iching_coins_9999" }] };
        await send("POST", "/purchases/sync");
        assert.equal((await statusOf()).coins, 0);
      });

      it("ending a subscription keeps the coins the seeker paid for", async () => {
        entitlements = { premiumUntil: now + 86_400_000, coinPurchases: [coinPack("GPA.1")] };
        await send("POST", "/purchases/sync");
        entitlements = { premiumUntil: null, coinPurchases: [coinPack("GPA.1")] };
        await send("POST", "/purchases/sync");
        assert.deepEqual([(await statusOf()).premium, (await statusOf()).coins], [false, 10]);
      });
    });

    describe("revenuecat webhook", () => {
      it("is closed when no secret is configured", async () => {
        const res = new FakeRes();
        await createRevenueCatWebhook(deps, () => "")(req("POST", "/", { event: { type: "RENEWAL", app_user_id: "u1" } }, { authorization: "Bearer " }), res);
        assert.equal(res.code, 401);
      });

      it("does not touch accounts for a dashboard test event", async () => {
        entitlements = { premiumUntil: now + 86_400_000, coinPurchases: [] };
        assert.equal((await fireWebhook({ type: "TEST", app_user_id: "u1" })).code, 200);
        assert.equal(store.users.has("u1"), false);
      });

      it("asks to be retried when RevenueCat is down, and the retry lands once", async () => {
        const working: Deps["fetchEntitlements"] = async () => ({ premiumUntil: null, coinPurchases: [coinPack("GPA.1")] });
        deps.fetchEntitlements = async () => {
          throw new Error("RevenueCat HTTP 503");
        };
        assert.equal((await fireWebhook({ type: "NON_RENEWING_PURCHASE", app_user_id: "u1" })).code, 500);
        deps.fetchEntitlements = working;
        await fireWebhook({ type: "NON_RENEWING_PURCHASE", app_user_id: "u1" });
        await fireWebhook({ type: "NON_RENEWING_PURCHASE", app_user_id: "u1" });
        assert.equal((await statusOf()).coins, 10);
      });

      it("moves premium with a transfer, from the old account to the new one", async () => {
        deps.fetchEntitlements = async () => ({ premiumUntil: now + 86_400_000, coinPurchases: [] });
        await send("POST", "/purchases/sync");
        assert.equal((await statusOf()).premium, true);
        deps.fetchEntitlements = async (uid) => ({ premiumUntil: uid === "u2" ? now + 86_400_000 : null, coinPurchases: [] });
        await fireWebhook({ type: "TRANSFER", transferred_from: ["u1"], transferred_to: ["u2"] });
        assert.equal((await statusOf("u1")).premium, false);
        assert.equal((await statusOf("u2")).premium, true);
      });

      it("ends premium on a refund or expiry", async () => {
        entitlements = { premiumUntil: now + 86_400_000, coinPurchases: [] };
        await send("POST", "/purchases/sync");
        entitlements = { premiumUntil: null, coinPurchases: [] };
        await fireWebhook({ type: "CANCELLATION", cancel_reason: "CUSTOMER_SUPPORT", app_user_id: "u1" });
        assert.equal((await statusOf()).premium, false);
      });

      describe("a refunded coin pack", () => {
        const refund = (over: object = {}) =>
          fireWebhook({ type: "CANCELLATION", cancel_reason: "CUSTOMER_SUPPORT", app_user_id: "u1", product_id: "iching_coins_10", transaction_id: "GPA.1", ...over });

        it("takes the coins back once, however often the event is delivered", async () => {
          await buyCoins("GPA.1", "GPA.2");
          assert.equal((await refund()).code, 200);
          await refund();
          assert.equal((await statusOf()).coins, 10);
        });

        it("stops at zero when the coins were already spent", async () => {
          await buyCoins("GPA.1");
          store.users.get("u1")!.coins = 3;
          await refund();
          assert.equal((await statusOf()).coins, 0);
        });

        it("is not credited again by a later sync, even if RevenueCat still lists it", async () => {
          await buyCoins("GPA.1");
          await refund();
          await send("POST", "/purchases/sync");
          assert.equal((await statusOf()).coins, 0);
        });

        it("is never credited when the refund arrives first", async () => {
          await refund();
          await buyCoins("GPA.1");
          assert.equal((await statusOf()).coins, 0);
        });

        it("leaves coins alone when a subscription is cancelled", async () => {
          await buyCoins("GPA.1");
          await refund({ product_id: "iching_premium_monthly:monthly", transaction_id: "GPA.sub" });
          assert.equal((await statusOf()).coins, 10);
        });
      });

      it("writes nothing for an event that only names RevenueCat anonymous ids", async () => {
        entitlements = { premiumUntil: now + 86_400_000, coinPurchases: [] };
        await fireWebhook({ type: "RENEWAL", app_user_id: "$RCAnonymousID:a", aliases: ["$RCAnonymousID:a"] });
        assert.equal(store.users.size, 0);
      });
    });

    describe("merging an anonymous account into the one signed into", () => {
      const asNewDevice = { authorization: "Bearer token-anon", "x-device-id": "device-of-anon" };
      const merge = (fromIdToken: unknown) => send("POST", "/account/merge", { fromIdToken });
      let deletedLogins: string[];

      beforeEach(async () => {
        deletedLogins = [];
        deps.deleteAuthUser = async (uid) => void deletedLogins.push(uid);
        entitlements = { premiumUntil: null, coinPurchases: [coinPack("GPA.anon")] };
        await send("POST", "/purchases/sync", {}, asNewDevice);
      });

      it("moves the coins and readings, then removes the anonymous account", async () => {
        const reading = await send("POST", "/divine", { question: "ก่อนล็อกอิน" }, asNewDevice);
        const id = reading.events[0].data.readingId as string;
        const res = await merge("token-anon");
        assert.equal(res.code, 200);
        assert.equal((res.payload as { coins: number }).coins, 10);
        assert.notEqual(await store.getReading("u1", id), null);
        assert.equal(await store.getReading("anon", id), null);
        assert.equal(store.users.has("anon"), false);
        assert.deepEqual(deletedLogins, ["anon"]);
      });

      it("carries the anonymous account's streak and themes over", async () => {
        store.streaks.set("anon", { current: 9, best: 9, lastEpochDay: 20_000, unlocked: ["ink_paper", "bamboo"] });
        await merge("token-anon");
        assert.deepEqual(store.streaks.get("u1"), { current: 9, best: 9, lastEpochDay: 20_000, unlocked: ["ink_paper", "bamboo"] });
        assert.equal(store.streaks.has("anon"), false);
      });

      it("does not double the coins when the app repeats the call", async () => {
        await merge("token-anon");
        deps.isAnonymousUser = async () => null;
        assert.equal((await merge("token-anon")).code, 200);
        assert.equal((await statusOf()).coins, 10);
      });

      it("refuses to absorb an account that has its own login", async () => {
        deps.isAnonymousUser = async () => false;
        assert.deepEqual((await merge("token-anon")).payload, { error: "merge_refused" });
        assert.equal(store.users.get("anon")?.coins, 10);
        assert.deepEqual(deletedLogins, []);
      });

      it("refuses a forged or missing source token", async () => {
        assert.equal((await merge("forged")).code, 403);
        assert.equal((await merge(undefined)).code, 400);
        assert.equal(store.users.get("anon")?.coins, 10);
      });

      it("refuses merging an account into itself", async () => {
        assert.equal((await merge("token-u1")).code, 400);
      });

      it("needs the destination to be logged in", async () => {
        const res = await send("POST", "/account/merge", { fromIdToken: "token-anon" }, { authorization: "Bearer forged" });
        assert.equal(res.code, 401);
        assert.equal(store.users.get("anon")?.coins, 10);
      });
    });

    describe("deleting the account", () => {
      let authUsers: Set<string>;
      let purchasersDeleted: string[];

      beforeEach(() => {
        authUsers = new Set(["u1"]);
        purchasersDeleted = [];
        deps.deletePurchaser = async (uid) => void purchasersDeleted.push(uid);
        deps.deleteAuthUser = async (uid) => {
          // Firebase Admin throws this when the user is already gone.
          if (!authUsers.delete(uid)) throw Object.assign(new Error("no user"), { code: "auth/user-not-found" });
        };
      });

      it("removes coins, premium, readings, the RevenueCat customer and the login", async () => {
        entitlements = { premiumUntil: now + 86_400_000, coinPurchases: [coinPack("GPA.1")] };
        await send("POST", "/purchases/sync");
        await send("POST", "/divine", { question: "ลบฉันด้วย" });
        assert.equal((await send("DELETE", "/account")).code, 200);
        assert.equal(store.users.has("u1"), false);
        assert.equal(store.readings.size, 0);
        assert.deepEqual(purchasersDeleted, ["u1"]);
        assert.equal(authUsers.has("u1"), false);
      });

      it("leaves everyone else's data alone", async () => {
        const other = { authorization: "Bearer token-u2", "x-device-id": "device-of-u2" };
        await send("POST", "/divine", { question: "ของคนอื่น" }, other);
        await send("DELETE", "/account");
        assert.equal(store.readings.size, 1);
      });

      it("still deletes the login when RevenueCat is down", async () => {
        deps.deletePurchaser = async () => {
          throw new Error("RevenueCat HTTP 500");
        };
        assert.equal((await send("DELETE", "/account")).code, 200);
        assert.equal(authUsers.has("u1"), false);
      });

      it("reports failure when the login could not be deleted, and the retry finishes the job", async () => {
        const realDelete = deps.deleteAuthUser;
        deps.deleteAuthUser = async () => {
          throw new Error("auth unavailable");
        };
        assert.equal((await send("DELETE", "/account")).code, 500);
        deps.deleteAuthUser = realDelete;
        assert.equal((await send("DELETE", "/account")).code, 200);
        assert.equal(authUsers.has("u1"), false);
      });

      it("treats a repeat as success when the first answer was lost on the way back", async () => {
        assert.equal((await send("DELETE", "/account")).code, 200);
        // The app never saw that 200 and retries with its still-valid ID token.
        assert.equal((await send("DELETE", "/account")).code, 200);
      });

      it("a late webhook does not bring a deleted account's data back", async () => {
        await buyCoins("GPA.1");
        await send("DELETE", "/account");
        entitlements = { premiumUntil: null, coinPurchases: [] };
        await fireWebhook({ type: "EXPIRATION", app_user_id: "u1" });
        assert.equal(store.users.has("u1"), false);
      });
    });

    describe("revenuecat client", () => {
      const respond = (status: number, body: unknown = {}) => {
        const calls: { url: string; init?: RequestInit }[] = [];
        const fetchImpl = (async (url: string, init?: RequestInit) => {
          calls.push({ url, init });
          return new Response(JSON.stringify(body), { status });
        }) as unknown as typeof fetch;
        return { calls, client: revenueCatClient(() => "sk_key", () => now, fetchImpl) };
      };

      it("asks for the encoded uid with the secret key", async () => {
        const { calls, client } = respond(200, { subscriber: {} });
        await client.fetchEntitlements("a/b c");
        assert.equal(calls[0].url, "https://api.revenuecat.com/v1/subscribers/a%2Fb%20c");
        assert.equal((calls[0].init!.headers as Record<string, string>).Authorization, "Bearer sk_key");
      });

      it("throws on an outage rather than reporting no entitlements", async () => {
        await assert.rejects(respond(500).client.fetchEntitlements("u1"));
        await assert.rejects(respond(401).client.fetchEntitlements("u1"));
      });

      it("treats deleting an unknown customer as done, and an outage as failure", async () => {
        await respond(404).client.deleteSubscriber("u1");
        await assert.rejects(respond(500).client.deleteSubscriber("u1"));
      });
    });

    describe("parsing entitlements", () => {
      const premiumUntil = (entitlementsJson: Record<string, { expires_date?: string | null }>) =>
        parseEntitlements({ subscriber: { entitlements: entitlementsJson } }, now).premiumUntil;

      it("reads a lifetime grant as far in the future", () => {
        assert.ok(premiumUntil({ iching_premium: { expires_date: null } })! > now + 365 * 86_400_000);
      });

      it("gives nothing for a date it cannot read or another entitlement", () => {
        assert.equal(premiumUntil({ iching_premium: { expires_date: "not a date" } }), null);
        assert.equal(premiumUntil({ other: { expires_date: "2099-01-01T00:00:00Z" } }), null);
      });

      it("gives nothing for an empty answer", () => {
        assert.deepEqual(parseEntitlements({}, now), { premiumUntil: null, coinPurchases: [] });
      });

      it("skips a coin purchase that has no transaction id, since it could be credited repeatedly", () => {
        const e = parseEntitlements({ subscriber: { non_subscriptions: { iching_coins_10: [{}, { id: "rc1" }] } } }, now);
        assert.deepEqual(e.coinPurchases, [{ transactionId: "rc1", productId: "iching_coins_10" }]);
      });
    });
  });
});
