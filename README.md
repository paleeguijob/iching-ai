# อี้จิง AI (I Ching AI)

แอปทำนายและให้คำปรึกษาผ่านคัมภีร์อี้จิง ในบทบาท "นักพรตเต๋า" — Kotlin / Compose Multiplatform (Android + iOS)

## ฟีเจอร์
- **เสี่ยงทาย** — โยนเหรียญโบราณ 3 เหรียญ × 6 ครั้ง พร้อมแอนิเมชันและเสียงเหรียญ / กระดิ่งลม
- **คำทำนาย** — RAG: คำถาม + ตัวบทคัมภีร์ (กวาหลัก, เส้นแปร, กวาแปร) + Persona → Gemini แบบ streaming
  แสดงในม้วนคัมภีร์แบบพิมพ์ทีละตัว ถ้าไม่มีกุญแจ/ออฟไลน์ จะแสดงตัวบทจากคัมภีร์แทน
- **คัมภีร์ ๖๔ กวา** — ค้นหาได้ พร้อมคำตัดสิน ภาพลักษณ์ ภาพรวม และเส้นทั้งหก (๓๘๔ เส้น)
- **บันทึกชะตา** — ประวัติคำถาม–คำตอบเป็นไทม์ไลน์ เก็บในเครื่อง
- **ตั้งค่า** — Gemini API key, เลือกโมเดล, เปิด/ปิดเสียง

## สถาปัตยกรรม (Clean Architecture)

```
androidApp ─┐
iosApp ─────┴─► composeApp (UI, ViewModel, DI) ─► core:data ─► core:domain
```

| Module | หน้าที่ |
|---|---|
| `core/domain` | Model, repository interfaces, use cases, prompt builder — Kotlin ล้วน |
| `core/data` | คัมภีร์ (`composeResources/files/hexagrams.json`), Gemini (Ktor SSE), Room (journal), DataStore (settings) |
| `composeApp` | Compose UI, MVVM + StateFlow, Navigation, Koin, เสียง (expect/actual) |
| `androidApp` / `iosApp` | จุดเริ่มของแต่ละแพลตฟอร์ม |

รายละเอียดดีไซน์: [docs/specs/2026-10-30-iching-ai-design.md](docs/specs/2026-10-30-iching-ai-design.md)

## เริ่มใช้งาน

1. สร้าง `local.properties` ที่ root (ไฟล์นี้ไม่ถูก commit):
   ```properties
   sdk.dir=/Users/<you>/Library/Android/sdk
   GEMINI_API_KEY=<กุญแจของคุณ>   # ไม่บังคับ — ใช้เป็นค่าเริ่มต้นของ dev build
   ```
2. Android: `./gradlew :androidApp:installDebug`
3. iOS: เปิด `iosApp/iosApp.xcodeproj` ใน Xcode แล้วกด Run (build phase จะเรียก Gradle ให้เอง)

ต้องใช้ JDK 17+ (ใช้ JBR ที่มากับ Android Studio ได้)

## ทดสอบ
```bash
./gradlew :core:domain:jvmTest :core:data:testAndroid
```

## หมายเหตุเรื่องความปลอดภัย
แอปเรียก Gemini ตรงจากเครื่องผู้ใช้ กุญแจที่ใส่ใน `local.properties` จะถูกฝังใน dev build —
อย่าแจกจ่าย build ที่มีกุญแจของคุณ สำหรับการเผยแพร่จริง ให้ผู้ใช้ใส่กุญแจเองในหน้าตั้งค่า
หรือย้ายการเรียก LLM ไปไว้หลัง backend

## ฟอนต์
Noto Serif Thai และ Charm — SIL Open Font License
