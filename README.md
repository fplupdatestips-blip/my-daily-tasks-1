# Daily Tasks

A personal app for repeating daily tasks: an Android app and a web app that
stay in sync through Firebase. Plan and mockups:
[plan](https://claude.ai/artifact/2qYBCd9AVQXZofGfz93hqJ) ·
[mockups](https://claude.ai/artifact/Xovn9zn73YYJGzatSvZMeJ).

## What's in here

| Folder | What it is |
| --- | --- |
| `shared/repeat-cases.json` | Test cases for the repeat rules. Both apps must pass every one. |
| `web/` | React + TypeScript + Vite + Tailwind web app |
| `android/core/` | Plain Kotlin: repeat rules, dates, Today list logic |
| `android/app/` | Android app: Kotlin, Jetpack Compose, Material 3 |
| `firestore.rules` | Each signed-in user can only touch `users/{their uid}` |

## Version 0.1 (first version core)

- Today screen grouped by Morning, Afternoon, Evening, Anytime, with a progress ring, week strip, one-tap complete and undo.
- Every task has a description shown under its name.
- Subtasks: tick them one by one; ticking the last one completes the task. On repeating tasks they reset each day; on one-off tasks progress is kept.
- Repeat rules: one-off, every day, weekdays, specific days, every N days, N times a week or month, monthly by date or weekday, yearly, N days after last done. Start and end dates, pause, archive.
- One-off tasks that aren't done move to the next day with a "Moved from Thu" label ("3 days late" after a few days). Repeating tasks don't pile up; a missed day shows as missed.
- Task detail with current and best streak, 30-day rate, a month calendar (tap a past day to backfill) and history.
- Tasks list (active, paused, archived) with reordering, a weekly Progress view, and Settings (theme, first day of week, when your day ends, export and import on web).
- Google sign-in and live sync through Firestore, working offline on both.

Next, following the plan: phone reminders with Done and Snooze buttons, the
home-screen widget, onboarding, then polish.

## Setting up Firebase (once)

1. In the [Firebase console](https://console.firebase.google.com) create a project.
2. **Authentication › Sign-in method**: turn on **Google**.
3. **Firestore Database**: create a database, then paste `firestore.rules` into the Rules tab and publish.
4. **Project settings › Your apps**:
   - Add a **Web app**. Copy its config values into `web/.env.local` (see `web/.env.example`).
   - Add an **Android app** with package name `app.dailytasks` and this SHA-1 (from the debug key in `android/app/debug.keystore`):
     `AA:75:83:F3:4A:B5:6F:68:DD:4B:A4:97:83:6D:B6:2B:61:46:E3:5E`.
     Download `google-services.json` into `android/app/`. For GitHub builds, also save its contents as a repository secret named `GOOGLE_SERVICES_JSON`.

## Running the web app

```bash
cd web
npm install
npm run dev      # http://localhost:5173
npm test         # shared repeat-rule tests
npm run build    # output in web/dist
```

Without `web/.env.local` it runs in **test mode**: no sign-in, data saved in
that browser only. To publish it: `npm run build`, then from the repo root
`npx firebase-tools deploy --only hosting,firestore:rules`.

## Building the Android app

- **GitHub**: every push runs `.github/workflows/build.yml`, which tests the
  repeat rules and builds a debug APK. Download it from the run's
  **Artifacts** (`daily-tasks-apk`) and install it on your phone.
- **Android Studio**: open the `android/` folder and press Run.
- Repeat-rule tests on any machine with Java: `gradle -p android/core test`.
