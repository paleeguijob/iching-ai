import { DocumentData, FieldValue, Firestore } from "firebase-admin/firestore";
import { QuotaTxResult, Store, StoredReading } from "./core/api";
import type { Coin } from "./core/cast";
import { bangkokDay, DeviceDoc, normalizeDevice, normalizeUser, UserDoc } from "./core/quota";

/**
 * Firestore layout
 *   users/{uid}                      premiumUntil, coins, usage{day,...}, lastReadingAt
 *   users/{uid}/readings/{id}        one divination (the spec's `readings` table)
 *   users/{uid}/coinGrants/{txId}    idempotency marker for consumable purchases
 *   devices/{sha256(deviceId)}       per-device daily free usage (anti account-farming)
 *   reports/{autoId}                 "report this answer" submissions
 *   config/runtime                   kill-switch and limits (see core/quota.ts RuntimeConfig)
 * Clients never touch Firestore directly (firestore.rules denies everything).
 */
export function firestoreStore(db: Firestore): Store {
  const users = db.collection("users");
  const readings = (uid: string) => users.doc(uid).collection("readings");
  const safeId = (id: string) => id.replace(/[/.#$\[\]]/g, "_").slice(0, 200) || "unknown";

  const toReading = (id: string, d: DocumentData): StoredReading => ({
    id,
    question: d.question ?? "",
    category: d.category ?? null,
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

    async listReadings(uid, limit) {
      const snap = await readings(uid).orderBy("createdAt", "desc").limit(limit).get();
      return snap.docs.map((d) => toReading(d.id, d.data()));
    },

    async addReport(report) {
      await db.collection("reports").add(report);
      await readings(report.uid).doc(report.readingId).set({ reported: true }, { merge: true });
    },

    async setPremiumUntil(uid, premiumUntil) {
      await users.doc(uid).set({ premiumUntil, updatedAt: Date.now() }, { merge: true });
    },

    async grantCoins(uid, transactionId, coins) {
      const grantRef = users.doc(uid).collection("coinGrants").doc(safeId(transactionId));
      return db.runTransaction(async (tx) => {
        if ((await tx.get(grantRef)).exists) return false;
        tx.set(grantRef, { coins, at: Date.now() });
        tx.set(users.doc(uid), { coins: FieldValue.increment(coins) }, { merge: true });
        return true;
      });
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
