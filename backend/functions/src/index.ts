import { initializeApp } from "firebase-admin/app";
import { getAppCheck } from "firebase-admin/app-check";
import { getAuth } from "firebase-admin/auth";
import { getFirestore } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import * as logger from "firebase-functions/logger";
import { defineSecret } from "firebase-functions/params";
import { onRequest } from "firebase-functions/v2/https";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { createApi, createRevenueCatWebhook, Deps, HttpReq, HttpRes, revenueCatClient } from "./core/api";
import { buildDailyPush } from "./core/daily";
import { streamGemini } from "./core/gemini";
import { loadScripture } from "./core/hexagrams";
import { mergeConfig, RuntimeConfig } from "./core/quota";
import { firestoreStore } from "./store";

initializeApp();
const db = getFirestore();

const GEMINI_API_KEY = defineSecret("GEMINI_API_KEY");
/** RevenueCat secret API key (v1, "sk_..."). Set to "unset" until RevenueCat is ready. */
const REVENUECAT_API_KEY = defineSecret("REVENUECAT_API_KEY");
/** Any long random string; paste the same value as the webhook's Authorization header in RevenueCat. */
const REVENUECAT_WEBHOOK_AUTH = defineSecret("REVENUECAT_WEBHOOK_AUTH");

const REGION = "asia-southeast1";
const configured = (v: string) => (v && v !== "unset" ? v : "");

let configCache: { at: number; value: RuntimeConfig } | null = null;
async function runtimeConfig(): Promise<RuntimeConfig> {
  if (configCache && Date.now() - configCache.at < 60_000) return configCache.value;
  const snap = await db.doc("config/runtime").get();
  configCache = { at: Date.now(), value: mergeConfig(snap.data()) };
  return configCache.value;
}

function deps(): Deps {
  const rcKey = configured(REVENUECAT_API_KEY.value());
  const rc = rcKey ? revenueCatClient(() => rcKey, Date.now) : null;
  return {
    verifyIdToken: (token) => getAuth().verifyIdToken(token),
    verifyAppCheck: async (token) => {
      await getAppCheck().verifyToken(token);
    },
    store: firestoreStore(db),
    scripture: loadScripture(),
    config: runtimeConfig,
    // 2048 rather than the spec's 1024: thinking tokens count against this cap and a long Thai
    // reading was getting cut off. Billing is by tokens actually produced, so the cap costs nothing.
    streamModel: (prompt, model, signal) =>
      streamGemini(prompt, { apiKey: GEMINI_API_KEY.value(), model, signal, maxOutputTokens: 2048, temperature: 0.7 }),
    fetchEntitlements: rc ? rc.fetchEntitlements : null,
    deletePurchaser: rc ? rc.deleteSubscriber : null,
    deleteAuthUser: (uid) => getAuth().deleteUser(uid),
    now: Date.now,
    log: logger,
    skipAppCheck: process.env.FUNCTIONS_EMULATOR === "true",
  };
}

/** The app's API: /divine, /follow-up, /status, /ad-reward, /report, /readings, /purchases/sync, /account. */
export const api = onRequest(
  {
    region: REGION,
    secrets: [GEMINI_API_KEY, REVENUECAT_API_KEY],
    timeoutSeconds: 120,
    memory: "256MiB",
    concurrency: 40,
    // Hard ceiling on spend if something goes wrong; raise when traffic grows.
    maxInstances: 10,
    invoker: "public",
  },
  async (req, res) => {
    await createApi(deps())(req as unknown as HttpReq, res as unknown as HttpRes);
  },
);

/** Sends the daily hexagram to everyone who opted in. No retries: a repeat is worse than a miss. */
export const dailyPush = onSchedule(
  { schedule: "0 7 * * *", timeZone: "Asia/Bangkok", region: REGION, retryCount: 0, maxInstances: 1 },
  async () => {
    try {
      if (!(await runtimeConfig()).dailyPushEnabled) {
        logger.info("daily_push_skipped");
        return;
      }
      const push = buildDailyPush(Date.now(), loadScripture());
      await getMessaging().send({
        topic: push.topic,
        notification: { title: push.title, body: push.body },
        data: { type: "daily", hexagramId: String(push.hexagramId) },
        android: { ttl: push.ttlMs, priority: "high", notification: { channelId: "daily" } },
      });
      logger.info("daily_push_sent", { hexagramId: push.hexagramId });
    } catch (e) {
      logger.error("daily_push_failed", { error: String(e) });
    }
  },
);

/** RevenueCat → Integrations → Webhooks. URL: https://<region>-<project>.cloudfunctions.net/revenuecatWebhook */
export const revenuecatWebhook = onRequest(
  { region: REGION, secrets: [REVENUECAT_API_KEY, REVENUECAT_WEBHOOK_AUTH], maxInstances: 5, invoker: "public" },
  async (req, res) => {
    const d = deps();
    await createRevenueCatWebhook(d, () => configured(REVENUECAT_WEBHOOK_AUTH.value()))(
      req as unknown as HttpReq,
      res as unknown as HttpRes,
    );
  },
);
