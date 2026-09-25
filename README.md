# Spotify Customizer Patches

Independent third-party patch collection for the Spotify Android app, built on the Morphe patching ecosystem.

## ❓ About

This is a **patch project**, not a Spotify client, manager, downloader, or modified app. It produces patches (`.mpp` bundle) that can later modify a legally obtained Spotify Android APK via Morphe tooling.

- Independent third-party project. Not affiliated with Spotify AB or the Morphe open-source project.
- Uses the Morphe patching ecosystem: `MorpheApp/morphe-patcher` as an external Gradle plugin/dependency (see `settings.gradle.kts`, `gradle/libs.versions.toml`). The patcher source is not copied into this repo.
- No Premium / DRM / subscription bypass functionality is provided or planned. Focus is on harmless UI/customization experimentation.
- No Spotify APKs, patched binaries, or keystores are committed to this repo.

### How to use these patches

Click here to add these patches to Morphe: https://morphe.software/add-source?github=xeriomy/spotify-customizer

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->
> **[v1.0.0](https://github.com/xeriomy/spotify-customizer/releases/tag/v1.0.0)**&nbsp;&nbsp;•&nbsp;&nbsp;`main`&nbsp;&nbsp;•&nbsp;&nbsp;1 patches total
<details open>
<summary>📦 Spotify&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Placeholder patch](#placeholder-patch) | Placeholder patch to verify the patch collection builds. Does nothing. |  |

</details>

<!-- PATCHES_END -->

## 🚀 Getting started

Prerequisites:

- JDK 21+
- GitHub Packages auth for the Morphe registry (`read:packages`). Without it, Gradle cannot resolve `app.morphe.patches` plugin / `app.morphe:morphe-patcher`:
  - Create a PAT with `read:packages`, then add to `~/.gradle/gradle.properties` (create if missing):
    ```properties
    gpr.user = <GitHub username>
    gpr.key = <Personal access token>
    ```
  - Or set `GITHUB_ACTOR` / `GITHUB_TOKEN` env vars (CI provides `GITHUB_TOKEN` automatically).

## 🧑‍💻 Dev usage

- Make all changes on the `dev` branch. Merge `dev` into `main` (no squash) for stable releases; `release.yml` + semantic-release handles versioning and `patches-list.json` / `patches-bundle.json` generation. Do not edit those generated files by hand.
- Use semantic commits (`feat:`, `fix:`, `chore:`). `feat`/`fix` trigger pre-releases on `dev`; `chore` does not.
- Do not force-push release commits.

### 🛠️ Building locally

- Run `./gradlew buildAndroid`
- The built patches `.mpp` file is in `patches/build/libs/patches-*.mpp`
- Apply the `.mpp` locally with [Morphe Desktop](https://github.com/MorpheApp/morphe-desktop) like any other patch bundle.

See the [Morphe documentation](https://github.com/MorpheApp/morphe-documentation) and [patcher docs](https://github.com/MorpheApp/morphe-patcher/tree/main/docs) for patch/fingerprint APIs.

## 📁 Where future Spotify patches live

- `patches/src/main/kotlin/app/spotifycustomizer/patches/spotify/` — Spotify patches (one patch per file) + `Fingerprints.kt`.
- `patches/src/main/kotlin/app/spotifycustomizer/patches/shared/Constants.kt` — `COMPATIBILITY_SPOTIFY` (`com.spotify.music`). Pin concrete `AppTarget` versions there once verified; do not guess versions, class names, or resources.
- `patches/src/main/kotlin/util/PatchListGenerator.kt` — generates `patches-list.json` (do not edit manually).
- `extensions/extension/` — optional precompiled DEX (`extensions/extension.mpe`) for complex runtime logic; reference via `extendWith(...)`.

Current state is infrastructure only: a no-op `Placeholder patch` exists to verify the build. No real Spotify modifications are implemented yet.

## 📜 License

Spotify Customizer Patches are licensed under the [GNU General Public License v3.0](LICENSE). See [NOTICE](NOTICE): do not use the "Morphe" name/branding for this derivative work; references to Morphe are for descriptive compatibility only.
