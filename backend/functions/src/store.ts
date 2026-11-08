import { DocumentData, FieldValue, Firestore } from "firebase-admin/firestore";
import { QuotaTxResult, Store, StoredReading, YearHexagram } from "./core/api";
import type { Coin } from "./core/cast";
import { bangkokDay, DeviceDoc, normalizeDevice, normalizeUser, UserDoc } from "./core/quota";
import { mergeStreaks, normalizeStreak, THEMES } from "./core/streak";

/**
 * Firestore layout
 *   users/{uid}                      premiumUntil, coins, usage{day,...}, lastReadingAt
 *   users/{uid}/readings/{id}        one divination (the spec's `readings` table)
 *   users/{uid}/meta/streak          daily-hexagram streak and unlocked themes (core/streak.ts)
 *   users/{uid}/meta/year-{year}     the one year hexagram the account cast in a campaign (lines, coins as {c}, castAt)
 *   coinGrants/{txId}                coins, at, revokedAt: one per store transaction, across accounts and
 *                                    kept after account deletion (no uid), so a pack is never credited twice
 *   users/{uid}/coinGrants/{txId}    the same marker as written before it moved; still honoured
 *   devices/{sha256(deviceId)}       per-device daily free usage (anti account-farming)
 *   reports/{autoId}                 "report this answer" submissions
 *   config/runtime                   kill-switch and limits (see core/quota.ts RuntimeConfig)
 * Clients never touch Firestore directly (firestore.rules denies everything).
 */
export function firestoreStore(db: Firestore): Store {
  const users = db.collection("users");
  const readings = (uid: string) => users.doc(uid).collection("readings");
  const streakRef = (uid: string) => users.doc(uid).collection("meta").doc("streak");
  const yearRef = (uid: string, year: number) => users.doc(uid).collection("meta").doc(`year-${year}`);
  const toYear = (year: number, d: DocumentData): YearHexagram => ({
    year,
    primaryId: d.primaryId,
    lines: d.lines ?? [],
    coins: Array.isArray(d.coins) ? d.coins.map((t: { c: number[] }) => t.c as Coin[]) : [],
    castAt: d.castAt ?? 0,
  });
  const safeId = (id: string) => id.replace(/[/.#$\[\]]/g, "_").slice(0, 200) || "unknown";
  const grantRefs = (uid: string, transactionId: string) => ({
    grantRef: db.collection("coinGrants").doc(safeId(transactionId)),
    legacyRef: users.doc(uid).collection("coinGrants").doc(safeId(transactionId)),
  });

  const toReading = (id: string, d: DocumentData): StoredReading => ({
    id,
    question: d.question ?? "",
    category: d.category ?? null,
    yearOf: d.yearOf ?? null,
    lines: d.lines ?? [],
    coins: Array.isArray(d.coins) ? d.coins.map((t: { c: number[] } | number[]) => (Array.isArray(t) ? t : t.c) as Coin[]) : [],
    primaryId: d.primaryId,
    relatingId: d.relatingId ?? null,
    aiText: d.aiText ?? null,
    source: d.source ?? "scripture",
    model: d.model ?? null,
    tokensIn: d.tokensIn ?? 0,
    tokensOut: d.tokensOut ?? 0,
    charge: d.charge ?? null,
    interrupted: d.interrupted ?? null,
    followUps: d.followUps ?? [],
    followUpCount: d.followUpCount ?? 0,
    outcome: d.outcome ?? null,
    outcomeNote: d.outcomeNote ?? null,
    createdAt: d.createdAt ?? 0,
    siamsi: d.siamsi ?? null,
  });

  // Firestore cannot store nested arrays, so each toss is wrapped as {c: [3, 2, 3]}.
  const encode = (r: Partial<Omit<StoredReading, "id">>) => {
    const { coins, ...rest } = r;
    return coins ? { ...rest, coins: coins.map((c) => ({ c })) } : rest;
  };

  return {
    async quotaTx<T>(uid: string, deviceHash: string | null, fn: (u: UserDoc, d: DeviceDoc | null) => QuotaTxResult<T>): Promise<T> {
      return db.runTransaction(async (tx) => {
        const now = Date.now();
        const day = bangkokDay(now);
        const userRef = users.doc(uid);
        const deviceRef = deviceHash ? db.collection("devices").doc(deviceHash) : null;
        const userSnap = await tx.get(userRef);
        const deviceSnap = deviceRef ? await tx.get(deviceRef) : null;
        const user = normalizeUser(userSnap.data() as Partial<UserDoc> | undefined, day);
        const device = deviceRef ? normalizeDevice(deviceSnap?.data() as Partial<DeviceDoc> | undefined, day) : null;
        const out = fn(user, device);
        if (out.user) {
          tx.set(
            userRef,
            {
              coins: out.user.coins,
              usage: out.user.usage,
              lastReadingAt: out.user.lastReadingAt,
              updatedAt: now,
              ...(userSnap.exists ? {} : { createdAt: now, premiumUntil: null }),
            },
            { merge: true },
          );
        }
        if (out.device && deviceRef) tx.set(deviceRef, { usage: out.device.usage, updatedAt: now }, { merge: true });
        return out.result;
      });
    },

    async createReading(uid, reading) {
      const ref = readings(uid).doc();
      await ref.set(encode(reading));
      return ref.id;
    },

    async getReading(uid, id) {
      const snap = await readings(uid).doc(id).get();
      return snap.exists ? toReading(snap.id, snap.data()!) : null;
    },

    async updateReading(uid, id, patch) {
      await readings(uid).doc(id).set(encode(patch), { merge: true });
    },

    async deleteReading(uid, id) {
      await readings(uid).doc(id).delete();
    },

    async reserveFollowUp(uid, id, limit) {
      const ref = readings(uid).doc(id);
      return db.runTransaction(async (tx) => {
        const snap = await tx.get(ref);
        const count = (snap.data()?.followUpCount as number | undefined) ?? 0;
        if (!snap.exists || count >= limit) return false;
        tx.update(ref, { followUpCount: count + 1 });
        return true;
      });
    },

    async releaseFollowUp(uid, id) {
      await readings(uid).doc(id).update({ followUpCount: FieldValue.increment(-1) });
    },

    async appendFollowUp(uid, id, turn) {
      await readings(uid).doc(id).update({ followUps: FieldValue.arrayUnion(turn) });
    },

    async listReadings(uid, limit, since) {
      const query = since == null ? readings(uid) : readings(uid).where("createdAt", ">=", since);
      const snap = await query.orderBy("createdAt", "desc").limit(limit).get();
      return snap.docs.map((d) => toReading(d.id, d.data()));
    },

    async addReport(report) {
      await db.collection("reports").add(report);
      await readings(report.uid).doc(report.readingId).set({ reported: true }, { merge: true });
    },

    async setPremiumUntil(uid, premiumUntil) {
      const ref = users.doc(uid);
      await db.runTransaction(async (tx) => {
        // "No premium" must not create a document: a late webhook would bring a deleted account back.
        if (premiumUntil === null && !(await tx.get(ref)).exists) return;
        tx.set(ref, { premiumUntil, updatedAt: Date.now() }, { merge: true });
      });
    },

    async grantCoins(uid, transactionId, coins) {
      const { grantRef, legacyRef } = grantRefs(uid, transactionId);
      return db.runTransaction(async (tx) => {
        const grant = await tx.get(grantRef);
        const legacy = await tx.get(legacyRef);
        if (grant.exists || legacy.exists) return false;
        tx.set(grantRef, { coins, at: Date.now() });
        tx.set(users.doc(uid), { coins: FieldValue.increment(coins) }, { merge: true });
        return true;
      });
    },

    async revokeCoins(uid, transactionId, coins) {
      const { grantRef, legacyRef } = grantRefs(uid, transactionId);
      return db.runTransaction(async (tx) => {
        const grant = await tx.get(grantRef);
        const legacy = await tx.get(legacyRef);
        const user = await tx.get(users.doc(uid));
        if (grant.data()?.revokedAt) return false;
        tx.set(grantRef, { coins, revokedAt: Date.now() }, { merge: true });
        if (!(grant.exists || legacy.exists) || !user.exists) return false;
        tx.set(users.doc(uid), { coins: Math.max(0, Math.floor(user.data()?.coins ?? 0) - coins) }, { merge: true });
        return true;
      });
    },

    async updateStreak(uid, fn) {
      return db.runTransaction(async (tx) => {
        const next = fn(normalizeStreak((await tx.get(streakRef(uid))).data()));
        tx.set(streakRef(uid), next);
        return next;
      });
    },

    async getYearHexagram(uid, year) {
      const snap = await yearRef(uid, year).get();
      return snap.exists ? toYear(year, snap.data()!) : null;
    },

    async claimYearHexagram(uid, candidate) {
      return db.runTransaction(async (tx) => {
        const ref = yearRef(uid, candidate.year);
        const existing = await tx.get(ref);
        const streak = normalizeStreak((await tx.get(streakRef(uid))).data());
        const kept = existing.exists ? toYear(candidate.year, existing.data()!) : candidate;
        if (!existing.exists) tx.set(ref, { primaryId: kept.primaryId, lines: kept.lines, coins: kept.coins.map((c) => ({ c })), castAt: kept.castAt });
        if (!streak.unlocked.includes("cinnabar_gold")) {
          tx.set(streakRef(uid), { ...streak, unlocked: THEMES.filter((t) => t === "cinnabar_gold" || streak.unlocked.includes(t)) });
        }
        return kept;
      });
    },

    async mergeUser(fromUid, toUid) {
      const left = normalizeStreak((await streakRef(fromUid).get()).data());
      await this.updateStreak(toUid, (stored) => mergeStreaks(stored, left));
      await db.runTransaction(async (tx) => {
        const from = await tx.get(users.doc(fromUid));
        const coins = Math.max(0, Math.floor(from.data()?.coins ?? 0));
        if (coins === 0) return;
        tx.set(users.doc(toUid), { coins: FieldValue.increment(coins) }, { merge: true });
        tx.set(users.doc(fromUid), { coins: 0 }, { merge: true });
      });
      const writer = db.bulkWriter();
      const toMeta = users.doc(toUid).collection("meta");
      for (const doc of (await users.doc(fromUid).collection("meta").get()).docs) {
        if (doc.id.startsWith("year-") && !(await toMeta.doc(doc.id).get()).exists) writer.set(toMeta.doc(doc.id), doc.data());
      }
      for (const doc of (await readings(fromUid).get()).docs) writer.set(readings(toUid).doc(doc.id), doc.data());
      for (const doc of (await db.collection("reports").where("uid", "==", fromUid).get()).docs) writer.update(doc.ref, { uid: toUid });
      await writer.close();
      await this.deleteUser(fromUid);
    },

    async deleteUser(uid) {
      await db.recursiveDelete(users.doc(uid));
      const reports = await db.collection("reports").where("uid", "==", uid).get();
      const batch = db.batch();
      reports.docs.forEach((d) => batch.delete(d.ref));
      await batch.commit();
    },
  };
}
