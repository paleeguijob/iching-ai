import { Cast, changingPositions } from "./cast";
import { Hexagram, lowerTrigram, trigramNature, upperTrigram } from "./hexagrams";
import type { Lang } from "./lang";
import { LEVEL_EN, LEVEL_TH, SiamSiSet, SiamSiStick, TOPIC_EN, TOPIC_TH } from "./siamsi";
import { MAX_SUMMARY_CHARS, SUMMARY_MARKER } from "./summary";

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

const CATEGORY_EN: Record<Category, string> = {
  work: "Work",
  love: "Love",
  money: "Money",
  mind: "Peace of mind",
  health: "Health",
  decision: "A decision",
  other: "General",
  newyear: "New Year",
};

const categoryName = (c: Category, lang: Lang) => (lang === "en" ? CATEGORY_EN : CATEGORY_TH)[c];

const LINE_NAMES = ["หนึ่ง", "สอง", "สาม", "สี่", "ห้า", "หก"];
export const linePositionTh = (position: number) => `เส้นที่ ${LINE_NAMES[position - 1]}`;
export const linePositionEn = (position: number) => `Line ${position}`;
export const linePosition = (position: number, lang: Lang) => (lang === "en" ? linePositionEn(position) : linePositionTh(position));

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

/** The same hermit in English. Same rules as [PERSONA]; the hermit speaks as "I" and calls the user "seeker" or "you". */
export const PERSONA_EN = `
You are a Taoist hermit who lives in seclusion on the peaks of Mount Wudang. You live simply and understand the truth of change (the Tao). Your task is to interpret the I Ching hexagram that was cast and relate it to the user's question.

Guidelines:
1. Speak of yourself as "I" and address the user as "you" or "seeker".
2. Your tone is calm, kind and never judging, yet plain and direct, like a kind elder talking with the person in front of them.
3. Do not talk about technology in the reading itself. But if the seeker asks directly whether you are an AI, answer honestly and politely that you are an assistant made with AI, playing the role of a hermit. Never deny it.
4. Always build your answer on the traditional meaning of the hexagram the system gives you (Context Data). Never invent a meaning for the hexagram.
5. Structure: say what this hexagram means for this question -> then say clearly what to do or what to watch out for.
6. Never predict death, serious illness or grave misfortune as certain, and never promise an outcome. Never tell the seeker to invest, buy or sell, stop a medicine or make a legal decision. Offer guidance for reflection only.
7. If the question is about a serious health, legal or money matter, gently advise the seeker to also consult a qualified professional.

Writing style:
- Answer only in English, in short prose of 2 paragraphs, about 80-150 words in total.
- Use plain, everyday English. Keep sentences short and to the point. Answer the question; no long opening, no repetition.
- Avoid old-fashioned words, religious jargon and heavy metaphor. One sentence of nature imagery is fine if it makes things clearer.
- No headings, markdown, bold text or bullet lists.
- Name the hexagram once by its English name, and if there are changing lines, say briefly what the changing lines and the relating hexagram tell about the question.
`.trim();

/** Asked for in the reading prompt only: follow-ups have no share card. */
const SUMMARY_RULE = `
เมื่อจบคำทำนายแล้ว ให้ขึ้นบรรทัดใหม่ พิมพ์ ${SUMMARY_MARKER} ตามด้วยใจความของคำทำนายนี้เป็นประโยคเดียว ไม่เกิน 90 ตัวอักษร สำหรับให้ผู้แสวงหาแชร์แก่ผู้อื่น
เขียนเป็นคำสอนที่ใครอ่านก็เข้าใจ ห้ามเอ่ยถึงคำถาม ชื่อคน หรือเรื่องส่วนตัวของผู้แสวงหา และไม่พิมพ์สิ่งใดต่อจากนั้น
`.trim();

/** The card holds [MAX_SUMMARY_CHARS].en; the model is asked for less so a summary rarely runs over. */
const SUMMARY_RULE_EN = `
When the reading is finished, start a new line and write ${SUMMARY_MARKER} exactly as shown, in Thai script, then the heart of this reading in one English sentence of no more than ${MAX_SUMMARY_CHARS.en - 20} characters, for the seeker to share with others.
Write it as a teaching anyone can understand. Never mention the question, anyone's name or the seeker's personal matters, and write nothing after it.
`.trim();

/** The card headings the app splits a reading into, in this order. */
export const READING_SECTIONS = ["ดวงเด่น", "ควรระวัง", "คำแนะนำ"] as const;

const SECTION_LINES = READING_SECTIONS.map((x) => `[${x}]`);

const SECTION_MARKER = new RegExp(`\\[(${READING_SECTIONS.join("|")})\\]\\s*`, "g");

const SECTION_NAMES_EN: Record<(typeof READING_SECTIONS)[number], string> = {
  ดวงเด่น: "Strengths",
  ควรระวัง: "Be careful",
  คำแนะนำ: "Advice",
};

/**
 * "[ดวงเด่น] ก" → "ดวงเด่น: ก", for readers that don't split cards: older apps and the follow-up prompt.
 * In English the label is translated too ("Strengths: ก"), so an English prompt carries no Thai beyond its markers.
 */
export function sectionsAsPlainText(text: string, lang: Lang = "th"): string {
  if (lang !== "en") return text.replace(SECTION_MARKER, "$1: ");
  return text.replace(SECTION_MARKER, (_, name: (typeof READING_SECTIONS)[number]) => `${SECTION_NAMES_EN[name]}: `);
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

/** [CARDS_RULES] in English. The labels stay in Thai script: the app finds the cards by them and strips them. */
export const CARDS_RULES_EN = `
[Card format — use this instead of the 2-paragraph format above]
- Begin with one short paragraph from the hermit, about 40-70 words, saying how this hexagram answers the question.
- Then write the three sections in this order, each on a new line, starting with its label followed by 1-2 sentences:
${SECTION_LINES.map((m, i) => `  ${m} ${["what is strong or favourable in this matter", "what to be careful about", "what to do next"][i]}`).join("\n")}
- Write each label exactly as shown, character for character, in Thai script, even though everything else is in English. Never translate the labels, and use no other labels or headings. No markdown.
- The no-headings rule above still applies, except for these three card labels, which must always be there.
`.trim();

export const FOLLOW_UP_RULES = `
ขณะนี้ผู้แสวงหาถามต่อเนื่องจากคำทำนายเดิม
- ตอบในบทบาทเดิม ยึดกวาและเส้นแปรเดิมเป็นแก่น ห้ามเสี่ยงทายใหม่หรือเปลี่ยนกวา
- ตอบสั้นกระชับ 1 ย่อหน้า ไม่เกินประมาณ 80 คำ ภาษาอ่านง่าย ตรงคำถาม ไม่ใช้ markdown
- หากคำถามออกนอกเรื่องของคำทำนายนี้ ให้ชวนกลับมาที่กวาเดิมอย่างนุ่มนวล
`.trim();

export const FOLLOW_UP_RULES_EN = `
The seeker is now asking a follow-up to the earlier reading.
- Answer in the same role, keeping the same hexagram and changing lines at the heart. Never cast again or change the hexagram.
- Keep it short: 1 paragraph of no more than about 80 words, in plain English, answering the question directly. No markdown.
- If the question strays from this reading, gently bring the seeker back to the original hexagram.
- Answer only in English.
`.trim();

function hexagramBlock(label: string, h: Hexagram, includeDescription = true, lang: Lang = "th"): string {
  if (lang === "en") {
    const rows = [
      `${label}: Hexagram ${h.id} ${h.nameCn} (${h.pinyin}) — ${h.nameTh}`,
      `Trigrams: upper ${trigramNature(upperTrigram(h), h)} / lower ${trigramNature(lowerTrigram(h), h)}`,
      `Judgment: ${h.judgment}`,
      `Image: ${h.image}`,
    ];
    if (includeDescription) rows.push(`Overview: ${h.description}`);
    return rows.join("\n");
  }
  const rows = [
    `${label}: กวาที่ ${h.id} ${h.nameCn} (${h.pinyin}) — ${h.nameTh}`,
    `ตรีแกรม: บน ${trigramNature(upperTrigram(h), h)} / ล่าง ${trigramNature(lowerTrigram(h), h)}`,
    `คำตัดสิน: ${h.judgment}`,
    `ภาพลักษณ์: ${h.image}`,
  ];
  if (includeDescription) rows.push(`ภาพรวม: ${h.description}`);
  return rows.join("\n");
}

/**
 * Scripture context: primary hexagram, only the changing lines that were cast, and the relating hexagram.
 * [lang] defaults to the language of the cast's hexagrams.
 */
export function scriptureContext(cast: Pick<Cast, "lines" | "primary" | "relating">, lang: Lang = cast.primary.lang ?? "th"): string {
  if (lang === "en") return scriptureContextEn(cast);
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

function scriptureContextEn(cast: Pick<Cast, "lines" | "primary" | "relating">): string {
  const parts = ["[Context Data — the I Ching hexagram that was cast]", hexagramBlock("Primary hexagram", cast.primary, true, "en")];
  const changing = changingPositions(cast.lines);
  if (changing.length === 0) {
    parts.push("Changing lines: none — keep the judgment and image of the primary hexagram at the heart");
  } else {
    parts.push(`Changing lines (${changing.length}) — their specific meanings, to be stressed:`);
    for (const p of changing) parts.push(`- ${linePositionEn(p)}: ${cast.primary.lines[p - 1]}`);
    if (changing.length === 6 && cast.primary.allLines) {
      parts.push(`- When all six lines change together: ${cast.primary.allLines}`);
    }
  }
  if (cast.relating) {
    parts.push("", hexagramBlock("Relating hexagram (the direction the situation is unfolding toward)", cast.relating, false, "en"));
  }
  return parts.join("\n");
}

export function sanitizeQuestion(raw: unknown, max = MAX_QUESTION_CHARS): string {
  if (typeof raw !== "string") return "";
  // Strip control characters and fake section markers a user might use to steer the prompt.
  return raw
    .replace(/[\u0000-\u0008\u000B-\u001F\u007F]/g, " ")
    .replace(/\[(context data|คำถาม|system|สรุป|บริบทปี|ดวงเด่น|ควรระวัง|คำแนะนำ|seeker|question|year context|earlier|follow-up)[^\]]*\]/gi, " ")
    .trim()
    .slice(0, max);
}

export function parseCategory(raw: unknown): Category | null {
  return typeof raw === "string" && (CATEGORIES as readonly string[]).includes(raw) ? (raw as Category) : null;
}

/** Everything a prompt says besides the scripture and rules, per language. */
const TEXT = {
  th: {
    persona: PERSONA,
    cardsRules: CARDS_RULES,
    followUpRules: FOLLOW_UP_RULES,
    summaryRule: SUMMARY_RULE,
    yearContext: "[บริบทปี]",
    category: "[หมวดคำถาม]",
    question: "[คำถามของผู้แสวงหา]",
    noQuestion: "ผู้แสวงหามิได้เอ่ยคำถาม เพียงตั้งจิตอธิษฐานขอคำชี้แนะในวิถีชีวิตยามนี้",
    interpret: "จงแปลผลคัมภีร์ข้างต้นให้เข้ากับคำถามนี้ ตามกฎการตอบอย่างเคร่งครัด",
    cardsReminder: `ตอบเป็นข้อความสั้นหนึ่งย่อหน้า แล้วตามด้วย ${SECTION_LINES.join(" ")} ตามลำดับ`,
    firstQuestion: "[คำถามแรกของผู้แสวงหา]",
    silentQuestion: "(อธิษฐานในใจ)",
    firstReading: "[สรุปคำทำนายเดิม]",
    earlier: "[บทสนทนาก่อนหน้า (ย่อ)]",
    seeker: "ผู้แสวงหา",
    hermit: "นักพรต",
    followUp: "[คำถามต่อเนื่อง]",
    interpretSiamSi: "จงแปลกลอนเซียมซีข้างต้นเป็นภาษาที่เข้าใจง่าย แล้วโยงเข้ากับคำถามนี้ ตามกฎการตอบอย่างเคร่งครัด",
  },
  en: {
    persona: PERSONA_EN,
    cardsRules: CARDS_RULES_EN,
    followUpRules: FOLLOW_UP_RULES_EN,
    summaryRule: SUMMARY_RULE_EN,
    yearContext: "[Year context]",
    category: "[Question category]",
    question: "[Seeker's question]",
    noQuestion: "The seeker asked no question in words, only quietly asked for guidance on the path of life at this time.",
    interpret:
      "Interpret the scripture above for this question, strictly following the guidelines. Answer only in English; the only Thai you write is the markers, exactly as shown.",
    cardsReminder: `Answer with one short paragraph, then follow with ${SECTION_LINES.join(" ")} in that order.`,
    firstQuestion: "[Seeker's first question]",
    silentQuestion: "(a silent prayer)",
    firstReading: "[Earlier reading in brief]",
    earlier: "[Earlier conversation (shortened)]",
    seeker: "Seeker",
    hermit: "Hermit",
    followUp: "[Follow-up question]",
    interpretSiamSi:
      "Explain the Siam Si verse above in plain English, then relate it to this question, strictly following the guidelines. Answer only in English; the only Thai you write is the markers, exactly as shown.",
  },
} as const;

export function buildReadingPrompt(
  question: string,
  cast: Pick<Cast, "lines" | "primary" | "relating">,
  category: Category | null = null,
  yearContext?: string,
  cards = false,
  lang: Lang = "th",
): Prompt {
  const t = TEXT[lang];
  const user = [
    scriptureContext(cast, lang),
    "",
    yearContext ? `${t.yearContext} ${yearContext}` : null,
    category ? `${t.category} ${categoryName(category, lang)}` : null,
    t.question,
    question.trim() || t.noQuestion,
    "",
    t.interpret,
  ]
    .filter((x) => x !== null)
    .join("\n");
  if (!cards) return { systemInstruction: t.persona, userMessage: `${user}\n\n${t.summaryRule}` };
  return { systemInstruction: `${t.persona}\n\n${t.cardsRules}`, userMessage: `${user}\n${t.cardsReminder}\n\n${t.summaryRule}` };
}

export interface FollowUpTurn {
  q: string;
  a: string;
}

const clip = (s: string, n: number) => (s.length <= n ? s : `${s.slice(0, n)}…`);

/**
 * Follow-up prompt. To keep input tokens low it sends a summary — the cast, the opening of the
 * first reading and short versions of earlier turns — never the full conversation.
 * [lang] is the language of the reading being followed up, which [context] must be in too.
 */
export function buildFollowUpPrompt(
  question: string,
  context: string,
  firstReading: string,
  previous: FollowUpTurn[],
  message: string,
  lang: Lang = "th",
): Prompt {
  const t = TEXT[lang];
  const user = [
    context,
    "",
    t.firstQuestion,
    question.trim() || t.silentQuestion,
    "",
    t.firstReading,
    clip(sectionsAsPlainText(firstReading.trim(), lang), 700),
    ...(previous.length
      ? ["", t.earlier, ...previous.map((p) => `${t.seeker}: ${clip(p.q, 150)}\n${t.hermit}: ${clip(sectionsAsPlainText(p.a, lang), 220)}`)]
      : []),
    "",
    t.followUp,
    message.trim(),
  ].join("\n");
  return { systemInstruction: `${t.persona}\n\n${t.followUpRules}`, userMessage: user };
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

export const SIAMSI_RULES_EN = `
[Extra rules for Siam Si — these replace rule 5 and the rule about naming the hexagram above]
- Your main task is to explain the Siam Si verse so anyone can understand it. Verses often use old words and images; say in plain, everyday English what the verse is telling the seeker.
- Structure: in the first paragraph, explain what the verse and the legend of this slip mean (for example, what the images in the verse stand for) -> in the second paragraph, say how that meaning answers the seeker's question, and what to do or watch out for.
- Tell the legend only briefly, as far as it helps the verse make sense. Do not retell the whole story.
- Do not copy out the whole verse again. You may quote a word or phrase from it when needed.
- Keep to the overall meaning and the readings by topic that the system gives. Never contradict the level of the slip.
- Never write a new verse, and never claim to speak for a temple or shrine.
- Never predict death or serious illness.
`.trim();

export function siamsiContext(set: SiamSiSet, stick: SiamSiStick, lang: Lang = "th"): string {
  const grade = stick.grade_orig ? ` (${stick.grade_orig})` : "";
  const label = stick.label_orig ? ` ${stick.label_orig}` : "";
  if (lang === "en") {
    return [
      "[Context Data — the Siam Si slip that was drawn]",
      `Book: ${set.name} (${set.source.title}) · slip ${stick.number}${label} of ${set.count}`,
      `Level: ${LEVEL_EN[stick.level]}${grade}`,
      "Original verse:",
      ...stick.poem_orig.map((l) => `  ${l}`),
      ...(stick.poem_th.join() !== stick.poem_orig.join() ? ["Translation:", ...stick.poem_th.map((l) => `  ${l}`)] : []),
      `Legend: ${stick.story}`,
      `Overall meaning: ${stick.summary}`,
      "Readings by topic:",
      ...TOPIC_EN.filter(([k]) => stick.topics[k]).map(([k, en]) => `- ${en}: ${stick.topics[k]}`),
    ].join("\n");
  }
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

/** [set] and [stick] must already be in [lang] (findStick's result and its lang). */
export function buildSiamSiPrompt(
  question: string,
  set: SiamSiSet,
  stick: SiamSiStick,
  category: Category | null = null,
  lang: Lang = "th",
): Prompt {
  const t = TEXT[lang];
  const user = [
    siamsiContext(set, stick, lang),
    "",
    category ? `${t.category} ${categoryName(category, lang)}` : null,
    t.question,
    question.trim() || t.noQuestion,
    "",
    t.interpretSiamSi,
  ]
    .filter((x) => x !== null)
    .join("\n");
  const rules = lang === "en" ? SIAMSI_RULES_EN : SIAMSI_RULES;
  return { systemInstruction: `${t.persona}\n\n${rules}`, userMessage: `${user}\n\n${t.summaryRule}` };
}
