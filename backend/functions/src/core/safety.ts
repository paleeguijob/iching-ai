import type { Lang } from "./lang";

/**
 * Safety pre-check run before any casting or model call. Crisis questions get a fixed, caring
 * reply with Thai hotlines (in English: local emergency services and findahelpline.com) instead
 * of a reading, and never use up quota. Both languages' phrases are always checked.
 */
export type SafetyVerdict =
  | { kind: "ok"; sensitive: boolean }
  | { kind: "crisis"; topic: "self_harm" | "medical_emergency"; message: string };

export const SELF_HARM = [
  "ฆ่าตัวตาย", "อยากตาย", "อยากหายไปจากโลก", "ไม่อยากมีชีวิต", "ไม่อยากอยู่แล้ว", "ไม่อยากตื่น",
  "จบชีวิต", "ปลิดชีพ", "ทำร้ายตัวเอง", "ทำร้ายตนเอง", "กรีดข้อมือ", "กรีดแขน", "กินยาตาย",
  "กินยาฆ่าตัว", "ผูกคอ", "โดดตึก", "กระโดดตึก", "โดดสะพาน", "ตายไปซะ", "อยู่ไปก็ไร้ค่า",
  "suicide", "killmyself", "endmylife", "selfharm", "wanttodie", "cutmyself",
];

export const MEDICAL_EMERGENCY = [
  "เจ็บหน้าอก", "แน่นหน้าอก", "หายใจไม่ออก", "หมดสติ", "ชักเกร็ง", "เลือดออกไม่หยุด",
  "กินยาเกินขนาด", "ยาเกินขนาด", "แพ้ยารุนแรง", "ปากเบี้ยว", "แขนขาอ่อนแรง", "โดนงูกัด",
  "overdose", "heartattack", "stroke", "cantbreathe",
];

/**
 * English phrases, matched after normalizeForSafety (lower case, no spaces or apostrophes: "can't breathe" → "cantbreathe").
 * Kept apart from the Thai lists above, which must match the app's copy.
 */
export const SELF_HARM_EN = [
  "killmyself", "killingmyself", "suicide", "suicidal", "endmylife", "endingmylife", "enditall", "takemyownlife",
  "wanttodie", "wannadie", "wishiwasdead", "wishiweredead", "betteroffdead", "dontwanttolive", "dontwanttobealive",
  "selfharm", "hurtmyself", "harmmyself", "cutmyself", "cuttingmyself",
];

export const MEDICAL_EMERGENCY_EN = [
  "cantbreathe", "cannotbreathe", "notbreathing", "chestpain", "heartattack", "stroke", "unconscious",
  "severebleeding", "bleedingheavily", "wontstopbleeding", "seizure", "overdose", "anaphylaxis", "anaphylactic",
  "snakebite",
];

/** Topics that still get a reading, with the prompt reminding the hermit to point to experts. */
const SENSITIVE = [
  "หมอ", "โรค", "ป่วย", "มะเร็ง", "ผ่าตัด", "กินยา", "ตั้งครรภ์", "ซึมเศร้า", "ฟ้อง", "คดี", "ทนาย",
  "ศาล", "หุ้น", "คริปโต", "ลงทุน", "หนี้", "กู้", "ล้มละลาย", "พนัน", "หวย",
  "doctor", "illness", "disease", "cancer", "surgery", "medication", "pregnan", "depress", "lawsuit", "lawyer",
  "stocks", "crypto", "invest", "debt", "loan", "bankrupt", "gambl", "lottery",
];

export const SELF_HARM_MESSAGE = `ผู้แสวงหาเอ๋ย ถ้อยคำของท่านบอกผู้เฒ่าว่าใจของท่านกำลังแบกสิ่งที่หนักเหลือเกิน ในยามนี้คัมภีร์ขอวางไว้ก่อน เพราะสิ่งสำคัญที่สุดคือตัวท่านเอง

ท่านไม่จำเป็นต้องผ่านคืนนี้ตามลำพัง โปรดพูดคุยกับใครสักคนที่ไว้ใจได้ หรือโทรหาสายด่วนสุขภาพจิต 1323 (ตลอด 24 ชั่วโมง ไม่เสียค่าใช้จ่าย) หากรู้สึกว่าตนเองอาจไม่ปลอดภัยในตอนนี้ โปรดโทร 1669 หรือไปโรงพยาบาลที่ใกล้ที่สุดทันที

ดุจสายน้ำที่ไหลผ่านช่วงแคบก่อนจะถึงที่ราบกว้าง ความรู้สึกในยามนี้ไม่ใช่ทั้งหมดของชีวิตท่าน`;

export const MEDICAL_MESSAGE = `ผู้แสวงหาเอ๋ย สิ่งที่ท่านเล่ามาอาจเป็นเรื่องเร่งด่วนของร่างกาย ซึ่งไม่ควรรอคำตอบจากคัมภีร์

โปรดโทรแจ้งเหตุฉุกเฉินทางการแพทย์ 1669 หรือไปโรงพยาบาลที่ใกล้ที่สุดโดยเร็ว เมื่อกายปลอดภัยแล้ว ค่อยกลับมาพินิจคัมภีร์ด้วยใจที่สงบ`;

export const SELF_HARM_MESSAGE_EN = `Seeker, your words tell me your heart is carrying something very heavy. For now, let us set the scripture aside, because what matters most is you.

You do not have to get through tonight alone. Please talk to someone you trust, or reach out to a helpline; findahelpline.com shows free, confidential helplines in your country. If you feel you may not be safe right now, please call your local emergency number or go to the nearest hospital straight away.

Like a river that passes through a narrow gorge before it reaches the open plain, what you feel now is not the whole of your life.`;

export const MEDICAL_MESSAGE_EN = `Seeker, what you describe may be urgent for your body, and it should not wait for an answer from the scripture.

Please call your local emergency number or go to the nearest hospital now. If you are unsure whom to call, findahelpline.com can help you find a local helpline. Once your body is safe, come back and reflect on the scripture with a calm mind.`;

/** Lower-case and drop spaces / zero-width marks so "อยาก ตาย" and "Kill  myself" still match. */
export function normalizeForSafety(text: string): string {
  return text.toLowerCase().replace(/[\s​‌‍⁠'’.,!?-]/g, "");
}

const SELF_HARM_ALL = [...SELF_HARM, ...SELF_HARM_EN];
const MEDICAL_EMERGENCY_ALL = [...MEDICAL_EMERGENCY, ...MEDICAL_EMERGENCY_EN];

/** Checks the Thai and the English phrases whatever [lang] is; [lang] only picks the reply's language. */
export function checkSafety(text: string, lang: Lang = "th"): SafetyVerdict {
  const t = normalizeForSafety(text);
  const en = lang === "en";
  if (SELF_HARM_ALL.some((w) => t.includes(w))) {
    return { kind: "crisis", topic: "self_harm", message: en ? SELF_HARM_MESSAGE_EN : SELF_HARM_MESSAGE };
  }
  if (MEDICAL_EMERGENCY_ALL.some((w) => t.includes(w))) {
    return { kind: "crisis", topic: "medical_emergency", message: en ? MEDICAL_MESSAGE_EN : MEDICAL_MESSAGE };
  }
  return { kind: "ok", sensitive: SENSITIVE.some((w) => t.includes(w)) };
}
