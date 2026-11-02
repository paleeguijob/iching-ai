/**
 * The real Firestore layer (src/store.ts) and security rules, against the local emulator.
 * Run with: npm run test:emulator. Skipped by plain `npm test`, which has no emulator.
 */
import { strict as assert } from "node:assert";
import { beforeEach, describe, it } from "node:test";
import { initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { bangkokDay, chargeReading, DEFAULT_CONFIG, refundReading } from "../src/core/quota";
import { firestoreStore } from "../src/store";

const host = process.env.FIRESTORE_EMULATOR_HOST;
const projectId = "demo-iching";

describe("firestore store", { skip: !host && "needs the Firestore emulator" }, () => {
  const db = host ? getFirestore(initializeApp({ projectId }, "store-test")) : (null as never);
  const store = host ? firestoreStore(db) : (null as never);
  const cfg = { ...DEFAULT_CONFIG, minSecondsBetweenReadings: 0 };

  const userDoc = async (uid: string) => (await db.doc(`users/${uid}`).get()).data();
  const seed = (uid: string, coins: number) =>
    db.doc(`users/${uid}`).set({ coins, premiumUntil: null, usage: { day: bangkokDay(Date.now()), free: 1, premium: 0, adEarned: 0, adUsed: 0 }, lastReadingAt: null });
  const spendOne = (uid: string) =>
    store.quotaTx(uid, null, (user, device) => {
      const d = chargeReading(user, device, cfg, Date.now());
      return d.ok ? { user: d.user, device: d.device, result: d.charge as string } : { result: d.reason as string };
    });

  beforeEach(async () => {
    await fetch(`http://${host}/emulator/v1/projects/${projectId}/databases/(default)/documents`, { method: "DELETE" });
  });

  it("credits a pack once when the app and the webhook race", async () => {
    const results = await Promise.all(Array.from({ length: 10 }, () => store.grantCoins("u1", "GPA.1", 10)));
    assert.equal(results.filter(Boolean).length, 1);
    assert.equal((await userDoc("u1"))?.coins, 10);
  });

  it("credits a pack once across accounts, when RevenueCat transfers the purchase", async () => {
    assert.equal(await store.grantCoins("u1", "GPA.1", 10), true);
    assert.equal(await store.grantCoins("u2", "GPA.1", 10), false);
    assert.equal((await userDoc("u2"))?.coins ?? 0, 0);
  });

  it("keeps a transaction marked as credited after the account that bought it is deleted", async () => {
    await store.grantCoins("u1", "GPA.1", 10);
    await store.deleteUser("u1");
    assert.equal(await store.grantCoins("u2", "GPA.1", 10), false);
  });

  it("honours a marker written before markers moved out of the user document", async () => {
    await db.doc("users/u1/coinGrants/GPA_1").set({ coins: 10, at: 1 });
    assert.equal(await store.grantCoins("u1", "GPA.1", 10), false);
  });

  it("takes a refunded pack back once and never below zero", async () => {
    await store.grantCoins("u1", "GPA.1", 10);
    await store.grantCoins("u1", "GPA.2", 10);
    assert.equal(await store.revokeCoins("u1", "GPA.1", 10), true);
    assert.equal(await store.revokeCoins("u1", "GPA.1", 10), false);
    assert.equal((await userDoc("u1"))?.coins, 10);
    await db.doc("users/u1").set({ coins: 3 }, { merge: true });
    assert.equal(await store.revokeCoins("u1", "GPA.2", 10), true);
    assert.equal((await userDoc("u1"))?.coins, 0);
  });

  it("never credits a pack whose refund arrived first", async () => {
    assert.equal(await store.revokeCoins("u1", "GPA.1", 10), false);
    assert.equal(await store.grantCoins("u1", "GPA.1", 10), false);
    assert.equal((await userDoc("u1"))?.coins ?? 0, 0);
  });

  it("clearing premium does not create a document for a deleted account", async () => {
    await store.setPremiumUntil("gone", null);
    assert.equal(await userDoc("gone"), undefined);
  });

  it("merges coins and readings into the destination and removes the source", async () => {
    const reading = { question: "q", category: null, lines: [7, 7, 7, 7, 7, 7], coins: [[3, 2, 2]] as never, primaryId: 1, relatingId: null, aiText: "t", source: "ai" as const, model: "m", tokensIn: 0, tokensOut: 0, charge: null, interrupted: null, followUps: [], followUpCount: 0, outcome: null, outcomeNote: null, createdAt: 1 };
    await store.grantCoins("anon", "GPA.1", 10);
    await store.grantCoins("u1", "GPA.2", 10);
    const id = await store.createReading("anon", reading);
    await store.addReport({ uid: "anon", readingId: id, reason: "x", note: "", createdAt: 1 });

    await store.mergeUser("anon", "u1");
    await store.mergeUser("anon", "u1");

    assert.equal((await userDoc("u1"))?.coins, 20);
    assert.equal(await userDoc("anon"), undefined);
    assert.equal(await store.getReading("anon", id), null);
    assert.deepEqual((await store.getReading("u1", id))?.coins, [[3, 2, 2]]);
    assert.equal((await db.collection("reports").where("uid", "==", "u1").get()).size, 1);
  });

  it("never spends more coins than there are, under concurrent readings", async () => {
    await seed("u1", 3);
    const charges = await Promise.all(Array.from({ length: 10 }, () => spendOne("u1")));
    assert.equal(charges.filter((c) => c === "coin").length, 3);
    assert.equal(charges.filter((c) => c === "quota").length, 7);
    assert.equal((await userDoc("u1"))?.coins, 0);
  });

  it("loses neither a purchase nor a charge when they happen together", async () => {
    await seed("u1", 5);
    await Promise.all([spendOne("u1"), spendOne("u1"), store.grantCoins("u1", "GPA.1", 10), spendOne("u1")]);
    assert.equal((await userDoc("u1"))?.coins, 12);
  });

  it("gives back exactly one coin on a refund", async () => {
    await seed("u1", 1);
    await spendOne("u1");
    await store.quotaTx("u1", null, (user, device) => {
      const back = refundReading(user, device, "coin");
      return { user: back.user, device: back.device, result: null };
    });
    assert.equal((await userDoc("u1"))?.coins, 1);
  });

  it("charging a reading does not overwrite premium, and premium does not overwrite coins", async () => {
    const until = Date.now() + 86_400_000;
    await seed("u1", 4);
    await store.setPremiumUntil("u1", until);
    assert.equal((await userDoc("u1"))?.coins, 4);
    assert.equal(await spendOne("u1"), "premium");
    assert.equal((await userDoc("u1"))?.premiumUntil, until);
  });

  it("creating a user by charging does not clear a premium written at the same moment", async () => {
    const until = Date.now() + 86_400_000;
    await Promise.all([store.setPremiumUntil("u1", until), spendOne("u1")]);
    assert.equal((await userDoc("u1"))?.premiumUntil, until);
  });

  it("deletes the user's readings, reports and purchase markers, and nobody else's", async () => {
    await store.grantCoins("u1", "GPA.1", 10);
    await store.grantCoins("u2", "GPA.2", 10);
    const base = { question: "q", category: null, lines: [7, 7, 7, 7, 7, 7], coins: [[3, 2, 2]], primaryId: 1, relatingId: null, aiText: "t", source: "ai" as const, model: "m", tokensIn: 0, tokensOut: 0, charge: null, interrupted: null, followUps: [], followUpCount: 0, outcome: null, outcomeNote: null, createdAt: 1 };
    const mine = await store.createReading("u1", base);
    const theirs = await store.createReading("u2", base);
    await store.addReport({ uid: "u1", readingId: mine, reason: "x", note: "", createdAt: 1 });
    await store.addReport({ uid: "u2", readingId: theirs, reason: "x", note: "", createdAt: 1 });

    await store.deleteUser("u1");

    assert.equal(await userDoc("u1"), undefined);
    assert.equal(await store.getReading("u1", mine), null);
    assert.equal((await db.collection("reports").where("uid", "==", "u1").get()).size, 0);
    assert.equal((await db.collection("users/u1/coinGrants").get()).size, 0);
    assert.equal((await userDoc("u2"))?.coins, 10);
    assert.notEqual(await store.getReading("u2", theirs), null);
    assert.equal((await db.collection("reports").where("uid", "==", "u2").get()).size, 1);
  });

  describe("security rules", () => {
    const docUrl = (path: string) => `http://${host}/v1/projects/${projectId}/databases/(default)/documents/${path}`;
    const write = (path: string, fields: object) =>
      fetch(docUrl(path), { method: "PATCH", headers: { "content-type": "application/json" }, body: JSON.stringify({ fields }) });

    it("a client cannot read anyone's account", async () => {
      await seed("u1", 3);
      assert.equal((await fetch(docUrl("users/u1"))).status, 403);
    });

    it("a client cannot give itself coins or premium", async () => {
      await seed("u1", 3);
      assert.equal((await write("users/u1", { coins: { integerValue: "9999" } })).status, 403);
      assert.equal((await write("users/u1", { premiumUntil: { integerValue: "4102444800000" } })).status, 403);
      assert.equal((await userDoc("u1"))?.coins, 3);
    });

    it("a client cannot change the runtime config", async () => {
      assert.equal((await write("config/runtime", { freeDailyReadings: { integerValue: "9999" } })).status, 403);
    });
  });
});
