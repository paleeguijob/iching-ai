/**
 * Safety pre-check run before any casting or model call. Crisis questions get a fixed, caring
 * reply with Thai hotlines instead of a reading, and never use up quota.
 */
export type SafetyVerdict =
  | { kind: "ok"; sensitive: boolean }
  | { kind: "crisis"; topic: "self_harm" | "medical_emergency"; message: string };

const SELF_HARM = [
  "ฆ่าตัวตาย", "อยากตาย", "อยากหายไปจากโลก", "ไม่อยากมีชีวิต", "ไม่อยากอยู่แล้ว", "ไม่อยากตื่น",
  "จบชีวิต", "ปลิดชีพ", "ทำร้ายตัวเอง", "ทำร้ายตนเอง", "กรีดข้อมือ", "กรีดแขน", "กินยาตาย",
  "กินยาฆ่าตัว", "ผูกคอ", "โดดตึก", "กระโดดตึก", "โดดสะพาน", "ตายไปซะ", "อยู่ไปก็ไร้ค่า",
  "suicide", "killmyself", "endmylife", "selfharm", "wanttodie", "cutmyself",
];

const MEDICAL_EMERGENCY = [
  "เจ็บหน้าอก", "แน่นหน้าอก", "หายใจไม่ออก", "หมดสติ", "ชักเกร็ง", "เลือดออกไม่หยุด",
  "กินยาเกินขนาด", "ยาเกินขนาด", "แพ้ยารุนแรง", "ปากเบี้ยว", "แขนขาอ่อนแรง", "โดนงูกัด",
  "overdose", "heartattack", "stroke", "cantbreathe",
];

/** Topics that still get a reading, with the prompt reminding the hermit to point to experts. */
const SENSITIVE = [
  "หมอ", "โรค", "ป่วย", "มะเร็ง", "ผ่าตัด", "กินยา", "ตั้งครรภ์", "ซึมเศร้า", "ฟ้อง", "คดี", "ทนาย",
  "ศาล", "หุ้น", "คริปโต", "ลงทุน", "หนี้", "กู้", "ล้มละลาย", "พนัน", "หวย",
];

export const SELF_HARM_MESSAGE = `ผู้แสวงหาเอ๋ย ถ้อยคำของท่านบอกผู้เฒ่าว่าใจของท่านกำลังแบกสิ่งที่หนักเหลือเกิน ในยามนี้คัมภีร์ขอวางไว้ก่อน เพราะสิ่งสำคัญที่สุดคือตัวท่านเอง

ท่านไม่จำเป็นต้องผ่านคืนนี้ตามลำพัง โปรดพูดคุยกับใครสักคนที่ไว้ใจได้ หรือโทรหาสายด่วนสุขภาพจิต 1323 (ตลอด 24 ชั่วโมง ไม่เสียค่าใช้จ่าย) หากรู้สึกว่าตนเองอาจไม่ปลอดภัยในตอนนี้ โปรดโทร 1669 หรือไปโรงพยาบาลที่ใกล้ที่สุดทันที

ดุจสายน้ำที่ไหลผ่านช่วงแคบก่อนจะถึงที่ราบกว้าง ความรู้สึกในยามนี้ไม่ใช่ทั้งหมดของชีวิตท่าน`;

export const MEDICAL_MESSAGE = `ผู้แสวงหาเอ๋ย สิ่งที่ท่านเล่ามาอาจเป็นเรื่องเร่งด่วนของร่างกาย ซึ่งไม่ควรรอคำตอบจากคัมภีร์

โปรดโทรแจ้งเหตุฉุกเฉินทางการแพทย์ 1669 หรือไปโรงพยาบาลที่ใกล้ที่สุดโดยเร็ว เมื่อกายปลอดภัยแล้ว ค่อยกลับมาพินิจคัมภีร์ด้วยใจที่สงบ`;

/** Lower-case and drop spaces / zero-width marks so "อยาก ตาย" and "Kill  myself" still match. */
export function normalizeForSafety(text: string): string {
  return text.toLowerCase().replace(/[\s​‌‍⁠'’.,!?-]/g, "");
}

export function checkSafety(text: string): SafetyVerdict {
  const t = normalizeForSafety(text);
  if (SELF_HARM.some((w) => t.includes(w))) return { kind: "crisis", topic: "self_harm", message: SELF_HARM_MESSAGE };
  if (MEDICAL_EMERGENCY.some((w) => t.includes(w))) {
    return { kind: "crisis", topic: "medical_emergency", message: MEDICAL_MESSAGE };
  }
  return { kind: "ok", sensitive: SENSITIVE.some((w) => t.includes(w)) };
}
