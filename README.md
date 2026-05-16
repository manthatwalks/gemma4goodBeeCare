# BeeCare Anywhere

On-device beekeeping diagnostics for smallholder farmers — Gemma 4 E2B running locally via LiteRT, offline after a one-time model download.

See [PLAN.md](./PLAN.md) for the full project plan, technical approach, and phase breakdown.

## Status

**Phase 1 — Buildable shell.** App compiles, installs, and launches to a Compose screen titled "BeeCare Anywhere". No model integration yet.

## Build

### Prerequisites

- **JDK 21** (the Gradle daemon JVM is pinned to 21 in `gradle/gradle-daemon-jvm.properties`)
- Android SDK with `platforms;android-35`, `build-tools;35.0.0`, and `platform-tools`
- `local.properties` with `sdk.dir=<path-to-sdk>` (auto-created on first build by Android Studio; create manually otherwise)

The simplest macOS setup:

```sh
# Toolchain
brew install --cask android-commandlinetools
brew install openjdk@21

# Make JDK 21 discoverable by Gradle's daemon-JVM auto-selection
mkdir -p ~/Library/Java/JavaVirtualMachines
ln -sfn /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk \
        ~/Library/Java/JavaVirtualMachines/openjdk-21.jdk

# SDK packages
export ANDROID_HOME="/opt/homebrew/share/android-commandlinetools"
yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" \
    "platforms;android-35" "build-tools;35.0.0" "platform-tools"

# Project SDK pointer (gitignored)
echo "sdk.dir=$ANDROID_HOME" > local.properties
```

### Build

```sh
./gradlew assembleDebug             # produces app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug              # installs on a connected device (requires adb)
```

The Gradle wrapper is committed — no separate Gradle install needed.

### Install on a connected device

```sh
./gradlew installDebug
```

## Project layout

```
.
├── PLAN.md                          # full project plan
├── app/
│   ├── build.gradle.kts             # app module config
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── kotlin/com/beecareanywhere/
│       │   ├── MainActivity.kt      # Compose entry point
│       │   └── ui/theme/            # Material 3 theme
│       └── res/                     # strings, themes, icons
├── build.gradle.kts                 # root project
├── settings.gradle.kts
├── gradle.properties
└── gradle/libs.versions.toml        # version catalog
```

Phases 2 onward add `model/`, `multimodal/`, `data/`, and `di/` packages. See PLAN.md for the full structure.

## Repository

https://github.com/manthatwalks/gemma4goodBeeCare

## License

To be determined. Apache 2.0 likely (matches Gemma + LiteRT).
