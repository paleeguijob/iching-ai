import { randomInt } from "node:crypto";
import { Hexagram, Scripture } from "./hexagrams";

/** Coin face values in the traditional three-coin method: heads (yang side) = 3, tails = 2. */
export type Coin = 2 | 3;
/** 6 = old yin (changing), 7 = young yang, 8 = young yin, 9 = old yang (changing). */
export type LineValue = 6 | 7 | 8 | 9;

export interface Cast {
  /** Three coins per toss, six tosses, bottom line first. Sent to the app so it can animate them. */
  coins: Coin[][];
  lines: LineValue[];
  primary: Hexagram;
  relating: Hexagram | null;
}

export const isYang = (v: LineValue) => v === 7 || v === 9;
export const isChanging = (v: LineValue) => v === 6 || v === 9;
export const changingPositions = (lines: LineValue[]) =>
  lines.flatMap((v, i) => (isChanging(v) ? [i + 1] : []));

/** A fair coin from the OS CSPRNG, so the app cannot influence the result. */
export const cryptoCoin = (): Coin => (randomInt(2) === 0 ? 2 : 3);

export function castHexagram(scripture: Scripture, coin: () => Coin = cryptoCoin): Cast {
  const coins: Coin[][] = [];
  for (let i = 0; i < 6; i++) coins.push([coin(), coin(), coin()]);
  const lines = coins.map((t) => (t[0] + t[1] + t[2]) as LineValue);
  return resolveLines(scripture, lines, coins);
}

export function resolveLines(scripture: Scripture, lines: LineValue[], coins: Coin[][] = []): Cast {
  if (lines.length !== 6) throw new Error("A hexagram needs six lines");
  const primaryPattern = lines.map((v) => (isYang(v) ? "1" : "0")).join("");
  const relatingPattern = lines.map((v) => ((isChanging(v) ? !isYang(v) : isYang(v)) ? "1" : "0")).join("");
  return {
    coins,
    lines,
    primary: scripture.fromPattern(primaryPattern),
    relating: relatingPattern === primaryPattern ? null : scripture.fromPattern(relatingPattern),
  };
}
