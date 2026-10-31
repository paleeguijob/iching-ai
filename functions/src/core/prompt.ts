import { Cast, changingPositions } from "./cast";
import { Hexagram, lowerTrigram, upperTrigram } from "./hexagrams";

export interface Prompt {
  systemInstruction: string;
  userMessage: string;
}

export const MAX_QUESTION_CHARS = 300;
export const MAX_FOLLOW_UP_CHARS = 300;

export const CATEGORIES = ["work", "love", "money", "mind", "health", "decision", "other"] as const;
export type Category = (typeof CATEGORIES)[number];

const CATEGORY_TH: Record<Category, string> = {
  work: "การงาน",
  love: "ความรัก",
  money: "การเงิน",
  mind: "สุขภาพใจ",
  health: "สุขภาพ",
  decision: "การตัดสินใจ",
  other: "ทั่วไป",
};

const LINE_NAMES = ["หนึ่ง", "สอง", "สาม", "สี่", "ห้า", "หก"];
export const linePositionTh = (position: number) => `เส้นที่ ${LINE_NAMES[position - 1]}`;

/**
 * The hermit persona. Rule 3 changed from spec v1.0: the reading never talks about technology,
 * but if the seeker asks directly whether this is an AI the answer must be honest (store policy).
 */
export const PERSONA = `
คุณคือนักพรตเต๋าผู้เร้นกายบนยอดเขาอู่ตัง มีชีวิตอยู่ด้วยความสมถะและเข้าใจสัจธรรมของการเปลี่ยนแปลง (เต๋า) หน้าที่ของคุณคือการแปลผล 'คัมภีร์อี้จิง' ที่จับยามได้ ให้เข้ากับคำถามของผู้ใช้

กฎการตอบ (Guidelines):
1. ใช้สรรพนามแทนตัวเองว่า 'อาตมา' หรือ 'ผู้เฒ่า' และเรียกผู้ใช้ว่า 'ท่าน' หรือ 'ผู้แสวงหา'
2. น้ำเสียงต้องสงบ เมตตา ลุ่มลึก แฝงปรัชญาธรรมชาติ (น้ำ, ลม, ภูเขา) และไม่ตัดสินถูกผิด
3. ไม่พูดถึงเทคโนโลยีในเนื้อคำทำนาย แต่หากผู้แสวงหาถามตรง ๆ ว่าท่านเป็น AI หรือไม่ ต้องตอบตามจริงอย่างสุภาพว่าเป็นผู้ช่วยที่สร้างโดย AI ซึ่งสวมบทบาทนักพรต ห้ามปฏิเสธ
4. อ้างอิงความหมายดั้งเดิมของกวาที่ระบบส่งให้ (Context Data) เป็นแก่นหลักในการตอบเสมอ ห้ามแต่งความหมายของกวาเอง
5. โครงสร้าง: เริ่มด้วยคำเปรียบเปรยถึงธรรมชาติ -> แปลความหมายกวาให้เข้ากับคำถาม -> สรุปคำแนะนำให้ก้าวต่อไปด้วยความว่างและสมดุล
6. ห้ามทำนายความตาย โรคร้าย หรือเหตุร้ายแรงแบบฟันธง ห้ามสั่งให้ผู้แสวงหาลงทุน ซื้อขาย หยุดยา หรือตัดสินใจทางกฎหมาย ให้ชี้แนะในเชิงสะท้อนคิดเท่านั้น
7. หากคำถามเกี่ยวกับสุขภาพ กฎหมาย หรือการเงินที่ร้ายแรง ให้ชี้แนะด้วยเมตตาว่าควรปรึกษาผู้เชี่ยวชาญในทางโลกควบคู่กันไป

รูปแบบการเขียน:
- ตอบเป็นภาษาไทยล้วน เป็นร้อยแก้ว 3-5 ย่อหน้า ความยาวรวมประมาณ 180-320 คำ
- ไม่ใช้หัวข้อ สัญลักษณ์ markdown ตัวหนา หรือรายการแบบบูลเล็ต
- เอ่ยชื่อกวาเป็นภาษาไทยอย่างน้อยหนึ่งครั้ง และหากมีเส้นแปร ให้เชื่อมความหมายของเส้นแปรและกวาแปรเข้ากับคำถามด้วย
`.trim();

export const FOLLOW_UP_RULES = `
ขณะนี้ผู้แสวงหาถามต่อเนื่องจากคำทำนายเดิม
- ตอบในบทบาทเดิม ยึดกวาและเส้นแปรเดิมเป็นแก่น ห้ามเสี่ยงทายใหม่หรือเปลี่ยนกวา
- ตอบสั้นกระชับ 1-2 ย่อหน้า ไม่เกินประมาณ 150 คำ ไม่ใช้ markdown
- หากคำถามออกนอกเรื่องของคำทำนายนี้ ให้ชวนกลับมาที่กวาเดิมอย่างนุ่มนวล
`.trim();

function hexagramBlock(label: string, h: Hexagram, includeDescription = true): string {
  const rows = [
    `${label}: กวาที่ ${h.id} ${h.nameCn} (${h.pinyin}) — ${h.nameTh}`,
    `ตรีแกรม: บน ${upperTrigram(h).natureTh} / ล่าง ${lowerTrigram(h).natureTh}`,
    `คำตัดสิน: ${h.judgment}`,
    `ภาพลักษณ์: ${h.image}`,
  ];
  if (includeDescription) rows.push(`ภาพรวม: ${h.description}`);
  return rows.join("\n");
}

/** Scripture context: primary hexagram, only the changing lines that were cast, and the relating hexagram. */
export function scriptureContext(cast: Pick<Cast, "lines" | "primary" | "relating">): string {
  const parts = ["[Context Data — คัมภีร์อี้จิงที่จับยามได้]", hexagramBlock("กวาหลัก", cast.primary)];
  const changing = changingPositions(cast.lines);
  if (changing.length === 0) {
    parts.push("เส้นแปร: ไม่มี — ให้ยึดคำตัดสินและภาพลักษณ์ของกวาหลักเป็นแก่น");
  } else {
    parts.push(`เส้นแปร (${changing.length} เส้น) — ความหมายเฉพาะที่ต้องเน้น:`);
    for (const p of changing) parts.push(`- ${linePositionTh(p)}: ${cast.primary.lines[p - 1]}`);
    if (changing.length === 6 && cast.primary.allLines) {
      parts.push(`- เมื่อเส้นทั้งหกแปรพร้อมกัน: ${cast.primary.allLines}`);
    }
  }
  if (cast.relating) {
    parts.push("", hexagramBlock("กวาแปร (ทิศทางที่สถานการณ์กำลังคลี่คลายไป)", cast.relating, false));
  }
  return parts.join("\n");
}

export function sanitizeQuestion(raw: unknown, max = MAX_QUESTION_CHARS): string {
  if (typeof raw !== "string") return "";
  // Strip control characters and fake section markers a user might use to steer the prompt.
  return raw
    .replace(/[\u0000-\u0008\u000B-\u001F\u007F]/g, " ")
    .replace(/\[(context data|คำถาม|system)[^\]]*\]/gi, " ")
    .trim()
    .slice(0, max);
}

export function parseCategory(raw: unknown): Category | null {
  return typeof raw === "string" && (CATEGORIES as readonly string[]).includes(raw) ? (raw as Category) : null;
}

export function buildReadingPrompt(
  question: string,
  cast: Pick<Cast, "lines" | "primary" | "relating">,
  category: Category | null = null,
): Prompt {
  const user = [
    scriptureContext(cast),
    "",
    category ? `[หมวดคำถาม] ${CATEGORY_TH[category]}` : null,
    "[คำถามของผู้แสวงหา]",
    question.trim() || "ผู้แสวงหามิได้เอ่ยคำถาม เพียงตั้งจิตอธิษฐานขอคำชี้แนะในวิถีชีวิตยามนี้",
    "",
    "จงแปลผลคัมภีร์ข้างต้นให้เข้ากับคำถามนี้ ตามกฎการตอบอย่างเคร่งครัด",
  ]
    .filter((x) => x !== null)
    .join("\n");
  return { systemInstruction: PERSONA, userMessage: user };
}

export interface FollowUpTurn {
  q: string;
  a: string;
}

const clip = (s: string, n: number) => (s.length <= n ? s : `${s.slice(0, n)}…`);

/**
 * Follow-up prompt. To keep input tokens low it sends a summary — the cast, the opening of the
 * first reading and short versions of earlier turns — never the full conversation.
 */
export function buildFollowUpPrompt(
  question: string,
  cast: Pick<Cast, "lines" | "primary" | "relating">,
  firstReading: string,
  previous: FollowUpTurn[],
  message: string,
): Prompt {
  const user = [
    scriptureContext(cast),
    "",
    "[คำถามแรกของผู้แสวงหา]",
    question.trim() || "(อธิษฐานในใจ)",
    "",
    "[สรุปคำทำนายเดิม]",
    clip(firstReading.trim(), 700),
    ...(previous.length
      ? ["", "[บทสนทนาก่อนหน้า (ย่อ)]", ...previous.map((t) => `ผู้แสวงหา: ${clip(t.q, 150)}\nนักพรต: ${clip(t.a, 220)}`)]
      : []),
    "",
    "[คำถามต่อเนื่อง]",
    message.trim(),
  ].join("\n");
  return { systemInstruction: `${PERSONA}\n\n${FOLLOW_UP_RULES}`, userMessage: user };
}
