// Reads or merges fields into Firestore config/runtime of the live project.
//   node scripts/runtime-config.mjs                 → print the doc
//   node scripts/runtime-config.mjs '{"k": 1}'      → merge those fields, then print
// Uses Application Default Credentials (gcloud auth application-default login).
import { initializeApp, applicationDefault } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";

initializeApp({ credential: applicationDefault(), projectId: "iching-2001a" });
const ref = getFirestore().doc("config/runtime");
const patch = process.argv[2] ? JSON.parse(process.argv[2]) : null;
if (patch) {
  for (const [k, v] of Object.entries(patch)) {
    if (typeof v !== "number" && typeof v !== "boolean") throw new Error(`${k}: only numbers and booleans are written by this script`);
  }
  await ref.set(patch, { merge: true });
  console.log("merged:", JSON.stringify(patch));
}
const snap = await ref.get();
console.log(snap.exists ? JSON.stringify(snap.data(), null, 2) : "config/runtime does not exist");
