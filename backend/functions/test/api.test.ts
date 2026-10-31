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
  Store,
  StoredReading,
} from "../src/core/api";
import { GeminiError, Usage } from "../src/core/gemini";
import { loadScripture } from "../src/core/hexagrams";
import { bangkokDay, DEFAULT_CONFIG, DeviceDoc, normalizeDevice, normalizeUser, RuntimeConfig, UserDoc } from "../src/core/quota";

const scripture = loadScripture(resolve(__dirname, "../data/hexagrams.json"));

class MemoryStore implements Store {
  users = new Map<string, UserDoc>();
  devices = new Map<string, DeviceDoc>();
  readings = new Map<string, StoredReading>();
  reports: unknown[] = [];
  grants = new Set<string>();
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
  async listReadings(uid: string) {
    return [...this.readings.entries()].filter(([k]) => k.startsWith(`${uid}/`)).map(([, v]) => v);
  }
  async addReport(r: unknown) {
    this.reports.push(r);
  }
  async setPremiumUntil(uid: string, premiumUntil: number | null) {
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
  async deleteUser(uid: string) {
    this.users.delete(uid);
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
    droppingRes = null;
    cfg = { ...DEFAULT_CONFIG };
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
      now: () => now,
      log: { info() {}, warn() {}, error() {} },
    };
    handle = createApi(deps);
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

  it("journal sync is premium-only; outcome notes are saved", async () => {
    const first = new FakeRes();
    await handle(req("POST", "/divine", { question: "a" }), first);
    const id = first.events[0].data.readingId;
    const out = new FakeRes();
    await handle(req("POST", `/readings/${id}/outcome`, { outcome: "yes", note: "ได้งานจริง" }), out);
    assert.equal(out.code, 200);
    const denied = new FakeRes();
    await handle(req("GET", "/readings"), denied);
    assert.equal(denied.code, 403);
    entitlements = { premiumUntil: now + 86_400_000, coinPurchases: [] };
    await handle(req("POST", "/purchases/sync"), new FakeRes());
    const list = new FakeRes();
    await handle(req("GET", "/readings"), list);
    const readings = (list.payload as { readings: { outcome: string; outcomeNote: string }[] }).readings;
    assert.equal(readings[0].outcome, "yes");
    assert.equal(readings[0].outcomeNote, "ได้งานจริง");
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
          entitlements: { premium: { expires_date: "2026-12-01T00:00:00Z" } },
          non_subscriptions: { iching_coins_10: [{ id: "a", store_transaction_id: "tx1" }] },
        },
      },
      now,
    );
    assert.equal(e.premiumUntil, Date.parse("2026-12-01T00:00:00Z"));
    assert.deepEqual(e.coinPurchases, [{ transactionId: "tx1", productId: "iching_coins_10" }]);
    assert.equal(parseEntitlements({ subscriber: { entitlements: { premium: { expires_date: "2020-01-01T00:00:00Z" } } } }, now).premiumUntil, null);
  });
});
