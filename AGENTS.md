# AGENTS.md

## Status
- Morphe patch collection (from `MorpheApp/morphe-patches-template`, current as of 2026-09-23). Patcher stays external — do not copy `morphe-patcher` source in.
- Plugin `app.morphe.patches:1.3.4`, `app.morphe:morphe-patcher:1.13.0` (see `gradle/libs.versions.toml`, `settings.gradle.kts`). Do not bump without verifying against the current template.

## Commands
- `./gradlew buildAndroid` → `patches/build/libs/patches-*.mpp`. Apply locally with Morphe Desktop.
- Requires GitHub Packages auth (`read:packages`): `~/.gradle/gradle.properties` with `gpr.user`/`gpr.key`, or `GITHUB_ACTOR`/`GITHUB_TOKEN`. Without it plugin resolution fails as "not found".
- Work on `dev`; merge (no squash) to `main` for stable releases. Never hand-edit `patches-list.json`, `patches-bundle.json`, `CHANGELOG.md`, README patches section — `release.yml`/semantic-release owns them. No force-push (breaks releases).

## Conventions
- Spotify patches: `patches/src/main/kotlin/app/spotifycustomizer/patches/spotify/` (one patch per file) + `Fingerprints.kt`; compatibility in `.../shared/Constants.kt` (`com.spotify.music`, currently experimental any-version).
- Extensions: `extensions/extension/` → `extensions/extension.mpe`, referenced via `extendWith(...)`.
- No Premium/DRM/subscription bypass patches. No guessing Spotify classes/methods/resources/versions — derive from a legally obtained APK and record verified app version.

## Constraints
- Do not commit APKs, patched binaries, keystores, `gradle.properties` tokens, or `local.properties`. Base APKs are user-supplied, never stored here.
