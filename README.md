# อี้จิง AI (I Ching AI)

แอปทำนายและให้คำปรึกษาผ่านคัมภีร์อี้จิง ในบทบาท "นักพรตเต๋า" — Kotlin / Compose Multiplatform (Android + iOS)

## ฟีเจอร์ (v1.0 — Store Release)
- **เสี่ยงทาย** — สุ่มฝั่ง server (3 เหรียญ × 6 ครั้ง) แอปเล่นแอนิเมชัน + เสียง + haptic ตามผล ออฟไลน์สุ่มในเครื่องได้
- **คำทำนาย AI** — RAG: คำถาม + ตัวบทคัมภีร์ + Persona → Gemini (streaming) ผ่าน Cloud Function
  Free 1 ครั้ง/วัน + ชมโฆษณาแลก 2 ครั้ง · Premium 10 ครั้ง/วัน · เหรียญอี้จิง
- **ถามต่อกับนักพรต** (Premium) 3 คำถามต่อคำทำนาย
- **คัมภีร์ ๖๔ กวา** — ค้นหาได้ ออฟไลน์
- **บันทึกชะตา** — Free 7 วันล่าสุด · Premium ไม่จำกัด + กู้คืนข้ามเครื่อง · "เกิดขึ้นจริงไหม" + บันทึกส่วนตัว
- **บัญชี** — Anonymous → ผูก Apple / Google · Restore purchases · ลบบัญชีในแอป
- **Safety** — คำถามวิกฤตได้รับสายด่วน 1323/1669 แทนคำทำนาย · ป้าย AI-generated · ปุ่มรายงานคำตอบ

## ฟีเจอร์ (v1.1)
- **กวาประจำวัน** — กวาเดียวกันทุกคนตามวันเวลาไทย พร้อมข้อความที่เขียนไว้ล่วงหน้า 64 ชุด ไม่เรียก AI · แจ้งเตือน 07:00 ผ่าน FCM (เลือกเปิดเอง, ยังไม่รองรับ iOS)
- **การ์ดแชร์** — ภาพม้วนคัมภีร์ 1080×1920 จากกวาประจำวันหรือคำทำนาย ไม่มีคำถามของผู้ใช้
- **เส้นทางเต๋า** — นับวันที่เปิดดูกวาประจำวันต่อเนื่อง ปลดล็อกธีมที่ 7, 30 และ 100 วัน
- **ฟังคำทำนาย** (Premium) — ใช้เสียงอ่านของเครื่อง

## สถาปัตยกรรม

```
iching-ai/
├── frontend/   แอปมือถือ (Kotlin Compose Multiplatform) — เปิดโฟลเดอร์นี้ใน Android Studio
│   ├── androidApp/   Firebase/RevenueCat/AdMob SDK (Kotlin)
│   ├── iosApp/       Swift bridges + SPM packages (เปิด iosApp.xcodeproj ใน Xcode)
│   ├── composeApp/   UI, ViewModel, AppSession, NativeBridges
│   └── core/         domain (Kotlin ล้วน) + data (Ktor → backend, Room, DataStore, คัมภีร์ hexagrams.json)
├── backend/    Firebase — รัน firebase CLI จากโฟลเดอร์นี้
│   ├── functions/    Cloud Functions (TypeScript): /divine /follow-up /status /ad-reward /report /readings /purchases/sync /account · `dailyPush` (ตั้งเวลา 07:00)
│   ├── hosting/      privacy / terms / delete-account
│   └── firebase.json, .firebaserc, firestore.rules
└── docs/       spec + คู่มือขึ้น store

frontend ↔ backend คุยกันผ่าน HTTPS เท่านั้น (สัญญา API: backend/functions/src/core/api.ts ↔ frontend/core/data/.../HttpOracleApi.kt)
ข้อมูลที่ใช้ร่วมกันมีชิ้นเดียวคือคัมภีร์: ต้นฉบับอยู่ที่ frontend/core/data/src/commonMain/composeResources/files/hexagrams.json
และ backend คัดลอกไปใช้อัตโนมัติตอน build/test
```

| Module | หน้าที่ |
|---|---|
| `frontend/core/domain` | Model, repository interfaces, use cases — Kotlin ล้วน |
| `frontend/core/data` | คัมภีร์ (`hexagrams.json`), `HttpOracleApi` (SSE), Room v2 (journal), DataStore |
| `frontend/composeApp` | Compose UI, ViewModel, Koin, `AppSession`, `NativeBridges` (สัญญากับโค้ด native) |
| `frontend/androidApp` / `frontend/iosApp` | จุดเริ่ม + implementation ของ NativeBridges ด้วย SDK ทางการ |
| `backend/functions` | ตรรกะ server ทั้งหมดอยู่ใน `src/core` (ทดสอบได้โดยไม่ต้องมี Firebase) |

## เริ่มใช้งาน / ขึ้น store
ดู **[docs/RELEASE.md](docs/RELEASE.md)** — ขั้นตอนตั้งค่า Firebase, RevenueCat, AdMob, build และส่ง review ทีละข้อ

## ทดสอบ
```bash
cd backend/functions && npm install && npm test   # backend 63 tests
cd frontend && ./gradlew :core:domain:jvmTest :core:data:testAndroid :composeApp:testAndroid
```

## ฟอนต์
Noto Serif Thai และ Charm — SIL Open Font License
