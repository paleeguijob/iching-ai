# อี้จิง AI — การ์ดโปรบนหน้าแรก

17 พ.ย. 2026 · ต่อจากหน้าแรกแบบ E (กวาประจำวันเป็นพระเอก + แถว streak ๑–๗) บน branch `feature/v1.4-siamsi`

## 1. เป้าหมาย

เปิดโปรลดราคา Premium บนหน้าแรกได้จาก server โดยไม่ต้องออกแอปเวอร์ชันใหม่ และไม่ทำให้หน้าแรกกลับไปรกเหมือนก่อนออกแบบใหม่

**อยู่ในรอบนี้:** การ์ดโปรแบบ P2 หนึ่งใบใต้แถว streak · กดแล้วไปหน้าจ่ายเงินเดิม · กด × ปิดถาวรต่อโปร · analytics

**ไม่อยู่ในรอบนี้:** popup · รูปภาพจาก server · ปลายทางอื่น (หน้าอื่นในแอป, ลิงก์ร้านข้างนอก) · App Links จากนอกแอป · การ์ดโปสเตอร์ในหน้ากวาประจำวัน/ประจำปี/บันทึก · การตั้งราคา (ยังทำใน Play Console และ RevenueCat)

## 2. ค่าตั้งบน server

เพิ่ม `config/runtime.promo` ใน Firestore อ่านด้วยฟังก์ชันใหม่ `parsePromo` ใน `core/promo.ts` ตามแบบ `parseCampaign` ถ้าไม่มี ผิดรูปแบบ หรือขาดฟิลด์ใด = ไม่มีโปร

| ฟิลด์ | ชนิด | ตัวอย่าง | ความหมาย |
|---|---|---|---|
| `id` | string ไม่ว่าง | `"1111-2026"` | key ของ analytics และการกดปิด · เปลี่ยน id = โปรใหม่ |
| `enabled` | boolean | `true` | สวิตช์ปิดทันที |
| `startsAt` / `endsAt` | ISO string, `endsAt > startsAt` | `2026-11-10T00:00+07:00` | ช่วงเวลาแสดง |
| `label` | string ไม่ว่าง ≤ 20 ตัวอักษร | `"โปรพิเศษ"` | ป้ายเล็กสีชาดด้านบน |
| `title` | string ไม่ว่าง ≤ 40 ตัวอักษร | `"Premium รายปี ลด ๔๐%"` | ข้อความหลัก |
| `cta` | string ไม่ว่าง ≤ 12 ตัวอักษร | `"ดูโปร"` | ลิงก์ด้านล่าง ต่อท้ายด้วย " ›" |
| `showEndDate` | boolean | `true` | ต่อท้ายป้ายด้วย "ถึง ๒๐ พ.ย." จาก `endsAt` |

ข้อความยาวเกินกำหนด = ผิดรูปแบบ (กันการ์ดล้นจอ)

## 3. ส่งให้แอปผ่าน `/status`

`QuotaStatus` ได้ฟิลด์ใหม่ `promo: { id, label, title, cta, endsAt, showEndDate } | null`

server ส่งค่าเฉพาะเมื่อครบทุกข้อ ไม่อย่างนั้นส่ง `null`:
- `promo` ผ่านการ parse และ `enabled`
- `startsAt <= now < endsAt`
- ผู้ใช้ไม่ได้เป็น Premium

แอปเวอร์ชันเก่าไม่รู้จักฟิลด์นี้และไม่สนใจ (DTO ใช้ค่าเริ่มต้น)

## 4. ฝั่งแอป

**โมเดล:** `AccountStatus.promo: PromoStatus?` (`id, label, title, cta, endsAtMillis, showEndDate`) อ่านจาก `PromoDto` ใน `HttpOracleApi`

**การกดปิด:** `AppSettings.dismissedPromoIds: List<String>` เก็บ id ล่าสุดไม่เกิน 20 ตัว (เก่าสุดหลุดก่อน)

**ฟังก์ชันตัดสิน (pure, มี test):** `promoCard(status, dismissed, nowMillis): PromoCard?` คืนการ์ดเมื่อ
- `status.promo != null` และ `!status.premium` (กันกรณี status ในเครื่องเพิ่งเปลี่ยนเป็น Premium ก่อน server ตอบ)
- `nowMillis < endsAtMillis` (กันแอปที่เปิดค้างข้ามเวลาสิ้นสุด)
- id ไม่อยู่ใน `dismissed`

`PromoCard` เตรียมข้อความไว้แล้ว: `label` (ต่อด้วย " · ถึง ๒๐ พ.ย." เมื่อ `showEndDate` ใช้วันที่ไทยตามเวลา Bangkok), `title`, `cta`

**HomeViewModel:** `promoCard: StateFlow<PromoCard?>` จาก `status` + settings + เวลาปัจจุบัน · `onPromoShown(id)` (log ครั้งเดียวต่อ id ต่อ ViewModel) · `onPromoTapped(id)` · `onPromoDismissed(id)` บันทึกลง settings

**หน้าตา (แบบ P2):** ใต้แถว streak และบรรทัดกวาประจำปี ภายในส่วนที่เลื่อนได้
- การ์ดขาวขอบบาง มุมโค้ง 14dp ไม่มีพื้นสีเข้ม
- ช่องสี่เหลี่ยม 52dp สีชาด (`CinnabarRed`) มีตรา 易 สีกระดาษอยู่ทางซ้าย
- ขวา: ป้ายสีชาด (labelSmall) · ชื่อโปร (titleSmall) · cta สีชาด
- ปุ่ม × มุมขวาบน ขนาดแตะ 48dp contentDescription "ปิดโปรนี้"
- กดตัวการ์ด → `onPromoTapped` แล้วเปิด `PaywallRoute` เดิม · กด × → `onPromoDismissed` การ์ดหายทันที

**หน้าจ่ายเงิน:** ไม่แก้ ราคาส่วนลดมาจาก offering ปัจจุบันของ RevenueCat ซึ่งต้องสลับเองก่อนเปิดโปร (ใส่เป็นขั้นตอนใน `RELEASE.md`)

## 5. Analytics

| event | params | เมื่อ |
|---|---|---|
| `promo_shown` | `promo_id` | การ์ดแสดงครั้งแรกในแต่ละ ViewModel |
| `promo_tapped` | `promo_id` | กดการ์ด |
| `promo_dismissed` | `promo_id` | กด × |

## 6. การทดสอบ

**backend (`core.test.ts`):** parse ค่าครบ · ขาดฟิลด์ · ข้อความยาวเกิน · `endsAt <= startsAt` · `status()` ส่ง `null` เมื่อ ปิด / ก่อนเริ่ม / หลังจบ / Premium และส่งค่าเมื่อครบเงื่อนไข

**แอป (`PromoCardTest`):** ไม่มีโปร · Premium · กดปิดแล้ว · id ใหม่หลังปิดอันเก่า · หมดเวลาในเครื่อง · ป้ายวันสิ้นสุด (ไทย, เวลา Bangkok) · `dismissedPromoIds` เกิน 20 แล้วตัวเก่าหลุด

**บน emulator:** ตั้ง `promo` ใน Firestore ของ dev → การ์ดขึ้นใต้ streak · กดไปหน้าจ่ายเงิน · กลับมากด × → หาย · ปิดเปิดแอปใหม่ไม่กลับมา · จอเล็ก 720×1280 แถบถามด้านล่างยังอยู่ครบ

## 7. ความเสี่ยง

- **หน้าแรกรกขึ้น:** คุมด้วยกติกา — ใบเดียว, ใต้ streak เท่านั้น, ไม่มีพื้นสีเข้ม, ปิดได้, ไม่แสดงกับ Premium
- **ราคาในโปรไม่ตรงกับหน้าจ่ายเงิน:** ข้อความโปรพิมพ์เองใน Firestore ส่วนราคาอยู่ RevenueCat ต้องตรวจทั้งสองที่ก่อนเปิด `enabled` (เช็กลิสต์ใน `RELEASE.md`)
- **status ช้า/ออฟไลน์:** ไม่มี status = ไม่มีการ์ด ไม่แสดงโปรเก่าที่ค้างอยู่ในเครื่อง
