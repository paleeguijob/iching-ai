# อี้จิง AI — Design Spec (Compose Multiplatform)

อ้างอิง: "System Specification - อี้จิง AI" v1.0.0

## ขอบเขต
- แพลตฟอร์ม: Android + iOS ด้วย Kotlin / Compose Multiplatform (แทน Flutter ในสเปกเดิม)
- AI: แอปเรียก Google Gemini API โดยตรง (ไม่มี backend) ผู้ใช้ใส่ API key เองในหน้าตั้งค่า
  dev build อ่านค่าเริ่มต้นจาก `local.properties` (`GEMINI_API_KEY`) ซึ่งไม่ถูก commit
- ไม่มีบัญชีผู้ใช้ — บันทึกชะตาเก็บในเครื่อง

## สถาปัตยกรรม (Clean Architecture)

```
androidApp ──► composeApp (presentation + DI) ──► core:data ──► core:domain
iosApp (Swift) ─┘                                └───────────────► core:domain
```

| Module | หน้าที่ | ขึ้นกับ |
|---|---|---|
| `core:domain` | Model, repository interfaces, use cases (Kotlin ล้วน) | coroutines |
| `core:data` | คัมภีร์ (JSON resource), Gemini (Ktor SSE), Room (journal), DataStore (settings) | domain |
| `composeApp` | Compose UI, ViewModel (StateFlow), Navigation, Koin, เสียง (expect/actual) | domain, data |
| `androidApp` | Activity + Application | composeApp |
| `iosApp` | SwiftUI host ของ `MainViewController` | ComposeApp.framework |

## Flow หลัก (RAG)
1. ผู้ใช้พิมพ์คำถาม → 2. `CastHexagramUseCase` โยน 3 เหรียญ × 6 ครั้ง (หัว=3, ก้อย=2;
   6 = หยินแก่ (แปร), 7 = หยางอ่อน, 8 = หยินอ่อน, 9 = หยางแก่ (แปร)) → กวาหลัก + กวาแปร
3. `HexagramRepository` ดึงคำตัดสิน/ภาพลักษณ์/ภาพรวม/เส้นแปรที่เกี่ยวข้อง
4. `InterpretReadingUseCase` ประกอบ Persona Prompt (ตามสเปกข้อ 3) + context + คำถาม → Gemini
   (temperature 0.75, streaming)
5. แสดงผลแบบพิมพ์ทีละตัวในม้วนคัมภีร์ แล้วบันทึกลง Destiny Journal

Fallback: ไม่มี key / ไม่มีเน็ต / API error → แสดงคำแปลจากคัมภีร์ (`ScriptureFallbackComposer`) พร้อมปุ่มลองใหม่

## Pattern convention
`pattern` 6 ตัวอักษร อ่านจาก**ล่างขึ้นบน** (index 0 = เส้นที่ 1), `1` = หยาง, `0` = หยิน

## หน้าจอ
หน้าแรก (ตั้งจิต/คำถาม) · เสี่ยงทาย (เหรียญหมุน + เส้นก่อตัว + เสียง) · คำทำนาย (ม้วนคัมภีร์ typewriter + ตราชาด) ·
ห้องสมุด 64 กวา (ค้นหา + รายละเอียด) · บันทึกชะตา (ไทม์ไลน์) · ตั้งค่า (API key, โมเดล, เสียง)

ธีม: Paper Cream `#FAF9F6`, Ink Black `#2D3748`, Bamboo Green `#8F9779`, Cinnabar Red `#9C4221`
ฟอนต์: Noto Serif Thai (เนื้อหา), Charm (หัวเรื่อง)

## การทดสอบ
Unit test ใน `core:domain` (เสี่ยงทาย, การหากวาแปร, prompt) และ `core:data` (parse คัมภีร์ครบ 64 กวา)
ตรวจด้วยการรันบน Android emulator และ iOS Simulator
