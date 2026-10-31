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

## สถาปัตยกรรม

```
androidApp (Firebase/RevenueCat/AdMob SDK) ─┐                       ┌─► core:data (Ktor → Cloud Function, Room, DataStore)
iosApp     (Swift bridges, SPM packages) ───┴─► composeApp (UI, VM) ─┤
                                                                    └─► core:domain
functions/  Cloud Functions (TypeScript): /divine /follow-up /status /ad-reward /report /readings /purchases/sync /account
hosting/    privacy / terms / delete-account
```

| Module | หน้าที่ |
|---|---|
| `core/domain` | Model, repository interfaces, use cases — Kotlin ล้วน |
| `core/data` | คัมภีร์ (`hexagrams.json`), `HttpOracleApi` (SSE), Room v2 (journal), DataStore |
| `composeApp` | Compose UI, ViewModel, Koin, `AppSession`, `NativeBridges` (สัญญากับโค้ด native) |
| `androidApp` / `iosApp` | จุดเริ่ม + implementation ของ NativeBridges ด้วย SDK ทางการ |
| `functions` | ตรรกะ server ทั้งหมดอยู่ใน `src/core` (ทดสอบได้โดยไม่ต้องมี Firebase) |

## เริ่มใช้งาน / ขึ้น store
ดู **[docs/RELEASE.md](docs/RELEASE.md)** — ขั้นตอนตั้งค่า Firebase, RevenueCat, AdMob, build และส่ง review ทีละข้อ

## ทดสอบ
```bash
cd functions && npm install && npm test          # backend 35 tests
./gradlew :core:domain:jvmTest :core:data:testAndroid
```

## ฟอนต์
Noto Serif Thai และ Charm — SIL Open Font License
