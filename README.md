# Spotify Customizer Patches

Independent third-party patch collection for the Spotify Android app, built on the Morphe patching ecosystem.

## ❓ About

This is a **patch project**. It produces patches (`.mpp` bundle) that can later modify a obtained Spotify Android APK via Morphe tooling.

- Independent third-party project. Not affiliated with Spotify AB or the Morphe open-source project.
- Uses the Morphe patching ecosystem: `MorpheApp/morphe-patcher` as an external Gradle plugin/dependency (see `settings.gradle.kts`, `gradle/libs.versions.toml`). The patcher source is not copied into this repo.
- No Premium / DRM / subscription bypass functionality is provided or planned. Focus is on harmless UI/customization experimentation.
- No Spotify APKs, patched binaries, or keystores are committed to this repo.

### How to use these patches

Click here to add these patches to Morphe: https://morphe.software/add-source?github=xeriomy/spotify-customizer

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->
> **[v1.0.0](https://github.com/xeriomy/spotify-customizer/releases/tag/v1.0.0)**&nbsp;&nbsp;•&nbsp;&nbsp;`main`&nbsp;&nbsp;•&nbsp;&nbsp;2 patches total
<details open>
<summary>📦 Spotify&nbsp;&nbsp;•&nbsp;&nbsp;2 patches</summary>
<br>

**🎯 Supported versions:**

| 9.1.84.2231 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Change app name](#change-app-name) | Changes the name shown under the app icon to a custom name. | • App name |
| [Clone app](#clone-app) | Changes the package name so the app installs alongside the original. | • Package name |

</details>

<!-- PATCHES_END -->

## 🛠️ Contributing

See [docs/TESTING.md](docs/TESTING.md) for the patch → build → device-test loop,
how to derive a fingerprint from an APK, and the three failures this collection
has already hit, so the same ground does not get covered twice.

## 📜 License

Spotify Customizer Patches are licensed under the [GNU General Public License v3.0](LICENSE). See [NOTICE](NOTICE): do not use the "Morphe" name/branding for this derivative work; references to Morphe are for descriptive compatibility only.
