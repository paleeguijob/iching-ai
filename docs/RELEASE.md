# คู่มือขึ้น Store — อี้จิง AI v1.0 (MVP), v1.1 (หัวข้อ 11) และ v1.2 (หัวข้อ 12)

**ลำดับการปล่อย: Android ก่อน** · iOS ทำภายหลังเมื่อสมัคร Apple Developer แล้ว (รวมไว้ที่หัวข้อ 9)

โค้ด backend และแอป Android พร้อมแล้ว เหลือขั้นตอนที่ต้องใช้บัญชีของคุณเอง ทำตามลำดับด้านล่าง
(☐ = ต้องทำเอง · ☑ = ทำแล้ว · ค่าที่ต้องกรอกอยู่ใน `frontend/local.properties` และ Firebase secrets)

โครงสร้าง: **`frontend/`** = แอป (เปิดใน Android Studio) · **`backend/`** = Firebase (รันคำสั่ง `firebase` จากโฟลเดอร์นี้)

---

## 0. สิ่งที่เปลี่ยนจากเวอร์ชันก่อน (สรุปสำหรับรีวิวโค้ด)

| ด้าน | เดิม | ตอนนี้ |
|---|---|---|
| เรียก AI | แอปเรียก Gemini ตรง ฝัง API key ในแอป | ผ่าน Cloud Function `api` เท่านั้น key อยู่ใน Firebase Secret |
| สุ่มกวา | ในเครื่อง | ฝั่ง server (crypto RNG, 3 เหรียญ = 1/8, 3/8, 3/8, 1/8) แอปเล่นแอนิเมชันตามผล · ออฟไลน์ยังสุ่มในเครื่องได้แต่ได้แค่ตัวบท |
| โควตา | ไม่มี | Free ไม่มีสิทธิ์ฟรีรายวัน (ตั้งแต่ 6 พ.ย. 2026) · โฆษณาแลก 2/วัน (`gemini-3.1-flash-lite`) · Premium 10/วัน (`gemini-3.5-flash-lite`) · เหรียญ 1 = 1 คำทำนาย · จำกัดต่อเครื่อง · rate limit 8 วิ · Gemini error = คืนโควตา |
| บัญชี | ไม่มี | Firebase Anonymous → ผูก Google (Android) / Sign in with Apple (iOS) · ลบบัญชีในแอป |
| เงิน | ไม่มี | RevenueCat (รายเดือน 49 / รายปี 390 + ทดลอง 7 วัน / เหรียญ 29 บาท 10 เหรียญ) + AdMob rewarded |
| ถามต่อ | ไม่มี | Premium ถามต่อ 3 รอบ/คำทำนาย (ส่งสรุป ไม่ส่ง history เต็ม) |
| Journal | เก็บในเครื่อง | Free เห็น 7 วันล่าสุด · Premium ไม่จำกัด + ดึงจาก server ข้ามเครื่อง · ปุ่ม "เกิดขึ้นจริงไหม" + บันทึกส่วนตัว |
| Safety | ไม่มี | คำถามทำร้ายตนเอง/ฉุกเฉินทางการแพทย์ → ข้อความคงที่ + 1323/1669 ไม่ทำนาย ไม่ตัดโควตา |
| Store compliance | ไม่มี | Onboarding disclaimer, ป้าย "สร้างโดย AI", ปุ่มรายงานคำตอบ, Restore/Manage subscription, เงื่อนไขต่ออายุบน paywall, privacy/terms/delete-account บน Firebase Hosting, ATT + UMP consent |
| Persona | "ห้ามบอกว่าเป็น AI" | ไม่พูดถึงเทคโนโลยีในคำทำนาย แต่ถ้าถามตรง ๆ ต้องยอมรับ |

ไฟล์สำคัญ: `backend/functions/src/core/*` (logic ทั้งหมดของ server + เทสต์) · `frontend/composeApp/.../session/AppSession.kt` ·
`frontend/composeApp/.../feature/divination/DivinationViewModel.kt` · `frontend/composeApp/.../platform/NativeBridges.kt` (สัญญากับโค้ด native) ·
`frontend/androidApp/.../platform/*` · `frontend/iosApp/iosApp/Bridges/*`

### ค่าใช้จ่าย (เช็กล่าสุด 31 ต.ค. 2026, คิด 1 USD ≈ 35 บาท)

| รายการ | ค่าใช้จ่าย |
|---|---|
| Google Play | $25 จ่ายครั้งเดียว |
| ค่าธรรมเนียม Play | 15% ของรายได้ 1M USD แรก |
| RevenueCat | ฟรีถึงรายได้ $2,500/เดือน จากนั้น 1% |
| Gemini paid tier | 3.1 Flash-Lite ~0.06 บาท/คำทำนาย · 3.5 Flash-Lite ~0.09 บาท/คำทำนาย (input 2,000 + output 800 tokens) · รุ่น 2.5 ไม่เปิดให้ผู้ใช้ใหม่แล้ว |
| Firebase Blaze | จ่ายตามใช้ มีโควตาฟรี |
| Apple Developer (ตอนทำ iOS) | $99/ปี |

---

## 1. Firebase

1. ☐ อัปเกรดเป็นแผน **Blaze** (Cloud Functions + Secret Manager ต้องใช้)
2. ☑ `backend/.firebaserc` ใส่ project id แล้ว (`iching-2001a`)
3. ☐ Authentication → Sign-in method: เปิด **Anonymous** และ **Google**
4. ☑ ลงทะเบียนแอป Android (`com.iching.ai`) และวาง `google-services.json` แล้ว
   - ☐ ใส่ SHA-1 และ SHA-256 ของ debug key ให้ครบ และเพิ่ม **App signing key ของ Play** หลังอัปโหลดครั้งแรก (หัวข้อ 4) แล้วดาวน์โหลด `google-services.json` ใหม่
5. ☐ App Check: Android = **Play Integrity** · เพิ่ม debug token จาก Logcat ของ debug build ใน "Manage debug tokens"
6. ☐ Firestore: สร้างฐานข้อมูล (region `asia-southeast1`)
7. ☐ เปิด billing ให้โปรเจกต์ของ Gemini key ใน AI Studio ก่อนเปิดจริง: free tier มี rate limit และ Google นำเนื้อหาไปปรับปรุงผลิตภัณฑ์ได้ ส่วน paid tier ไม่นำไปใช้
8. ☐ ตั้ง **Budget alert** ที่ 50/90/100% ใน Google Cloud Billing **ของโปรเจกต์ที่เป็นเจ้าของ Gemini key** (อาจไม่ใช่โปรเจกต์เดียวกับ Firebase) · budget แค่แจ้งเตือน ไม่ได้หยุดการใช้จ่าย ต้องใช้ kill-switch ด้านล่างเอง
9. ☐ ตั้ง secrets แล้ว deploy:
   ```bash
   npm i -g firebase-tools && firebase login
   cd backend/functions && npm install && npm test   # เทสต์ต้องผ่านทั้งหมด
   npm run test:emulator   # Firestore จริงบน emulator ในเครื่อง: เหรียญ, การรวมบัญชี, security rules (ต้องมี Java)
   firebase functions:secrets:set GEMINI_API_KEY          # key จาก aistudio.google.com
   firebase functions:secrets:set REVENUECAT_API_KEY      # sk_... (ยังไม่มีใส่ unset ไปก่อน)
   firebase functions:secrets:set REVENUECAT_WEBHOOK_AUTH # สุ่มยาว ๆ เช่น openssl rand -hex 32
   cd .. && firebase deploy --only functions,firestore:rules,hosting   # รันใน backend/
   ```
10. ☐ ใส่ URL ใน `frontend/local.properties`:
    ```properties
    ICHING_API_BASE_URL=https://asia-southeast1-<project-id>.cloudfunctions.net/api
    ICHING_WEB_BASE_URL=https://<project-id>.web.app
    ```
11. ☐ แก้ `backend/hosting/public/*.html`: แทน `SUPPORT_EMAIL`, `DEVELOPER_NAME`, `EFFECTIVE_DATE` แล้ว deploy hosting อีกรอบ

**Kill-switch / ปรับโควตาโดยไม่ต้อง deploy:** สร้างเอกสาร Firestore `config/runtime` ใส่ฟิลด์ที่ต้องการ override เช่น
`aiEnabled: false` (ปิด AI ทุกคน → ได้ตัวบทแทน), `forceModel: "gemini-3.1-flash-lite"` (สลับทุกคนไปรุ่นถูก),
`freeDailyReadings` (ค่าเริ่มต้น 0 = ไม่มีคำทำนายฟรี), `premiumDailyReadings`, `adRewardsPerDay`, `enforceAppCheck` — มีผลภายใน 60 วินาที (ดูค่าทั้งหมดใน `backend/functions/src/core/quota.ts`)

**รุ่น Gemini:** ห้ามใช้ alias `-latest` เพราะอาจถูกเลื่อนไปรุ่นที่แพงกว่าโดยไม่ต้องแก้โค้ด (เช่น 3.8 Flash $0.75/$3.75 ถึงสิ้นปี 2026 และ $1.50/$7.50 ตั้งแต่ 1 ม.ค. 2027)
รุ่น 2.5 ยังไม่มีวันปิดบริการ แต่ Google ไม่ได้จัดเป็นรุ่น stable จึงต้องคอยดูประกาศและหน้า deprecations · ก่อนเปลี่ยนรุ่นให้ทดสอบคุณภาพภาษาไทยและคิดต้นทุนใหม่เทียบ KPI ≤ 0.10 บาท/คำทำนาย

**ดูต้นทุนจริง:** Cloud Logging กรอง `jsonPayload.message="reading_cost"` (มี tokensIn/tokensOut/costThb ต่อคำขอ) → สร้าง Log sink ไป BigQuery ถ้าต้องการรายงาน
ราคาใน `PRICES_USD_PER_M` (`backend/functions/src/core/gemini.ts`) ตรงกับหน้า pricing ของ Google ณ วันที่ด้านบน · ถ้าเปลี่ยนรุ่นต้องเพิ่มราคาของรุ่นนั้นด้วย ไม่งั้น log จะคิดเป็นราคา 2.5 Flash

---

## 2. จำลองการจ่ายใน debug (RevenueCat Test Store)

ทำได้ทันที ไม่ต้องมีบัญชี Google Play · ตอนซื้อจะมีหน้าต่างให้กดจำลองว่าสำเร็จ ล้มเหลว หรือยกเลิก

1. ☐ สร้างโปรเจกต์ RevenueCat → Apps and providers → Test configuration → สร้าง **Test Store** เพื่อรับ key `test_...`
2. ☐ Product catalog: สร้างสินค้า Test Store โดยใช้ id ตรงตามนี้ (backend เทียบ id ของเหรียญแบบตรงตัว)
   - `iching_premium_monthly` · `iching_premium_yearly` · `iching_coins_10`
3. ☐ Entitlement ชื่อ **`iching_premium`** ผูกกับ 2 subscription (backend เทียบชื่อนี้แบบตรงตัว)
4. ☐ Offering ที่เป็น **Current**: package Monthly, Annual และ custom package สำหรับ `iching_coins_10`
5. ☐ ใส่ test key ใน `frontend/local.properties` (ใช้เฉพาะ debug build):
   ```properties
   REVENUECAT_ANDROID_TEST_KEY=test_...
   ```
6. ☐ ตั้ง `REVENUECAT_API_KEY` เป็น secret key v1 (`sk_...`) ของโปรเจกต์เดียวกัน แล้ว `firebase deploy --only functions` (ถ้าไม่มี `/purchases/sync` จะตอบ 503 และ Premium จะไม่ติด)

**ข้อควรรู้**
- **ห้ามใส่ test key ใน `REVENUECAT_ANDROID_KEY`** — RevenueCat จะตั้งใจ crash ใน release build รวมถึง Play test track ทุกแบบ · Gradle ถูกตั้งให้ build ไม่ผ่านถ้าพลาด
- สมาชิกรายเดือนหมดอายุใน 25 นาที รายปีใน 5 ชั่วโมง (ต่ออายุเองสูงสุด 5 รอบแล้วยกเลิก) ใช้ทดสอบการหมดอายุได้
- แก้ราคา/ระยะเวลาสินค้าเดิมไม่ได้ ต้องสร้างใหม่แล้วสลับใน offering
- เอกสารไม่ได้ระบุว่ารองรับ consumable, ช่วงทดลองฟรี, webhook และ REST v1 กับ Test Store หรือไม่ — ต้องลองจริง ถ้าไม่ได้ให้ทดสอบส่วนนั้นด้วย Play license testing (หัวข้อ 4)

---

## 3. Build Android

1. ☐ เปิดโฟลเดอร์ **`frontend/`** ใน Android Studio → Sync (ถ้า plugin/SDK เวอร์ชันใน `gradle/libs.versions.toml` ไม่ตรงกับที่มี ให้กด update ตามที่ IDE แนะนำ)
2. ☐ `./gradlew :core:domain:jvmTest :core:data:testAndroid` แล้วรันบนเครื่องจริง
3. ☐ สร้าง upload keystore แล้วเขียน `frontend/keystore.properties` (**สำรองไฟล์ .jks และรหัสผ่านไว้นอกเครื่อง**):
   ```properties
   storeFile=release.jks
   storePassword=...
   keyAlias=upload
   keyPassword=...
   ```
4. ☐ `./gradlew :androidApp:bundleRelease` → ได้ `.aab` (เพิ่ม `versionCode` ใน `frontend/androidApp/build.gradle.kts` ทุกครั้งที่อัปโหลด)
   - release เปิด R8 และ resource shrinking แล้ว (rules อยู่ที่ `frontend/androidApp/proguard-rules.pro`) · ตอน build จะอัปโหลด mapping ให้ Crashlytics เอง จึงต้องต่อเน็ต · เก็บ `androidApp/build/outputs/mapping/release/mapping.txt` ของทุกเวอร์ชันที่ปล่อยไว้ด้วย

---

## 4. Google Play Console และ RevenueCat ของจริง

**ต้องอัปโหลด `.aab` ก่อน Play จึงจะให้สร้างสินค้าได้** จึงต้องทำตามลำดับนี้

1. ☐ สมัครบัญชีนักพัฒนา ($25) → สร้างแอป → กรอก payments profile
2. ☐ อัปโหลด `.aab` ขึ้น **Internal testing**
3. ☐ นำ SHA ของ **App signing key** (Setup → App signing) ไปเพิ่มใน Firebase แล้วใส่ `google-services.json` ใหม่ ไม่งั้น Google Sign-In จะใช้ไม่ได้ในเวอร์ชันที่ลงจาก Play
4. ☐ สร้างสินค้าใน Play:
   - `iching_premium_monthly` — subscription + base plan รายเดือน 49 บาท
   - `iching_premium_yearly` — subscription + base plan รายปี 390 บาท + **offer ทดลองฟรี 7 วัน**
   - `iching_coins_10` — in-app product 29 บาท
5. ☐ RevenueCat → เพิ่มแอป Play Store → อัปโหลด **service account JSON** ที่มีสิทธิ์ใน Play Console (สิทธิ์อาจใช้เวลาสักพักกว่าจะมีผล) และตั้ง Google Real-time developer notifications ตามคู่มือของ RevenueCat
6. ☐ Import สินค้า → ตั้ง `iching_coins_10` เป็น **consumable** → เพิ่มเข้า entitlement `iching_premium` (2 subscription) และ offering เดียวกับหัวข้อ 2
7. ☐ Integrations → Webhooks: URL `https://asia-southeast1-<project-id>.cloudfunctions.net/revenuecatWebhook`, Authorization header = `Bearer <REVENUECAT_WEBHOOK_AUTH>`
8. ☐ ใส่ public SDK key (ใช้ใน release และใน debug ถ้าไม่ได้ตั้ง test key):
   ```properties
   # frontend/local.properties
   REVENUECAT_ANDROID_KEY=goog_...
   ```
9. ☐ License testing: เพิ่มอีเมลผู้ทดสอบเพื่อทดสอบการซื้อจริงแบบไม่เสียเงิน

---

## 5. AdMob

1. ☐ สร้างแอป Android และ ad unit แบบ **Rewarded**
2. ☐ ใส่ค่าใน `frontend/local.properties` (ค่าเริ่มต้นคือ test id ของ Google — **ห้ามปล่อย production ด้วย test id**):
   ```properties
   ADMOB_APP_ID_ANDROID=ca-app-pub-...~...
   ADMOB_REWARDED_ANDROID=ca-app-pub-.../...
   ```
3. ☐ Privacy & messaging: สร้างข้อความ GDPR (UMP) — แอปเรียกฟอร์มให้อัตโนมัติเฉพาะผู้ใช้ในยุโรป
4. ☐ เพิ่ม `app-ads.txt` บนเว็บไซต์ผู้พัฒนา (ใส่ใน `backend/hosting/public/app-ads.txt` ได้)

---

## 6. ข้อมูลแอปใน Play Console

- ☐ Data safety: เก็บ User IDs, Device IDs, App interactions, Crash logs, Purchase history, User-generated content (คำถาม) · เข้ารหัสระหว่างส่ง: Yes · ผู้ใช้ขอลบได้: Yes → URL `https://<project-id>.web.app/delete-account`
- ☐ Privacy Policy URL: `https://<project-id>.web.app/privacy`
- ☐ Content rating (IARC) → ได้ Teen · Target audience 13+
- ☐ ประกาศ **Ads: Yes** และ **AI-generated content** (นโยบาย Generative AI: มีปุ่มรายงานในแอปแล้ว)
- ☐ ตรวจว่าอยู่ใน 15% service fee tier (สำหรับรายได้ 1M USD แรก)
- ☐ Screenshot + คำอธิบายภาษาไทย คำค้น: อี้จิง, เสี่ยงทาย, ดูดวง, เซียมซี, I Ching, คัมภีร์, ทำนาย

---

## 7. ทดสอบก่อนส่ง (เกณฑ์ spec: ซื้อ/restore ผ่าน sandbox, crash-free ≥ 99%)

**Debug + Test Store**
- ☐ ซื้อรายเดือน → Premium ทันที → ถามต่อได้ 3 ครั้ง → โฆษณาหาย → รอครบ 25 นาที → กลับเป็น Free
- ☐ กดจำลองล้มเหลวและยกเลิก → ไม่ได้ Premium และไม่มี error ค้าง
- ☐ ซื้อเหรียญ → จำนวนเหรียญขึ้นในหน้าตั้งค่า → ใช้หมดแล้วกลับเป็นตัวบท

**Release จาก Internal testing + บัญชี License testing**
- ☐ เปิดครั้งแรก → หน้า disclaimer → หน้าแรกบอกว่าต้องชมโฆษณาก่อนเสี่ยงทาย (ไม่มีคำว่า "ฟรี")
- ☐ บัญชี Free ไม่มีเหรียญ → ชมโฆษณาจบ → เสี่ยงทาย 6 ครั้ง → คำทำนายแบบ streaming + ป้าย "สร้างโดย AI" (ทำได้ 2 ครั้ง/วัน)
- ☐ บัญชี Free ข้ามโฆษณาหรือชมครบ 2 ครั้งแล้ว → ได้ตัวบท + การ์ดเสนอ Premium/เหรียญ ไม่มีคำทำนาย AI ฟรี
- ☐ ถามว่า "ไม่อยากมีชีวิตอยู่แล้ว" → หน้าสายด่วน 1323 ไม่มีการทำนาย
- ☐ ซื้อรายเดือน รายปี และเหรียญผ่าน Play → webhook ทำงาน (ดู log `revenuecat_event`)
- ☐ ลบแอป ติดตั้งใหม่ → ผูก Google → กู้คืนการซื้อ → ดึงบันทึกชะตาจากระบบ
- ☐ ติดตั้งใหม่ → ซื้อ Premium และเหรียญก่อนล็อกอิน → ล็อกอิน Google บัญชีที่มีอยู่แล้ว → Premium ติด เหรียญรวมกับของเดิม บันทึกชะตาเปิดต่อได้ (log `account_merged`) · RevenueCat → Project settings → Restore behavior ต้องเป็น **Transfer to new App User ID**
- ☐ จ่ายด้วยบัตรทดสอบแบบ slow card → ขึ้น "รอการชำระเงิน" ไม่ใช่ "ไม่สำเร็จ" → เมื่อผ่านแล้วสิทธิ์เข้าเอง
- ☐ คืนเงินค่าเหรียญใน Play Console → เหรียญถูกหัก 10 (ไม่ต่ำกว่า 0) และ log `revenuecat_event` เป็น `CANCELLATION` · ตรวจว่า `transaction_id` ใน event ตรงกับ `store_transaction_id` ของ subscriber
- ☐ ต่ออายุไม่ผ่าน (บัตรทดสอบที่ถูกปฏิเสธ) → ดูว่า Premium ยังอยู่ระหว่าง grace period หรือไม่
- ☐ โหมดเครื่องบิน → ยังเสี่ยงทายได้ (ตัวบทจากคัมภีร์) · Library ใช้ได้ออฟไลน์
- ☐ ลบบัญชี → ข้อมูลหายทั้งในเครื่องและ Firestore
- ☐ Firestore `config/runtime.aiEnabled=false` → ทุกคนได้ตัวบท ไม่มี error
- ☐ `reading_cost` ใน log แสดงรุ่น `gemini-3.1-flash-lite` หรือ `gemini-3.5-flash-lite` และต้นทุนไม่เกินค่าในหัวข้อค่าใช้จ่าย

---

## 8. Closed testing และขอ Production

บัญชีส่วนตัวที่สร้างหลัง 13 พ.ย. 2023 ต้องผ่านขั้นนี้ก่อน (เช็กเงื่อนไขล่าสุดใน Play Console อีกครั้ง)

1. ☐ ปล่อยขึ้น **Closed testing** ด้วย build ที่ใช้ `goog_` key และ AdMob id จริง
2. ☐ มีผู้ทดสอบ **อย่างน้อย 12 คน opt-in ต่อเนื่อง 14 วัน** — ถ้าออกแล้วกลับเข้ามาจะนับใหม่ · ควรหาเผื่อไว้และให้ใช้งานจริง
3. ☐ ขอ Production access (ปกติรีวิวไม่เกิน 7 วัน) → ส่ง Production

**เผื่อเวลาขั้นนี้อย่างน้อยราว 3 สัปดาห์**

---

## 9. iOS (ทำภายหลัง เมื่อสมัคร Apple Developer $99/ปี แล้ว)

**ต้องแก้โค้ดก่อน**
- ☐ **ถอนสิทธิ์ Sign in with Apple ตอนลบบัญชี** — ตอนนี้ยังไม่มีโค้ด revoke และ Apple กำหนดให้ทำ
- ☐ **Push กวาประจำวัน (v1.1)** — อัปโหลด APNs key ใน Firebase, เพิ่ม `FirebaseMessaging` ใน Xcode และทำ `IosNotifications` ใน `Bridges/IosAdsAndAnalytics.swift` ให้ทำงานจริง (ตอนนี้ `isSupported()` คืน `false` แอปจึงซ่อนสวิตช์) แล้วเรียก `PendingNavigation.requestDaily()` เมื่อแตะแจ้งเตือน
- ☐ **สี status bar ของธีมมืด (v1.1)** — `SystemBars.ios.kt` ยังไม่ทำงาน ตัวอักษร status bar จะเป็นสีเข้มบนพื้นเข้มในธีมราตรีหมึก
- ☐ แยก RevenueCat test key ของ debug ออกจาก release แบบเดียวกับ Android และตรวจว่า SDK เป็น 5.43.0 ขึ้นไป

**Firebase**
- ☐ Authentication: เปิด **Apple**
- ☐ ลงทะเบียนแอป iOS (bundle id เดียวกับใน Xcode) → วาง `GoogleService-Info.plist` ที่ `frontend/iosApp/iosApp/`
- ☐ App Check: **App Attest**

**RevenueCat**
- ☐ เพิ่มแอป App Store + อัปโหลด In-App Purchase Key จาก App Store Connect
- ☐ สร้างสินค้าด้วย id เดียวกับ Android แล้วผูกกับ entitlement/offering เดิม
- ☐ สร้าง `frontend/iosApp/Configuration/Secrets.xcconfig` → `REVENUECAT_IOS_KEY = appl_...`

**AdMob**
- ☐ สร้างแอป iOS + ad unit Rewarded → ใน `Secrets.xcconfig` ใส่ `ADMOB_APP_ID_IOS = ...`, `ADMOB_REWARDED_IOS = ...`
- ☐ อัปเดตรายการ `SKAdNetworkItems` ใน `Info.plist` ให้ครบตามเอกสาร AdMob ล่าสุด (ตอนนี้ใส่ของ Google ไว้ 1 รายการ)

**Build**
1. ☐ เปิด `frontend/iosApp/iosApp.xcodeproj` → Xcode จะดึง Swift packages (Firebase 12, RevenueCat 5, Google Mobile Ads 12, UMP 3) ให้เอง
   — ถ้าดึงแล้วเป็นเวอร์ชันหลักที่ใหม่กว่า และชื่อ API ใน `frontend/iosApp/iosApp/Bridges/IosAdsAndAnalytics.swift` ไม่ตรง ให้ส่ง error มา
2. ☐ ใส่ `TEAM_ID` ใน `frontend/iosApp/Configuration/Config.xcconfig` (bundle id จะเป็น `com.iching.ai<TEAM_ID>` ตามเทมเพลตเดิม — ถ้าต้องการ `com.iching.ai` ตรง ๆ ให้แก้ `PRODUCT_BUNDLE_IDENTIFIER`)
3. ☐ Signing & Capabilities: ยืนยันว่ามี **Sign in with Apple** และ **App Attest** (มาจาก `iosApp.entitlements`)
4. ☐ Product → Archive → อัปโหลด TestFlight (ห้ามใช้ test key เพราะ TestFlight นับเป็น release build)

**App Store Connect**
- ☐ สมัคร **App Store Small Business Program** (ค่าธรรมเนียม 15%) และกรอก Agreements/Tax/Banking ให้เสร็จก่อนสร้างสินค้า
- ☐ Age rating **12+** (Frequent/Intense: ไม่มี · Gambling: ไม่มี — เป็นการทำนายเพื่อความบันเทิง)
- ☐ App Privacy (Nutrition Label): Contact info (email จาก Sign in with Apple, linked), User Content (คำถาม/บันทึก, linked, App Functionality), Identifiers (User ID, Device ID — linked; Device ID สำหรับ Third-party Advertising เมื่ออนุญาต ATT), Purchases, Usage Data, Diagnostics (Crashlytics) · Tracking: Yes (AdMob เมื่อผู้ใช้อนุญาต)
- ☐ Privacy Policy URL: `https://<project-id>.web.app/privacy` · ใส่ลิงก์ Terms/EULA ในข้อมูลแอปด้วยเพราะมี subscription
- ☐ ตั้ง subscription group + ข้อความแสดงในหน้า paywall ตรงกับราคาใน store · ใส่ screenshot สำหรับรีวิวให้สินค้าแต่ละตัว และส่ง subscription ชุดแรกพร้อมแอปเวอร์ชันนั้น
- ☐ **Review notes** (กันโดน 4.3 spam — เน้นจุดต่าง):
  > แอปนี้ไม่ได้สุ่มข้อความ แต่ใช้การโยนเหรียญสามเหรียญตามตำราอี้จิงดั้งเดิมเพื่อได้กวา (64 กวา 384 เส้น)
  > แล้วใช้ RAG อ้างอิงตัวบทคัมภีร์จริงของกวานั้นให้ AI ตีความ พร้อมคลังคัมภีร์ 64 กวาแบบออฟไลน์ (แท็บ "คัมภีร์")
  > และบันทึกชะตาที่ผู้ใช้จดว่าคำทำนาย "เกิดขึ้นจริงไหม" เนื้อหา AI ติดป้ายชัดเจนและมีปุ่มรายงาน
  > คำถามเกี่ยวกับการทำร้ายตนเองจะได้รับข้อมูลสายด่วน 1323 แทนคำทำนาย
  > ทดสอบ: ใช้งานได้ทันทีโดยไม่ต้องสมัครบัญชี (anonymous) · ทดสอบการซื้อด้วย Sandbox account
- ☐ Screenshot 6.7"/6.5" + คำอธิบายภาษาไทย คำค้น: อี้จิง, เสี่ยงทาย, ดูดวง, เซียมซี, I Ching, คัมภีร์, ทำนาย
- ☐ ทดสอบซ้ำตามหัวข้อ 7 บน iOS: ซื้อ/restore ผ่าน Sandbox, ผูก Sign in with Apple, ลบบัญชีแล้วสิทธิ์ Apple ถูกถอน

---

## 10. สิ่งที่ยังไม่ได้ทำ (ตั้งใจเว้นไว้)
- ปลดล็อกเสียงจาก streak — ต้องมีไฟล์เสียงที่ใช้ได้ตามลิขสิทธิ์ก่อน
- schema กวา: `name_en` (ใช้ตอนทำ v2)
- context caching ของ Gemini (prompt ยังสั้นกว่าขั้นต่ำที่ caching คุ้ม)
- Server-side verification (SSV) ของ AdMob — ตอนนี้ server เชื่อสัญญาณจากแอปที่ผ่าน App Check และจำกัด 2 ครั้ง/วัน/เครื่อง ถ้าเห็นการโกงค่อยเพิ่ม

---

## 11. ปล่อย v1.1 (หลัง v1.0 ขึ้น store แล้ว)

ฟีเจอร์: กวาประจำวัน + push 07:00, การ์ดแชร์, streak + ธีม, เสียงอ่าน (Premium) · `versionCode` 110 เว้นเลข 3–109 ไว้ให้ v1.0 ออก patch

**ค่าใช้จ่ายเพิ่ม:** FCM ฟรี · Cloud Functions ใช้ราว 30 ครั้ง/เดือน อยู่ในโควตาฟรี · Cloud Scheduler 1 งาน อยู่ในโควตาฟรี 3 งานต่อ billing account (เกินคิดราว $0.10/งาน/เดือน — เช็กราคาล่าสุดใน Google Cloud)

1. ☐ อ่านและแก้ข้อความใน [`docs/content/daily-messages.md`](content/daily-messages.md) (Claude ร่าง ต้องให้คนตรวจตามสเปก) · ถ้าแก้ ให้แก้ที่ `hexagrams.json` ฟิลด์ `dailyMessage` ไม่เกิน 120 ตัวอักษร
2. ☐ Deploy: รันใน `backend/` → `firebase deploy --only functions,hosting` (การ deploy ครั้งแรกจะสร้างงาน Cloud Scheduler `dailyPush` ให้เอง ต้องเปิด Cloud Scheduler API ถ้า CLI ถาม)
3. ☐ ตรวจว่า `dailyPush` อยู่ใน Cloud Scheduler เวลา `0 7 * * *` เขต `Asia/Bangkok` · log ที่ควรเห็นทุกเช้า: `daily_push_sent`
4. ☐ ปิดฉุกเฉินได้ด้วย Firestore `config/runtime.dailyPushEnabled = false`
5. ☐ Play Console → Data safety: ทบทวนรายการ Device IDs เพราะเพิ่ม FCM registration token (ใช้เพื่อ App functionality และเก็บเมื่อผู้ใช้เปิดรับแจ้งเตือนเท่านั้น)
6. ☐ เพิ่มหัวข้อ "แจ้งเตือน" ในคำอธิบายแอป และประกาศสิทธิ์ `POST_NOTIFICATIONS` ถ้า Play ถาม

**ทดสอบบน Android**
- ☐ Firebase Console → Messaging → ส่งไปที่ topic `daily-hexagram-th` พร้อม custom data `type=daily` → แอปปิดอยู่ แตะแล้วเปิดหน้ากวาประจำวัน
- ☐ ปฏิเสธสิทธิ์แจ้งเตือนแล้วเปิดสวิตช์ → มีปุ่มพาไปหน้าตั้งค่าของระบบ · กลับมาแล้วสวิตช์ตรงกับสิทธิ์จริง
- ☐ แชร์การ์ดไป LINE และ IG Story → ภาพครบ 1080×1920 ไม่ถูกตัด · การ์ดจากคำทำนายมีป้าย AI และไม่มีคำถามของผู้ใช้
- ☐ คำทำนาย AI 10–20 ครั้งหลัง deploy → ไม่เห็นบรรทัด `[สรุป]` ในคำทำนาย · สรุปบนการ์ดอ่านรู้เรื่องและไม่เอ่ยเรื่องส่วนตัว · `reading_cost` ยังไม่เกิน KPI 0.10 บาท
- ☐ เปลี่ยนวันที่เครื่องทีละวัน → streak เพิ่ม, ถึง 7 วันปลดล็อกไผ่เขียว, ข้ามวันแล้วเริ่มนับใหม่แต่ธีมยังอยู่, ย้อนวันแล้วไม่เปลี่ยน
- ☐ เปลี่ยนทุกธีมแล้วไล่ดูทุกหน้า โดยเฉพาะราตรีหมึก
- ☐ Premium ฟังคำทำนายจนจบและกดหยุด · Free กดแล้วไป paywall · เครื่องที่ไม่มีเสียงไทยได้ข้อความพาไปหน้าตั้งค่า · ออกจากแอปแล้วเสียงหยุด
- ☐ ลบบัญชี → ธีมกลับเป็นค่าเริ่มต้น streak เป็นศูนย์ และไม่ได้รับแจ้งเตือนอีก
- ☐ เช้าถัดจาก deploy: ได้รับแจ้งเตือนเวลา 07:00 จริง และข้อความตรงกับกวาในแอป

**ดูผล KPI ใน Firebase Analytics:** อัตราแชร์ = `share_card` (source=reading) ÷ `reading_completed` · D7 retention · `daily_view`, `streak_milestone`, `theme_selected`, `tts_play`, `push_toggle`, `push_open`

## 12. ปล่อย v1.2 — แคมเปญตรุษจีน ปีมะแม ๒๕๗๐

ฟีเจอร์: การ์ดแคมเปญบนหน้าแรก, เสี่ยงทายกวาประจำปี (ฟรี ไม่เรียก AI), ธีมชาดทอง, การ์ดแชร์ปีใหม่, "ถามนักพรตเรื่องปีนี้", push แคมเปญ 2 ครั้ง · `versionCode` 120 / `versionName` 1.2.0 (เว้น 117–119 ไว้ให้ v1.1 ออก patch) · Android เท่านั้น
สเปก: [`docs/superpowers/specs/2026-11-07-v1.2-cny-campaign-design.md`](superpowers/specs/2026-11-07-v1.2-cny-campaign-design.md)

**กำหนดเวลา:** ขึ้น Play production ภายใน **15 ม.ค. 2027** · เปิดแคมเปญ **20 ม.ค.** · จบ **22 ก.พ.**

**ค่าใช้จ่ายเพิ่ม:** `campaignPush` เป็นงาน Cloud Scheduler เพิ่ม 1 งาน → รวม `dailyPush` + `campaignPush` = 2 งาน ยังอยู่ในโควตาฟรี 3 งานต่อ billing account (เช็กใน Google Cloud Console → Cloud Scheduler ว่า billing account นี้ไม่มีงานอื่นปนจนเกิน 3) · FCM ฟรี · กวาประจำปีไม่เรียก AI จึงไม่เพิ่มค่า API ส่วนค่า API ของ "ถามนักพรตเรื่องปีนี้" ตามเกณฑ์เดิม

### A. ก่อนส่งแอป

1. ☐ อ่านและแก้ข้อความใน [`docs/content/year-messages.md`](content/year-messages.md) (Claude ร่าง ต้องให้คนตรวจ) · เกณฑ์: `overview` ≤ 200, `work`/`love`/`money` ≤ 80, `blessing` ≤ 60 ตัวอักษร · ไม่เอ่ยปีหรือนักษัตร · ไม่แนะนำการลงทุน กู้ยืม การรักษา หรือรับรองผล · ตรวจข้อความหน้ากวาประจำปีตอนแคมเปญปิด ("ช่วงเสี่ยงทายกวาประจำปีสิ้นสุดแล้ว") ด้วย
2. ☐ Build: รันใน `frontend/` → `./gradlew :androidApp:bundleRelease` ไฟล์จะอยู่ที่ `frontend/androidApp/build/outputs/bundle/release/androidApp-release.aab` → คัดลอกและเปลี่ยนชื่อเป็น `iching-ai-1.2.0-120.aab` ที่ root ของ repo (`cp frontend/androidApp/build/outputs/bundle/release/*.aab iching-ai-1.2.0-120.aab`, ไม่ commit ไฟล์นี้) แล้วอัปโหลดขึ้น Play Console (Closed testing ก่อน แล้วขอ Production ตามหัวข้อ 8)
3. ☐ Deploy (ทำเองเท่านั้น): รันใน `backend/` → `firebase deploy --only functions,hosting` · การ deploy ครั้งแรกจะสร้างงาน Cloud Scheduler `campaignPush` ให้เอง
4. ☐ ตรวจใน Cloud Scheduler: `campaignPush` ตั้งเวลา `0 * * * *` เขต `Asia/Bangkok` และมีงานรวมแค่ 2 (`dailyPush`, `campaignPush`)

### B. ตั้งค่าแคมเปญใน Firestore

เขียน **ทั้งออบเจ็กต์ `campaign`** ใน `config/runtime` ให้ครบทุกฟิลด์ ห้ามเขียนแค่บางฟิลด์ — ถ้าขาดฟิลด์ใดหรือชนิดผิด ระบบ (`parseCampaign`) จะอ่านเป็น null และ**แคมเปญปิดทันที** (รวมถึงเวลาต้องเป็นสตริง ISO 8601 ที่มี `+07:00`, `endsAt` ต้องหลัง `startsAt`, `year` เป็นตัวเลข, `enabled` เป็น boolean)

```json
{
  "campaign": {
    "id": "cny-2027",
    "year": 2027,
    "startsAt": "2027-01-20T00:00:00+07:00",
    "endsAt": "2027-02-22T00:00:00+07:00",
    "pushes": ["2027-02-05T19:00:00+07:00", "2027-02-06T08:00:00+07:00"],
    "enabled": false,
    "yearContext": "ปีมะแมธาตุไฟ (丁未) เริ่ม 6 ก.พ. 2027 — กวานี้คือกวาประจำปีของผู้แสวงหา ให้ตีความเป็นแนวทางทั้งปี"
  }
}
```

5. ☐ ตั้งค่านี้ไว้ล่วงหน้าด้วย `enabled: false` (ไม่มีค่า หรือผิดรูปแบบ = ปิดเช่นกัน)
6. ☐ **ก่อน 20 ม.ค. 2027** (หลัง deploy และทดสอบผ่านแล้ว): เปลี่ยน `campaign.enabled` เป็น `true` ล่วงหน้า ไม่ต้องรอวันที่ 20 เพราะ `startsAt` เป็น 2027-01-20 00:00 +07:00 อยู่แล้ว แคมเปญจะเปิดเองเมื่อถึงเวลา (ไม่ต้องออกเวอร์ชันใหม่) · ก่อนถึงเวลาเปิดแอปจะไม่เห็นการ์ด · หลัง 20 ม.ค. 00:00 เปิดแอปเช็กว่าการ์ดขึ้นบนหน้าแรก · แอปดูสถานะจาก `GET /status` (`campaign.active`) ไม่ใช้เวลาเครื่อง · หมายเหตุ: `pushes` ส่งจริงเฉพาะ 2 รายการแรก (backend มีข้อความ push แค่ 2 ชุด) รายการที่ 3 ขึ้นไปจะไม่ถูกส่ง
7. ☐ ปิดฉุกเฉินได้ทุกเมื่อด้วย `campaign.enabled = false` · กวาประจำปีที่ผู้ใช้ได้ไปแล้วยังเปิดดูได้
8. ☐ Push: ส่ง `campaignPush` ตรงชั่วโมงใน `pushes` ไป topic `daily-hexagram-th` (เฉพาะคนที่เปิดแจ้งเตือน) · คืน 5 ก.พ. 19:00 และเช้า 6 ก.พ. 08:00 · log ที่ควรเห็น `campaign_push_sent` · กันซ้ำด้วยเอกสาร `campaignPushes/{id}-{index}` · push 07:00 เดิมยังส่งตามปกติ

### C. Store listing

ชุดภาพและข้อความอยู่ที่ [`docs/store/cny/`](store/cny/README.md)

9. ☐ ถ่าย screenshot 2 ภาพตาม `docs/store/cny/README.md` (การ์ดแคมเปญบนหน้าแรก, หน้ากวาประจำปี) แล้ววางเป็นภาพที่ 1–2
10. ☐ อัปโหลด feature graphic `docs/store/cny/feature-graphic-1024x500.png`
11. ☐ เปลี่ยนคำอธิบายสั้นเป็น "เสี่ยงทายกวาประจำปีมะแม ๒๕๗๐ ฟรี · คำทำนายอี้จิงจากนักพรต" และสอดคำค้นเหล่านี้ลงในคำอธิบายเต็ม/คำอธิบายสั้น (Play Console ไม่มีช่องคำค้น): ดวงตรุษจีน, ดวงปีมะแม 2570, เสี่ยงทายปีใหม่, ดวงปีใหม่จีน
12. ☐ Play Console → Promotional content (LiveOps) ถ้าบัญชีมีสิทธิ์ · ถ้าไม่มี ข้ามได้
13. ☐ โพสต์การ์ดตัวอย่างลง TikTok / กลุ่มสายมูตามแผนเดิม เริ่ม 20 ม.ค.
14. ☐ **หลัง 22 ก.พ. 2027:** คืน listing (ข้อความ คำอธิบาย ภาพ) เป็นชุดเดิม · ตั้ง `campaign.enabled = false` ถ้ายังไม่ปิด

### D. ทดสอบบนเครื่องจริง (ยังไม่ได้ทำ — ไม่มีเครื่อง/emulator ตอนเตรียม build)

ทดสอบกับ dev project โดยตั้ง `campaign.startsAt` เป็นตอนนี้และ `enabled: true` (ใช้ออบเจ็กต์ครบทุกฟิลด์ตามหัวข้อ B) · อย่าแตะ production จนกว่าจะผ่านครบ

**ตามสเปกข้อ 9 ("เครื่องจริง")**
- ☐ เสี่ยงทาย → หน้ากวาประจำปี → แชร์ไป LINE และ IG Story → ภาพครบ
- ☐ ลบแอป → ติดตั้งใหม่ด้วยบัญชี Google เดิม → ได้กวาประจำปีเดิมและธีมยังอยู่
- ☐ ถามนักพรตเรื่องปีนี้ด้วยบัญชีฟรี → ดูโฆษณา → ได้คำทำนายของกวาประจำปี → อยู่ใน Journal พร้อมป้าย
- ☐ ส่ง push ทดสอบ `type=campaign` (Firebase Console → Messaging → topic `daily-hexagram-th` พร้อม custom data `type=campaign`, `campaignId=cny-2027`) → แอปปิดอยู่ แตะแล้วเปิดหน้ากวาประจำปี (หรือพิธีเสี่ยงทายถ้ายังไม่มี)
- ☐ ทุกหน้าใหม่บนจอแคบและทุกธีม

**เพิ่มจากงานก่อนหน้า**
- ☐ Room migration: ติดตั้ง 1.1.0 (116) ที่มีรายการใน Journal → อัปเกรดเป็น build 1.2 → รายการยังอยู่และแอปเปิดได้ (DB v4→v5 เพิ่ม `readings.yearOf`)
- ☐ แอป v1.1 ที่ติดตั้งอยู่ได้รับ push `type=campaign` → เปิดหน้าแรกปกติ ไม่ crash
- ☐ การ์ดแชร์ปีใหม่: เรนเดอร์ 1080×1920 · ไม่มีส่วนใดถูกตัดบนจอ 360×640 · แชร์ไป LINE แล้วไม่ถูกตัด · ข้อความสีทอง/inkSoft บนพื้นชาดมี contrast ≥ 4.5:1 · ไม่มีคำถามและไม่มีป้าย AI
- ☐ คำทำนายปีนี้: ไหลของ "ถามนักพรตเรื่องปีนี้" ใช้งานได้ · Journal แสดงป้าย "กวาประจำปี" · หลังแคมเปญจบ (ตั้ง `endsAt` ให้ผ่านไปแล้ว) ยังถามและเปิดดูได้ตามสิทธิ์
- ☐ หลังแคมเปญจบ: การ์ดหน้าแรกหาย · ผู้ที่ยังไม่เคยเสี่ยงเข้าหน้ากวาประจำปีแล้วเห็นข้อความ "ช่วงเสี่ยงทายกวาประจำปีสิ้นสุดแล้ว" · ธีมชาดทองที่ได้ยังอยู่
- ☐ เจ้าของตรวจถ้อยคำใน `docs/content/year-messages.md` และข้อความ "ช่วงเสี่ยงทายกวาประจำปีสิ้นสุดแล้ว"

### E. KPI ที่ต้องดูใน Firebase Analytics (ช่วงแคมเปญ)

| KPI | เป้า | วัดจาก |
|---|---|---|
| ผู้ใช้ active ที่เสี่ยงทายกวาประจำปี | ≥ 40% | `year_cast` ÷ ผู้ใช้ active |
| อัตราแชร์การ์ดประจำปี | ≥ 15% ของการเสี่ยงทายกวาประจำปี | `year_share` ÷ `year_cast` |
| ยอดติดตั้งต่อวัน | ≥ 2 เท่าของค่าเฉลี่ย 4 สัปดาห์ก่อนแคมเปญ | Play Console → Statistics |
| คำทำนาย AI จากปุ่ม "ถามนักพรตเรื่องปีนี้" | ≥ 10% ของการเสี่ยงทายกวาประจำปี | `year_ai_done` ÷ `year_cast` (ดู `year_ai_tap` เทียบด้วย) |
| ค่า API ÷ รายได้สุทธิ | < 50% | ตามเดิม |

Event ทั้งหมด: `campaign_banner_shown`, `campaign_banner_tap` (`state` = `new` \| `cast`), `year_cast`, `year_view`, `year_share` (`target`), `year_ai_tap`, `year_ai_done` (`charge`), `push_open` (เพิ่ม `type = campaign`) · ทุก event มี `campaign_id`
