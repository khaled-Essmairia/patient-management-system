# Clinic Management System (Spring Boot 3)

Spring Boot 3.3 · Thymeleaf · Spring Security · Spring Data JPA · MySQL. Java 17+.

## One installation = one cabinet = one doctor
The app is installed separately for each cabinet. It has a single doctor: every consultation and
appointment belongs to him automatically, so nobody has to choose a doctor. The secretary edits the
doctor's name, specialty, fee, username and password in **Doctor profile**.

## Two roles only
| Role | What they can do |
|------|------------------|
| **Secretary** | Patients (full file), appointments, consultations, invoices & payments, reports, feedback, doctor profile |
| **Doctor** | See **their own** appointments, write/update reports, see their profile |

Navigation is a left **sidebar**. Patients have no account: the secretary creates and manages their files.

## Patient file (New patient / Update patient)
Header in the key/value style of the invoice screen, then three line grids you can add to with **+**
and remove with **−**, each with small pagination (5 lines per page):
1. **Consultations**: date, doctor, reason, symptoms, diagnosis, treatment, notes, next visit
2. **Appointments**: old and new, over the years
3. **Invoices**: date, description, unit price, quantity, VAT, method, status, with automatic totals

Everything is saved in one click. Consultations, appointments, invoices and reports also have their
own sidebar page with **View / Update / Delete**.

## Run
1. MySQL 8 on `localhost:3306` (edit user/password in `application.properties`).
2. `mvn spring-boot:run`, then open http://localhost:8081

A new database `clinic_db` is created automatically (the old `healthcare_db` is not reused).

## First accounts (change the passwords!)
| Role | Login | Password |
|------|-------|----------|
| Secretary | `secretary` | `123456` |
| Doctor | `doctor` | `123456` |

## Settings
`clinic.name` and `clinic.currency` in `application.properties`.


## Tables: page size and search
Every table (lists and the three grids of the patient file) has **Show 5 / 10 / 15 / 20 / 30 / 50 per page**, a **search box**
that looks in every column, and page buttons. In the patient file the **+** button stays visible
(table header and tool bar) whatever page or search is active.

## Sidebar (secretary)
Dashboard, Patients, Appointments, Consultations, **Import patients**, Invoices & payments, Reports, Feedback,
Doctor profile, **System administration > Admin profile**, **Reporting**, **License management** (last).

- **Import patients**: download the CSV template, fill it, upload it. Rows that are wrong are listed with their line number;
  patients that already exist (same first name, last name, birth date) are skipped.
- **Admin profile**: change the password of the signed-in secretary (old, new, confirm).
- **Reporting**: export each table to CSV, or every table in one ZIP. Password hashes are never exported.
- **License management**: shows the Machine ID, takes the key made by `license-generator.html`, keeps the history.
  Without a valid license the menu shows only *License management* and every other page redirects to it.
  The public key is in `application.properties` (`clinic.license.public-key`); if you create a new key pair in the
  generator, paste the new public key there.


## New in this version

### 1. Prescriptions (ordonnances)
- Menu **Prescriptions** (secretary and doctor). One medicine per line, with the dosage. **Save and print** opens the print page (A5).
- In **Consultations**, the **Prescription** button creates a prescription for that patient and copies the treatment written in the consultation.
- The header shows the doctor (name, specialty, qualification) from the **Doctor profile** screen, plus the cabinet name, address and phone
  from `application.properties`: `clinic.name`, `clinic.address`, `clinic.phone`, `clinic.footer`.

### 2. Arabic / French / English and printable invoices
- Language switch at the bottom of the sidebar and on the login page (default: `clinic.default-language=fr`). The choice is remembered in a cookie. Arabic is displayed right-to-left.
- All screen texts are in `src/main/resources/messages.properties` (English), `messages_fr.properties` and `messages_ar.properties`.
  The key of a text is `t.` + the English text in lowercase with `_` between words. To change a translation, edit the line in the file of that language.
- **Invoices**: **Print** on every invoice; **Receipt** on paid invoices (payment receipt with the amount received).

### 3. Daily backup
- A backup (ZIP containing one `.sql` file) is created automatically once a day. The check runs every hour and one minute after start-up, so a backup is also made when the computer is switched off at night.
- Menu **System administration > Backups**: **Back up now**, download, list.
- Settings: `backup.dir` (default `./backups`), `backup.extra-dir` (second copy, e.g. a USB drive), `backup.keep` (default 30).
- **Restore**: unzip the file, then `mysql -u root -p clinic_db < clinic_backup_YYYY-MM-DD_HHMMSS.sql` (existing tables are replaced).
- The backup contains patient data and password hashes: keep the folders private.

### 4. SMS reminders
- Switched off by default. Set `sms.enabled=true` and the `sms.http.*` properties of your SMS provider (examples are in `application.properties`).
- A reminder is sent `sms.reminder-hours-before` hours (default 24) before each **scheduled** appointment, between `sms.send-from-hour` and `sms.send-until-hour`.
  If the appointment is moved, a new reminder is sent. Each appointment gets at most 3 sending attempts.
- Appointments screen: **Send reminder** button, "SMS ✓" mark and the sending date in the details.
- Phone numbers with 8 digits get the country code `sms.default-country-code` (216). The text of the SMS is the key `sms.reminder` in the messages files.
- If your provider does not accept Arabic letters (UCS-2), use `sms.language=fr`.
