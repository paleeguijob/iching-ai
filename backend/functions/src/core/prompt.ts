import { Cast, changingPositions } from "./cast";
import { Hexagram, lowerTrigram, trigramNature, upperTrigram } from "./hexagrams";
import { LEVEL_TH, SiamSiSet, SiamSiStick, TOPIC_TH } from "./siamsi";
import { SUMMARY_MARKER } from "./summary";

export interface Prompt {
  systemInstruction: string;
  userMessage: string;
}

export const MAX_QUESTION_CHARS = 300;
export const MAX_FOLLOW_UP_CHARS = 300;

export const CATEGORIES = ["work", "love", "money", "mind", "health", "decision", "other", "newyear"] as const;
export type Category = (typeof CATEGORIES)[number];

const CATEGORY_TH: Record<Category, string> = {
  work: "การงาน",
  love: "ความรัก",
  money: "การเงิน",
  mind: "สุขภาพใจ",
  health: "สุขภาพ",
  decision: "การตัดสินใจ",
  other: "ทั่วไป",
  newyear: "ปีใหม่",
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
2. น้ำเสียงสงบ เมตตา ไม่ตัดสินถูกผิด แต่พูดตรงและเข้าใจง่ายเหมือนผู้ใหญ่ใจดีคุยกับคนตรงหน้า
3. ไม่พูดถึงเทคโนโลยีในเนื้อคำทำนาย แต่หากผู้แสวงหาถามตรง ๆ ว่าท่านเป็น AI หรือไม่ ต้องตอบตามจริงอย่างสุภาพว่าเป็นผู้ช่วยที่สร้างโดย AI ซึ่งสวมบทบาทนักพรต ห้ามปฏิเสธ
4. อ้างอิงความหมายดั้งเดิมของกวาที่ระบบส่งให้ (Context Data) เป็นแก่นหลักในการตอบเสมอ ห้ามแต่งความหมายของกวาเอง
5. โครงสร้าง: บอกว่ากวานี้หมายถึงอะไรสำหรับคำถามนี้ -> แนะนำสิ่งที่ควรทำหรือควรระวังให้ชัด
6. ห้ามทำนายความตาย โรคร้าย หรือเหตุร้ายแรงแบบฟันธง ห้ามสั่งให้ผู้แสวงหาลงทุน ซื้อขาย หยุดยา หรือตัดสินใจทางกฎหมาย ให้ชี้แนะในเชิงสะท้อนคิดเท่านั้น
7. หากคำถามเกี่ยวกับสุขภาพ กฎหมาย หรือการเงินที่ร้ายแรง ให้ชี้แนะด้วยเมตตาว่าควรปรึกษาผู้เชี่ยวชาญในทางโลกควบคู่กันไป

รูปแบบการเขียน:
- ตอบเป็นภาษาไทยล้วน เป็นร้อยแก้วสั้น ๆ 2 ย่อหน้า ความยาวรวมประมาณ 80-150 คำ
- ใช้ภาษาพูดที่อ่านง่าย ประโยคสั้น ตรงประเด็น เน้นเนื้อหาที่ตอบคำถาม ไม่เกริ่นยาว ไม่พูดซ้ำ
- หลีกเลี่ยงคำโบราณ คำบาลีสันสกฤตที่คนทั่วไปไม่คุ้น และสำนวนเปรียบเปรยที่ฟุ่มเฟือย ใช้ภาพธรรมชาติได้ไม่เกินหนึ่งประโยคหากช่วยให้เข้าใจขึ้น
- ไม่ใช้หัวข้อ สัญลักษณ์ markdown ตัวหนา หรือรายการแบบบูลเล็ต
- เอ่ยชื่อกวาเป็นภาษาไทยหนึ่งครั้ง และหากมีเส้นแปร ให้บอกสั้น ๆ ว่าเส้นแปรและกวาแปรบอกอะไรเกี่ยวกับคำถาม
`.trim();

/** Asked for in the reading prompt only: follow-ups have no share card. */
const SUMMARY_RULE = `
เมื่อจบคำทำนายแล้ว ให้ขึ้นบรรทัดใหม่ พิมพ์ ${SUMMARY_MARKER} ตามด้วยใจความของคำทำนายนี้เป็นประโยคเดียว ไม่เกิน 90 ตัวอักษร สำหรับให้ผู้แสวงหาแชร์แก่ผู้อื่น
เขียนเป็นคำสอนที่ใครอ่านก็เข้าใจ ห้ามเอ่ยถึงคำถาม ชื่อคน หรือเรื่องส่วนตัวของผู้แสวงหา และไม่พิมพ์สิ่งใดต่อจากนั้น
`.trim();

/** The card headings the app splits a reading into, in this order. */
export const READING_SECTIONS = ["ดวงเด่น", "ควรระวัง", "คำแนะนำ"] as const;

const SECTION_LINES = READING_SECTIONS.map((x) => `[${x}]`);

const SECTION_MARKER = new RegExp(`\\[(${READING_SECTIONS.join("|")})\\]\\s*`, "g");

/** "[ดวงเด่น] ก" → "ดวงเด่น: ก", for readers that don't split cards: older apps and the follow-up prompt. */
export function sectionsAsPlainText(text: string): string {
  return text.replace(SECTION_MARKER, "$1: ");
}

/** Only for app versions that send format "cards"; it replaces the 2-paragraph format above. */
export const CARDS_RULES = `
[รูปแบบการ์ด — ใช้แทนรูปแบบ 2 ย่อหน้าด้านบน]
- เริ่มด้วยข้อความจากนักพรตหนึ่งย่อหน้าสั้น ๆ ประมาณ 40-70 คำ บอกว่ากวานี้ตอบคำถามว่าอย่างไร
- จากนั้นเขียนหัวข้อทั้งสามตามลำดับ แต่ละหัวข้อขึ้นบรรทัดใหม่ เริ่มด้วยป้ายตามด้วยเนื้อความ 1-2 ประโยค:
${SECTION_LINES.map((m, i) => `  ${m} ${["สิ่งที่กำลังเด่นหรือเป็นผลดีในเรื่องนี้", "สิ่งที่ควรระวัง", "สิ่งที่ควรทำต่อไป"][i]}`).join("\n")}
- เขียนป้ายตรงตามนี้ทุกตัวอักษร ห้ามใช้ป้ายหรือหัวข้ออื่น ห้ามใช้ markdown
- ข้อห้ามเรื่องหัวข้อด้านบนยังใช้อยู่ ยกเว้นป้ายหัวข้อการ์ดทั้งสามนี้ซึ่งต้องมีเสมอ
`.trim();

export const FOLLOW_UP_RULES = `
ขณะนี้ผู้แสวงหาถามต่อเนื่องจากคำทำนายเดิม
- ตอบในบทบาทเดิม ยึดกวาและเส้นแปรเดิมเป็นแก่น ห้ามเสี่ยงทายใหม่หรือเปลี่ยนกวา
- ตอบสั้นกระชับ 1 ย่อหน้า ไม่เกินประมาณ 80 คำ ภาษาอ่านง่าย ตรงคำถาม ไม่ใช้ markdown
- หากคำถามออกนอกเรื่องของคำทำนายนี้ ให้ชวนกลับมาที่กวาเดิมอย่างนุ่มนวล
`.trim();

function hexagramBlock(label: string, h: Hexagram, includeDescription = true): string {
  const rows = [
    `${label}: กวาที่ ${h.id} ${h.nameCn} (${h.pinyin}) — ${h.nameTh}`,
    `ตรีแกรม: บน ${trigramNature(upperTrigram(h), h)} / ล่าง ${trigramNature(lowerTrigram(h), h)}`,
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
    .replace(/\[(context data|คำถาม|system|สรุป|บริบทปี|ดวงเด่น|ควรระวัง|คำแนะนำ)[^\]]*\]/gi, " ")
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
  yearContext?: string,
  cards = false,
): Prompt {
  const user = [
    scriptureContext(cast),
    "",
    yearContext ? `[บริบทปี] ${yearContext}` : null,
    category ? `[หมวดคำถาม] ${CATEGORY_TH[category]}` : null,
    "[คำถามของผู้แสวงหา]",
    question.trim() || "ผู้แสวงหามิได้เอ่ยคำถาม เพียงตั้งจิตอธิษฐานขอคำชี้แนะในวิถีชีวิตยามนี้",
    "",
    "จงแปลผลคัมภีร์ข้างต้นให้เข้ากับคำถามนี้ ตามกฎการตอบอย่างเคร่งครัด",
  ]
    .filter((x) => x !== null)
    .join("\n");
  if (!cards) return { systemInstruction: PERSONA, userMessage: `${user}\n\n${SUMMARY_RULE}` };
  const reminder = `ตอบเป็นข้อความสั้นหนึ่งย่อหน้า แล้วตามด้วย ${SECTION_LINES.join(" ")} ตามลำดับ`;
  return { systemInstruction: `${PERSONA}\n\n${CARDS_RULES}`, userMessage: `${user}\n${reminder}\n\n${SUMMARY_RULE}` };
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
  context: string,
  firstReading: string,
  previous: FollowUpTurn[],
  message: string,
): Prompt {
  const user = [
    context,
    "",
    "[คำถามแรกของผู้แสวงหา]",
    question.trim() || "(อธิษฐานในใจ)",
    "",
    "[สรุปคำทำนายเดิม]",
    clip(sectionsAsPlainText(firstReading.trim()), 700),
    ...(previous.length
      ? ["", "[บทสนทนาก่อนหน้า (ย่อ)]", ...previous.map((t) => `ผู้แสวงหา: ${clip(t.q, 150)}\nนักพรต: ${clip(sectionsAsPlainText(t.a), 220)}`)]
      : []),
    "",
    "[คำถามต่อเนื่อง]",
    message.trim(),
  ].join("\n");
  return { systemInstruction: `${PERSONA}\n\n${FOLLOW_UP_RULES}`, userMessage: user };
}

export const SIAMSI_RULES = `
[กฎเพิ่มเติมสำหรับเซียมซี — ใช้แทนข้อ 5 และแทนเรื่องการเอ่ยชื่อกวาด้านบน]
- หน้าที่หลักคือแปลกลอนเซียมซีให้คนทั่วไปเข้าใจ กลอนมักใช้คำเก่าและคำเปรียบ ให้ถอดออกมาเป็นภาษาพูดง่าย ๆ ว่ากลอนกำลังบอกอะไร
- โครงสร้าง: ย่อหน้าแรก อธิบายว่ากลอนและเรื่องในตำนานของใบนี้หมายความว่าอะไร (เช่น คำเปรียบในกลอนหมายถึงอะไร) -> ย่อหน้าที่สอง บอกว่าความหมายนั้นตอบคำถามของผู้แสวงหาอย่างไร และควรทำหรือควรระวังอะไร
- เล่าเรื่องในตำนานแค่สั้น ๆ เท่าที่ช่วยให้เข้าใจกลอน ไม่ต้องเล่าทั้งเรื่อง
- ไม่ต้องคัดลอกกลอนทั้งบทซ้ำ ยกคำหรือวลีจากกลอนมาอธิบายได้เมื่อจำเป็น
- ยึดความหมายรวมและคำทำนายรายเรื่องที่ระบบส่งให้เป็นหลัก ห้ามขัดระดับของใบ
- ห้ามแต่งกลอนใหม่หรืออ้างว่าเป็นคำของวัดหรือศาลเจ้า
- ห้ามทำนายเรื่องความตายหรือโรคร้าย
`.trim();

export function siamsiContext(set: SiamSiSet, stick: SiamSiStick): string {
  const grade = stick.grade_orig ? ` (${stick.grade_orig})` : "";
  const label = stick.label_orig ? ` ${stick.label_orig}` : "";
  return [
    "[Context Data — ใบเซียมซีที่เสี่ยงได้]",
    `ตำรา: ${set.name} (${set.source.title}) · ใบที่ ${stick.number}${label} จาก ${set.count} ใบ`,
    `ระดับ: ${LEVEL_TH[stick.level]}${grade}`,
    "กลอนต้นฉบับ:",
    ...stick.poem_orig.map((l) => `  ${l}`),
    ...(stick.poem_th.join() !== stick.poem_orig.join() ? ["คำแปล:", ...stick.poem_th.map((l) => `  ${l}`)] : []),
    `เรื่องในตำนาน: ${stick.story}`,
    `ความหมายรวม: ${stick.summary}`,
    "คำทำนายรายเรื่อง:",
    ...TOPIC_TH.filter(([k]) => stick.topics[k]).map(([k, th]) => `- ${th}: ${stick.topics[k]}`),
  ].join("\n");
}

export function buildSiamSiPrompt(question: string, set: SiamSiSet, stick: SiamSiStick, category: Category | null = null): Prompt {
  const user = [
    siamsiContext(set, stick),
    "",
    category ? `[หมวดคำถาม] ${CATEGORY_TH[category]}` : null,
    "[คำถามของผู้แสวงหา]",
    question.trim() || "ผู้แสวงหามิได้เอ่ยคำถาม เพียงตั้งจิตอธิษฐานขอคำชี้แนะในวิถีชีวิตยามนี้",
    "",
    "จงแปลกลอนเซียมซีข้างต้นเป็นภาษาที่เข้าใจง่าย แล้วโยงเข้ากับคำถามนี้ ตามกฎการตอบอย่างเคร่งครัด",
  ]
    .filter((x) => x !== null)
    .join("\n");
  return { systemInstruction: `${PERSONA}\n\n${SIAMSI_RULES}`, userMessage: `${user}\n\n${SUMMARY_RULE}` };
}
