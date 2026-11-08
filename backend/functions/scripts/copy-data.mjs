// Copies the scripture bundled with the app (frontend/) into backend/functions/data/ so the server uses exactly the same text.
// Siam Si sets come from the app bundle and from Hosting (sets downloaded later); the server needs all of them for prompts.
import { copyFileSync, existsSync, mkdirSync, readdirSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const appFiles = resolve(here, "../../../frontend/core/data/src/commonMain/composeResources/files");
const data = resolve(here, "../data");

mkdirSync(data, { recursive: true });
copyFileSync(join(appFiles, "hexagrams.json"), join(data, "hexagrams.json"));
console.log(`copied scripture → ${join(data, "hexagrams.json")}`);

const siamsiTarget = join(data, "siamsi");
mkdirSync(siamsiTarget, { recursive: true });
for (const dir of [join(appFiles, "siamsi"), resolve(here, "../../hosting/public/content/siamsi")]) {
  if (!existsSync(dir)) continue;
  for (const file of readdirSync(dir).filter((f) => f.endsWith(".json"))) {
    copyFileSync(join(dir, file), join(siamsiTarget, file));
    console.log(`copied siamsi ${file} → ${siamsiTarget}`);
  }
}
