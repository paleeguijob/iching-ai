# Play Console — Production access questionnaire

คำตอบสำหรับแบบสอบถามที่ Play Console ให้กรอกหลัง closed test ครบ 14 วัน (Dashboard → Apply for production)
ร่างจาก `com.iching.ai_production.pdf` ของ Testers Community แต่แก้ให้ตรงกับแอปและสิ่งที่ทำจริง:
- ตัด "offline capabilities" ออก เพราะคำทำนาย AI ต้องต่ออินเทอร์เน็ต
- ข้อ 8 เปลี่ยนจาก "กำลังทำ" เป็นสิ่งที่ทำเสร็จแล้ว พร้อมของจริงในแอป

☐ ก่อนส่ง ตรวจข้อ 1 ว่าตรงกับวิธีหาผู้ทดสอบจริง ถ้าไม่ได้ชวนคนรู้จักหรือกลุ่มผู้สนใจอี้จิง ให้ลบประโยคที่สองออก
☐ ข้อ 8 ระบุ "Share app" และคำอธิบายใหม่ ให้ส่งฟอร์มหลังปล่อย build ที่มีปุ่มนี้ และวางคำอธิบายจาก `listing-th.md` แล้ว

---

**1) How did you recruit users for your closed test?**

We used a paid testing provider (Testers Community) so the app was tested on many different Android devices and OS versions. We also shared the test link with people we know who are interested in I Ching and Chinese fortune-telling, so we could hear from our real target audience.

**2) How easy was it to recruit testers for your app?**

Easy

**3) Describe the engagement you received from testers during your closed test**

Testers installed the app and used the main flow across the 14 days: casting a hexagram, reading the AI interpretation, the daily hexagram, the journal and sharing reading cards. They reported no crashes or functional bugs across their devices. Their written report focused on how new users discover and learn the app, and on how to help the app grow, which shaped the changes in question 8.

**4) Provide a summary of the feedback that you received from testers. Include how you collected the feedback.**

We collected feedback through the provider's written test report and through direct messages with testers. Testers found the app stable on all devices and SDK versions they tried, with no crashes. They suggested four improvements: a richer store description with relevant keywords, a walkthrough for first-time users, an easy way to rate the app, and a "Share app" option in Settings. We also added an in-app "Send feedback" form in Settings so testers and future users can tell us what to improve directly.

**5) Who is the intended audience for your app?**

Thai-speaking adults (13+) who are interested in the I Ching, Chinese fortune-telling and traditional divination, and people who want a calm moment of self-reflection before making a decision about work, money, relationships or life.

**6) Describe how your app provides value to the users.**

The app lets users cast an I Ching hexagram the traditional way (three coins, six throws) and then explains the result in plain Thai. Every AI interpretation is grounded in the original text of the 64 hexagrams and 384 lines, and the original text is always available even without the AI reading. Users also get a daily hexagram, a personal journal to look back on past readings and note whether they came true, and shareable reading cards. AI-generated content is clearly labelled, can be reported in the app, and is presented for entertainment and reflection, not as medical, legal or financial advice.

**7) How many installs do you expect your app to have in your first year?**

10K – 100K

**8) What changes did you make to your app based on what you learned during your closed test?**

- Added a first-launch walkthrough that shows new users, step by step, how a reading works, and can be skipped.
- Added Google Play's In-App Review prompt, shown only right after a completed reading, plus a "Rate the app" option in Settings that opens the Play Store page.
- Added a "Share app" option in Settings that sends a link to the app through any messaging or social app.
- Added a "Send feedback" form in Settings so users can tell us what to improve.
- Rewrote the store description with more detail about the features and with the keywords our audience searches for (I Ching, fortune-telling, Chinese horoscope, hexagram, daily reading).

**9) How did you decide that your app is ready for production?**

The app ran without crashes or functional bugs on all devices and Android versions used during the 14-day closed test, and every improvement testers suggested is now in the app. We also checked purchases and restore with license-testing accounts, verified that AI usage limits are enforced on the server, and completed the Data safety, content rating, ads and AI-generated content declarations.

**10) What did you do differently this time?**

This is our first application for production access. *(ถ้าเคยขอแล้วถูกปฏิเสธ ให้เปลี่ยนเป็น: "Compared with our previous application, we ran a full 14-day closed test with a larger group of testers on more devices, collected written feedback, and shipped every suggested improvement before applying again.")*
