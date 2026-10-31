# คู่มือขึ้น Store — อี้จิง AI v1.0 (MVP)

โค้ดทั้ง backend และแอปพร้อมแล้ว เหลือขั้นตอนที่ต้องใช้บัญชีของคุณเอง ทำตามลำดับด้านล่าง
(☐ = ต้องทำเอง · ค่าที่ต้องกรอกอยู่ใน `frontend/local.properties`, `frontend/iosApp/Configuration/Secrets.xcconfig` และ Firebase secrets)

โครงสร้าง: **`frontend/`** = แอป (เปิดใน Android Studio / Xcode) · **`backend/`** = Firebase (รันคำสั่ง `firebase` จากโฟลเดอร์นี้)

---

## 0. สิ่งที่เปลี่ยนจากเวอร์ชันก่อน (สรุปสำหรับรีวิวโค้ด)

| ด้าน | เดิม | ตอนนี้ |
|---|---|---|
| เรียก AI | แอปเรียก Gemini ตรง ฝัง API key ในแอป | ผ่าน Cloud Function `api` เท่านั้น key อยู่ใน Firebase Secret |
| สุ่มกวา | ในเครื่อง | ฝั่ง server (crypto RNG, 3 เหรียญ = 1/8, 3/8, 3/8, 1/8) แอปเล่นแอนิเมชันตามผล · ออฟไลน์ยังสุ่มในเครื่องได้แต่ได้แค่ตัวบท |
| โควตา | ไม่มี | Free 1/วัน (Flash-Lite) + โฆษณาแลก 2/วัน · Premium 10/วัน (Flash) · เหรียญ 1 = 1 คำทำนาย · จำกัดต่อเครื่อง · rate limit 8 วิ · Gemini error = คืนโควตา |
| บัญชี | ไม่มี | Firebase Anonymous → ผูก Sign in with Apple (iOS) / Google (Android) · ลบบัญชีในแอป |
| เงิน | ไม่มี | RevenueCat (รายเดือน 79 / รายปี 590 + ทดลอง 7 วัน / เหรียญ 35 บาท 10 เหรียญ) + AdMob rewarded |
| ถามต่อ | ไม่มี | Premium ถามต่อ 3 รอบ/คำทำนาย (ส่งสรุป ไม่ส่ง history เต็ม) |
| Journal | เก็บในเครื่อง | Free เห็น 7 วันล่าสุด · Premium ไม่จำกัด + ดึงจาก server ข้ามเครื่อง · ปุ่ม "เกิดขึ้นจริงไหม" + บันทึกส่วนตัว |
| Safety | ไม่มี | คำถามทำร้ายตนเอง/ฉุกเฉินทางการแพทย์ → ข้อความคงที่ + 1323/1669 ไม่ทำนาย ไม่ตัดโควตา |
| Store compliance | ไม่มี | Onboarding disclaimer, ป้าย "สร้างโดย AI", ปุ่มรายงานคำตอบ, Restore/Manage subscription, เงื่อนไขต่ออายุบน paywall, privacy/terms/delete-account บน Firebase Hosting, ATT + UMP consent |
| Persona | "ห้ามบอกว่าเป็น AI" | ไม่พูดถึงเทคโนโลยีในคำทำนาย แต่ถ้าถามตรง ๆ ต้องยอมรับ |

ไฟล์สำคัญ: `backend/functions/src/core/*` (logic ทั้งหมดของ server + เทสต์ 35 ตัว) · `frontend/composeApp/.../session/AppSession.kt` ·
`frontend/composeApp/.../feature/divination/DivinationViewModel.kt` · `frontend/composeApp/.../platform/NativeBridges.kt` (สัญญากับโค้ด native) ·
`frontend/androidApp/.../platform/*` · `frontend/iosApp/iosApp/Bridges/*`

---

## 1. Firebase (มีโปรเจกต์แล้ว)

1. ☐ อัปเกรดเป็นแผน **Blaze** (Cloud Functions + Secret Manager ต้องใช้) แล้วตั้ง **Budget alert** ที่ 50/90/100% ใน Google Cloud Billing
2. ☐ แก้ `backend/.firebaserc` → ใส่ project id จริงแทน `YOUR_FIREBASE_PROJECT_ID`
3. ☐ Authentication → Sign-in method: เปิด **Anonymous**, **Apple**, **Google**
4. ☐ ลงทะเบียนแอป Android (`com.iching.ai`) ใส่ SHA-1 และ SHA-256 ของทั้ง debug key และ **App signing key ของ Play** → ดาวน์โหลด `google-services.json` วางที่ `frontend/androidApp/`
5. ☐ ลงทะเบียนแอป iOS (bundle id เดียวกับใน Xcode) → ดาวน์โหลด `GoogleService-Info.plist` วางที่ `frontend/iosApp/iosApp/` (โปรเจกต์ Xcode อ้างไฟล์นี้ไว้แล้ว)
6. ☐ App Check: Android = **Play Integrity**, iOS = **App Attest** · เพิ่ม debug token จาก Logcat/Xcode console ของ debug build ใน "Manage debug tokens"
7. ☐ Firestore: สร้างฐานข้อมูล (region `asia-southeast1`)
8. ☐ ตั้ง secrets แล้ว deploy:
   ```bash
   npm i -g firebase-tools && firebase login
   cd backend/functions && npm install && npm test   # ต้องผ่าน 35 เทสต์
   firebase functions:secrets:set GEMINI_API_KEY          # key จาก aistudio.google.com (แผนจ่ายเงิน = ข้อมูลไม่ถูกใช้ฝึกโมเดล)
   firebase functions:secrets:set REVENUECAT_API_KEY      # sk_... (ยังไม่มีใส่ unset ไปก่อน)
   firebase functions:secrets:set REVENUECAT_WEBHOOK_AUTH # สุ่มยาว ๆ เช่น openssl rand -hex 32
   cd .. && firebase deploy --only functions,firestore:rules,hosting   # รันใน backend/
   ```
9. ☐ ใส่ URL ใน `frontend/local.properties` (ทั้ง Android และ iOS build ใช้ไฟล์นี้):
   ```properties
   ICHING_API_BASE_URL=https://asia-southeast1-<project-id>.cloudfunctions.net/api
   ICHING_WEB_BASE_URL=https://<project-id>.web.app
   ```
10. ☐ แก้ `backend/hosting/public/*.html`: แทน `SUPPORT_EMAIL`, `DEVELOPER_NAME`, `EFFECTIVE_DATE` แล้ว deploy hosting อีกรอบ

**Kill-switch / ปรับโควตาโดยไม่ต้อง deploy:** สร้างเอกสาร Firestore `config/runtime` ใส่ฟิลด์ที่ต้องการ override เช่น
`aiEnabled: false` (ปิด AI ทุกคน → ได้ตัวบทแทน), `forceModel: "gemini-flash-lite-latest"` (สลับทุกคนไปรุ่นถูก),
`freeDailyReadings`, `premiumDailyReadings`, `adRewardsPerDay`, `enforceAppCheck` — มีผลภายใน 60 วินาที (ดูค่าทั้งหมดใน `backend/functions/src/core/quota.ts`)

**ดูต้นทุนจริง:** Cloud Logging กรอง `jsonPayload.message="reading_cost"` (มี tokensIn/tokensOut/costThb ต่อคำขอ) → สร้าง Log sink ไป BigQuery ถ้าต้องการรายงาน
ราคาในโค้ด (`PRICES_USD_PER_M` ใน `backend/functions/src/core/gemini.ts`) เป็นค่าประมาณจาก spec — เช็กหน้า pricing ของ Google แล้วแก้ให้ตรง

---

## 2. RevenueCat

1. ☐ สร้างโปรเจกต์ → เพิ่มแอป App Store และ Play Store
2. ☐ สร้างสินค้าใน store ทั้งสองฝั่ง (ใช้ product id เดียวกัน):
   - `iching_premium_monthly` — auto-renew รายเดือน 79 บาท
   - `iching_premium_yearly` — auto-renew รายปี 590 บาท + introductory offer **ทดลองฟรี 7 วัน**
   - `iching_coins_10` — **consumable** 35 บาท (ชื่อต้องมีคำว่า `coins` และตั้งเป็น consumable ใน RevenueCat)
3. ☐ Entitlement ชื่อ **`premium`** ผูกกับ 2 subscription
4. ☐ Offering ที่เป็น **Current**: package Monthly, Annual และ custom package สำหรับ `iching_coins_10`
5. ☐ Integrations → Webhooks: URL `https://asia-southeast1-<project-id>.cloudfunctions.net/revenuecatWebhook`, Authorization header = `Bearer <REVENUECAT_WEBHOOK_AUTH>`
6. ☐ ใส่ public SDK keys:
   - Android: `frontend/local.properties` → `REVENUECAT_ANDROID_KEY=goog_...`
   - iOS: สร้าง `frontend/iosApp/Configuration/Secrets.xcconfig` → `REVENUECAT_IOS_KEY = appl_...`
7. ☐ ใส่ secret key (v1 `sk_...`) ลง `REVENUECAT_API_KEY` แล้ว `firebase deploy --only functions`

---

## 3. AdMob

1. ☐ สร้างแอป Android + iOS และ ad unit แบบ **Rewarded** อย่างละ 1
2. ☐ ใส่ค่า (ตอนนี้ใช้ test id ของ Google อยู่ — **ห้ามปล่อย production ด้วย test id**):
   - Android `frontend/local.properties`: `ADMOB_APP_ID_ANDROID=ca-app-pub-...~...`, `ADMOB_REWARDED_ANDROID=ca-app-pub-.../...`
   - iOS `frontend/iosApp/Configuration/Secrets.xcconfig`: `ADMOB_APP_ID_IOS = ...`, `ADMOB_REWARDED_IOS = ...`
3. ☐ Privacy & messaging: สร้างข้อความ GDPR (UMP) — แอปเรียกฟอร์มให้อัตโนมัติเฉพาะผู้ใช้ในยุโรป
4. ☐ เพิ่ม `app-ads.txt` บนเว็บไซต์ผู้พัฒนา (ใส่ใน `backend/hosting/public/app-ads.txt` ได้)
5. ☐ iOS: อัปเดตรายการ `SKAdNetworkItems` ใน `Info.plist` ให้ครบตามเอกสาร AdMob ล่าสุด (ตอนนี้ใส่ของ Google ไว้ 1 รายการ)

---

## 4. Build แอป

**Android**
1. ☐ เปิดโฟลเดอร์ **`frontend/`** ใน Android Studio → Sync (ถ้า plugin/SDK เวอร์ชันใน `gradle/libs.versions.toml` ไม่ตรงกับที่มี ให้กด update ตามที่ IDE แนะนำ)
2. ☐ `./gradlew :core:domain:jvmTest :core:data:testAndroid` แล้วรันบนเครื่องจริง
3. ☐ สร้าง upload keystore แล้วเขียน `frontend/keystore.properties`:
   ```properties
   storeFile=release.jks
   storePassword=...
   keyAlias=upload
   keyPassword=...
   ```
4. ☐ `./gradlew :androidApp:bundleRelease` → ได้ `.aab`

**iOS**
1. ☐ เปิด `frontend/iosApp/iosApp.xcodeproj` → Xcode จะดึง Swift packages (Firebase 12, RevenueCat 5, Google Mobile Ads 12, UMP 3) ให้เอง
   — ถ้าดึงแล้วเป็นเวอร์ชันหลักที่ใหม่กว่า และชื่อ API ใน `frontend/iosApp/iosApp/Bridges/IosAdsAndAnalytics.swift` ไม่ตรง ให้ส่ง error มา
2. ☐ ใส่ `TEAM_ID` ใน `frontend/iosApp/Configuration/Config.xcconfig` (bundle id จะเป็น `com.iching.ai<TEAM_ID>` ตามเทมเพลตเดิม — ถ้าต้องการ `com.iching.ai` ตรง ๆ ให้แก้ `PRODUCT_BUNDLE_IDENTIFIER`)
3. ☐ Signing & Capabilities: ยืนยันว่ามี **Sign in with Apple** และ **App Attest** (มาจาก `iosApp.entitlements`)
4. ☐ Product → Archive → อัปโหลด TestFlight

---

## 5. App Store Connect

- ☐ สมัคร **App Store Small Business Program** (ค่าธรรมเนียม 15%) และกรอก Agreements/Tax/Banking ให้เสร็จก่อนสร้างสินค้า
- ☐ Age rating **12+** (Frequent/Intense: ไม่มี · Gambling: ไม่มี — เป็นการทำนายเพื่อความบันเทิง)
- ☐ App Privacy (Nutrition Label): Contact info (email จาก Sign in with Apple, linked), User Content (คำถาม/บันทึก, linked, App Functionality), Identifiers (User ID, Device ID — linked; Device ID สำหรับ Third-party Advertising เมื่ออนุญาต ATT), Purchases, Usage Data, Diagnostics (Crashlytics) · Tracking: Yes (AdMob เมื่อผู้ใช้อนุญาต)
- ☐ Privacy Policy URL: `https://<project-id>.web.app/privacy`
- ☐ ตั้ง subscription group + ข้อความแสดงในหน้า paywall ตรงกับราคาใน store
- ☐ **Review notes** (กันโดน 4.3 spam — เน้นจุดต่าง):
  > แอปนี้ไม่ได้สุ่มข้อความ แต่ใช้การโยนเหรียญสามเหรียญตามตำราอี้จิงดั้งเดิมเพื่อได้กวา (64 กวา 384 เส้น)
  > แล้วใช้ RAG อ้างอิงตัวบทคัมภีร์จริงของกวานั้นให้ AI ตีความ พร้อมคลังคัมภีร์ 64 กวาแบบออฟไลน์ (แท็บ "คัมภีร์")
  > และบันทึกชะตาที่ผู้ใช้จดว่าคำทำนาย "เกิดขึ้นจริงไหม" เนื้อหา AI ติดป้ายชัดเจนและมีปุ่มรายงาน
  > คำถามเกี่ยวกับการทำร้ายตนเองจะได้รับข้อมูลสายด่วน 1323 แทนคำทำนาย
  > ทดสอบ: ใช้งานได้ทันทีโดยไม่ต้องสมัครบัญชี (anonymous) · ทดสอบการซื้อด้วย Sandbox account
- ☐ Screenshot 6.7"/6.5" + คำอธิบายภาษาไทย คำค้น: อี้จิง, เสี่ยงทาย, ดูดวง, เซียมซี, I Ching, คัมภีร์, ทำนาย

## 6. Google Play Console

- ☐ Data safety: เก็บ User IDs, Device IDs, App interactions, Crash logs, Purchase history, User-generated content (คำถาม) · เข้ารหัสระหว่างส่ง: Yes · ผู้ใช้ขอลบได้: Yes → URL `https://<project-id>.web.app/delete-account`
- ☐ Content rating (IARC) → ได้ Teen · Target audience 13+
- ☐ ประกาศ **Ads: Yes** และ **AI-generated content** (นโยบาย Generative AI: มีปุ่มรายงานในแอปแล้ว)
- ☐ บัญชีส่วนตัวใหม่: ต้องทำ **Closed testing** กับผู้ทดสอบตามจำนวน/ระยะเวลาที่ Play Console กำหนด (เช็กตัวเลขล่าสุดในคอนโซล) ก่อนขอ Production
- ☐ ลงทะเบียน 15% service fee tier (สำหรับรายได้ 1M USD แรก)
- ☐ License testing: เพิ่มอีเมลผู้ทดสอบเพื่อทดสอบการซื้อ

---

## 7. ทดสอบก่อนส่ง (เกณฑ์ spec: ซื้อ/restore ผ่าน sandbox ทั้ง 2 แพลตฟอร์ม, crash-free ≥ 99%)

- ☐ เปิดครั้งแรก → หน้า disclaimer → เสี่ยงทาย 6 ครั้ง → คำทำนายแบบ streaming + ป้าย "สร้างโดย AI"
- ☐ เสี่ยงทายครั้งที่ 2 ในวันเดียวกัน → ได้ตัวบท + ปุ่มชมโฆษณา → ชมจบ → ได้คำทำนาย AI ของกวาเดิม (ทำได้ 2 ครั้ง/วัน)
- ☐ ถามว่า "ไม่อยากมีชีวิตอยู่แล้ว" → หน้าสายด่วน 1323 ไม่มีการทำนาย
- ☐ ซื้อรายเดือน (sandbox) → Premium ทันที → ถามต่อได้ 3 ครั้ง → โฆษณาหาย
- ☐ ลบแอป ติดตั้งใหม่ → ผูก Apple/Google → กู้คืนการซื้อ → ดึงบันทึกชะตาจากระบบ
- ☐ ซื้อเหรียญ → จำนวนเหรียญขึ้นในหน้าตั้งค่า → ใช้หมดแล้วกลับเป็นตัวบท
- ☐ โหมดเครื่องบิน → ยังเสี่ยงทายได้ (ตัวบทจากคัมภีร์) · Library ใช้ได้ออฟไลน์
- ☐ ลบบัญชี → ข้อมูลหายทั้งในเครื่องและ Firestore
- ☐ Firestore `config/runtime.aiEnabled=false` → ทุกคนได้ตัวบท ไม่มี error

## 8. สิ่งที่ยังไม่ได้ทำ (ตั้งใจเว้นไว้ตามขอบเขต MVP)
- v1.1: กว้าประจำวัน + push 07:00, การ์ดแชร์, streak, TTS (หมวดคำถามทำแล้วในหน้าแรก)
- schema กวา: `name_en`, `daily_message` (ใช้ตอนทำ v1.1/v2)
- context caching ของ Gemini (prompt ยังสั้นกว่าขั้นต่ำที่ caching คุ้ม)
- Server-side verification (SSV) ของ AdMob — ตอนนี้ server เชื่อสัญญาณจากแอปที่ผ่าน App Check และจำกัด 2 ครั้ง/วัน/เครื่อง ถ้าเห็นการโกงค่อยเพิ่ม
