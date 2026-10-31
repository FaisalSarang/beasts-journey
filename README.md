# Beast's Journey

A free, open-source workout tracker for people on a journey to get stronger. Built for Android. No ads, no paywalls, no account. All your data stays on your phone.

## ⬇️ Download

**[Download the latest APK](https://github.com/FaisalSarang/beasts-journey/releases/latest/download/BeastsJourney.apk)**

To install:
1. Open the link above on your Android phone (Android 8.0 or newer).
2. Tap the downloaded `BeastsJourney.apk`.
3. If asked, allow your browser or file manager to "install unknown apps", then tap **Install**.

New versions install over the old one, and your workouts and history are kept.

## Features

- **Home screen with your saved workouts.** Tap **Start** to begin one.
- **Build workouts with the + button.** Add exercises with a name, a type (dumbbells, barbell, kettlebell, cable machine, machine, treadmill, bike and more), the number of sets, and an optional rep range.
- **Log every set.** Enter the weight and reps for each set. Your target rep range appears in light text inside the reps box, and last session's numbers are shown next to every set.
- **Workouts in progress are saved.** If you close the app mid-workout, you can pick up where you left off.
- **Progress tracking.** Charts for each exercise show estimated 1-rep max, best weight, volume, or reps over time, with % change vs. last time and since your first session. Treadmill and other cardio track distance and minutes.
- **History.** See every finished session and delete the ones you don't want.
- kg or lb units.
- Red and black in dark mode, red and white in light mode (follows your phone setting).

## Building it yourself

The APK is built by GitHub Actions ([`.github/workflows/release.yml`](.github/workflows/release.yml)) on every push to `main` and published as a GitHub Release.

To build locally, open the project in Android Studio, or run `gradle assembleDebug` with Gradle 8.11+ and JDK 17.

Built with Kotlin and Jetpack Compose.
