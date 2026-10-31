// Copies the scripture bundled with the app (frontend/) into backend/functions/data/ so the server uses exactly the same text.
import { copyFileSync, mkdirSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const source = resolve(here, "../../../frontend/core/data/src/commonMain/composeResources/files/hexagrams.json");
const target = resolve(here, "../data/hexagrams.json");
mkdirSync(dirname(target), { recursive: true });
copyFileSync(source, target);
console.log(`copied scripture → ${target}`);
