# Breathe

A calm, offline breathing app for Android, built with Jetpack Compose. Pick a rhythm, pick a length, and follow a visual guide through a session that always ends after a full exhale.

## Features

- **Four breathing patterns**

  | Pattern | Rhythm (seconds) | For |
  | --- | --- | --- |
  | Calm | 4 · 7 · 8 | Winding down and easing stress |
  | Box | 4 · 4 · 4 · 4 | Steady focus before a big moment |
  | Balance | 5 · 5 | An even, coherent rhythm |
  | Sleep | 4 · 8 | Long exhales to drift off |

- **Session lengths** of 2, 5 or 10 minutes by default, or any three you choose (1 to 60 minutes) from Settings or by long-pressing them on Home. Sessions round up to a whole number of breaths.
- **Three guides** to follow: Orb (grows and shrinks), Tide (water rising and ebbing in a vessel) and Trace (a point of light tracing a rounded square).
- **Cues** through haptics, sound and screen-reader phase announcements. Haptics, sound and keeping the screen awake can each be turned off in Settings, and a session pauses when you leave the app.
- **Reduce Motion** follows the system setting, with an override in Settings.
- **Progress**: streaks, weekly minutes, sessions, and a mood check after each session.
- **Eight badges** (Steady Start, Unbroken, Seven Days, Night Tide, Four Winds, Deep Water, Square Mind, Mountain). Only full sessions count (at least 90% of the chosen length), consistency is judged over forgiving windows rather than unbroken streaks, and an earned badge is never lost.
- **Daily reminder** notification with start, snooze and skip actions, rescheduled after reboots and time changes.
- **Sharing** your streak or badges through the system share sheet.
- **First-open walkthrough** that leads into a 2-minute first session; it can be replayed from Settings.
- Light and dark themes that follow the system.

All data stays on the device, in `SharedPreferences`. There is no account, network access or analytics. See the [privacy policy](docs/privacy-policy.md).

## Requirements

- Android Studio (its bundled JDK 17+ works; the build targets Java 17)
- Android SDK 36
- A device or emulator running Android 7.0 (API 24) or later

The Gradle wrapper (Gradle 8.13, AGP 8.13.2, Kotlin 2.2.21) is checked in, so no separate Gradle install is needed.

## Building and running

Open the project in Android Studio and run the `app` configuration, or use the command line:

```bash
./gradlew assembleDebug
```

```bash
./gradlew installDebug
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Release builds

Release builds are shrunk with R8 and signed with the Google Play upload key. Copy [`keystore.properties.example`](keystore.properties.example) to `keystore.properties`, point it at your keystore and fill in the passwords. Both files stay out of git. Then build the bundle to upload to Play:

```bash
./gradlew bundleRelease
```

Without `keystore.properties` the release build is produced unsigned. Answers for the Play Console's policy forms are in [`docs/play-console-answers.md`](docs/play-console-answers.md).

## Tests

Unit tests cover session timing and lengths, practice stats, reminder scheduling and badge rules:

```bash
./gradlew testDebugUnitTest
```

## Project layout

```
app/src/main/java/com/jaskaransethy/breathe/
├── data/         Patterns, session timing, stats, badge rules and the BreatheStore persistence
├── reminder/     Reminder alarm scheduling, notification and broadcast receivers
└── ui/
    ├── welcome/      First-launch welcome
    ├── walkthrough/  First-open walkthrough
    ├── home/         Home, Progress and Settings tabs
    ├── session/      Breathing session, cues, sounds and the Orb / Tide / Trace guides
    ├── complete/     Session complete and mood check
    ├── badges/       Badges screen and "Badge earned" sheet
    ├── components/   Shared composables and sharing
    ├── icons/        Lucide icons
    └── theme/        Colours and typography
```

## Credits

- Fonts: [Cormorant Garamond](https://fonts.google.com/specimen/Cormorant+Garamond) and [Josefin Sans](https://fonts.google.com/specimen/Josefin+Sans), under the SIL Open Font License
- Icons: [Lucide](https://lucide.dev), under the ISC License
- Sound cues: public domain (CC0) recordings from Freesound and Wikimedia Commons

The full license texts and sound sources are in [`app/src/main/assets/licenses`](app/src/main/assets/licenses).

## License

Copyright (C) 2023–2026 Jaskaran

Breathe is free software: you can redistribute it and/or modify it under the terms of the [GNU General Public License](LICENSE) as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

Breathe is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

Bundled fonts, icons and sounds keep their own licenses; see [Credits](#credits).
