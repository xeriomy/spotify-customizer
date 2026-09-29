# Name change + Clone app patches

First real patches for `spotify-customizer`. Replaces the no-op `Placeholder patch`.

**Status:** planned, not started.
**Branch:** `feature/nameandclone-patch`
**Target app:** Spotify `9.1.84.2231`, **versionCode `146291969`**, verified against
`/sdcard/patches/originalapks/com.spotify.music_9.1.84.2231-146291969_minAPI24(...).apk`

> **Correction (2026-09-30).** An earlier draft of this plan stated versionCode
> `2231`. That was wrong — `2231` is a filename convention, not the real code.
> `apktool.yml` gives `versionCode: 146291969`, and the APK filename agrees. The
> plan originally also specified `versionCodes = mapOf(VersionCode.ANDROID_9 to 2231)`;
> `AppTarget.versionCodes` is keyed by `SupportedAbi`, and the patcher provides a
> `AppTarget(version, versionCode)` convenience constructor that fills every ABI.
> See T4.

---

## 1. Goal

Two patches a user can enable in the Morphe app:

- **Change app name** — set the launcher/app label to any string the user types.
- **Clone app** — rename the package so the patched Spotify installs *alongside*
  the original, without uninstalling it.

Both are `resourcePatch`es. **Neither needs a bytecode fingerprint**, so
`Fingerprints.kt` stays empty and AGENTS.md's "never guess Spotify classes" rule
is satisfied by construction: every fact below was read out of the real APK.

---

## 2. Verified facts (basis for the design)

Read with `apktool 2.12.0` and a `unzip` + DEX string scan of all 18 `classes*.dex`.

| Fact | Consequence |
|---|---|
| `res/values/strings.xml:678` → `<string name="app_name">Spotify</string>` | Name patch = one string edit |
| `app_name` exists in **exactly 1** of the 141 `values*` dirs — no locale variants | No 141-file sweep, no per-locale handling |
| Manifest `package="com.spotify.music"` | Clone patch rewrites this attribute |
| **9** `<provider>` elements under `<application>` | The real install-conflict surface |
| 8 of those 9 have `android:authorities` starting `com.spotify.music.` | Prefix rewrite handles them |
| `MediaProvider` authority is `@string/media_provider_authority` → `com.spotify.mobile.android.mediaapi` | Not a manifest literal — must rewrite the **string resource** |
| 3 `<provider>` entries under `<queries>` (`ASAA`, `amzn_appstore`, `androidx.car.app.connection`) | Visibility only. **Must not be rewritten.** Would break Samsung/Amazon/Car integrations |
| Authority suffixes `.share` (164), `.profile` (42), `.imagepicker` (3), `.pushnotificationsv2` (2), `.calimage` (2), `.vtec` (1) exist as **standalone strings in the DEX** | Authorities are built at runtime from `getPackageName() + suffix`. Renaming the package makes the code derive the new authority automatically. **No bytecode patching needed.** |
| `androidx-startup`, `early-initialization`, `mediaapi` have **0** DEX hits as literals | Same conclusion: resolved via resources / package name, not hardcoded |
| 4 custom permissions (`INTERNAL_BROADCAST`, `SECURED_BROADCAST`, `C2D_MESSAGE`, `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`) — **0 hits in all 18 dex files** | Manifest-only concern, low risk (see Risks) |
| `SECURED_BROADCAST` used once, as `android:permission` on a single element | Guard on a receiver, not something the app must hold |
| `res/values/strings.xml` contains **0** occurrences of `com.spotify.music` | No hidden string-resource package references to sweep |
| `app_name` referenced by 29 manifest elements via `@string/app_name` | One edit renames all of them consistently |
| `<manifest>` also has `android:taskAffinity` on 3 elements — none is `com.spotify.music.*` | Not a package-derived value; no action |

---

## 3. Design

### 3.1 Shared: package + authority derivation

Both patches need a validated package name. The clone patch owns it; the name
patch does not use it. A small shared helper in
`patches/src/main/kotlin/app/spotifycustomizer/patches/shared/PackageName.kt`
holds the original package constant and the rewrite rule so the two patches can
never drift.

Rewrite rule, applied to a manifest attribute value `v`:

```
v == "com.spotify.music"                      -> newPackage
v.startsWith("com.spotify.music.")            -> newPackage + v.removePrefix("com.spotify.music")
otherwise                                     -> unchanged
```

The `otherwise` branch is what keeps `<queries>` safe even if the traversal is
ever widened: `ASAA` / `amzn_appstore` / `androidx.car.app.connection` do not
start with the package prefix, so they are never touched.

### 3.2 `ChangeAppNamePatch.kt`

`resourcePatch`, one `stringOption`.

```
name        = "Change app name"
description = "Changes the name of the app to a custom name."
default     = false
option      = stringOption(
    key = "App name",
    default = "Spotify",
    description = "Name shown under the app icon. Accepts any text."
)
```

`execute`: open `document("res/values/strings.xml")`, find the `string` element
with `name="app_name"`, set its `textContent` to the option value. If the element
is missing, throw `PatchException` naming the resource — a silent no-op would
ship a patch that does nothing and looks like it worked.

**Escaping:** set `textContent` through the DOM and let the serializer escape.
Do not hand-build XML. The value is wrapped in literal double quotes so an
apostrophe in the user's input (`Bob's Spotify`) is valid Android resource
syntax, and any embedded `"` is stripped first — a raw string may not contain an
unescaped double quote. This mirrors the sanitizer logic in
`morphe-patches/util/resource/StringResourceSanitizer.kt`, reimplemented from the
Android rules rather than copied (that file carries GPLv3 §7 additional terms).

### 3.3 `CloneAppPatch.kt`

`resourcePatch`, one `stringOption` with a validator.

```
name        = "Clone app"
description = "Changes the package name so the app can be installed alongside the original."
default     = false
option      = stringOption(
    key = "Package name",
    default = "xeriomy.x.spotifyx",
    description = "New package name. Must be a valid Java package name.",
    validator = { it == null || isValidPackageName(it) && it != ORIGINAL_PACKAGE }
)
```

`execute`, in order:

1. **Manifest package.** `document("AndroidManifest.xml")`, set the root
   element's `package` attribute to the new name. This is the app id; changing
   it is what makes the two apps distinct to the installer.
2. **Provider authorities.** `getElementsByTagName("provider")`, but only those
   whose **parent chain reaches `<application>`**. Rewrite each
   `android:authorities` through the rule in 3.1. Skipping `<queries>` is
   load-bearing, not tidiness.
3. **`media_provider_authority` string resource.** The `MediaProvider` authority
   is a resource, so the manifest attribute is untouched and the *value* in
   `res/values/strings.xml` is rewritten: `com.spotify.mobile.android.mediaapi`
   → `<newPackage>.mediaapi`. The DEX has zero hardcoded `mediaapi` literals, so
   the runtime reads this same resource and stays consistent. Cross-app media
   integration (Android Auto etc.) stops working for the clone — same accepted
   trade-off as the Car App decision below.

Step 2 and step 3 must both run, or the install fails with
`INSTALL_FAILED_CONFLICTING_PROVIDER` on the untouched one.

**Default package name: `xeriomy.x.spotifyx`.** Project-specific rather than a
generic `.clone` suffix, so the clone is identifiable in the launcher, in
`adb shell pm list packages`, and in bug reports. It is only a default — the
option is editable, and the validator rejects anything that is not a valid Java
package name or that equals the original.

The validator matters beyond hygiene: the patcher itself declares
`PACKAGE_NAME_REGEX = ^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z][a-zA-Z0-9_]*)+$`, and an
invalid name produces an install that fails with no useful diagnostic. Failing
loudly at patch time is strictly better.

### 3.4 What is deliberately NOT done

- **Custom permissions are not renamed.** Renaming `com.spotify.music.permission.*`
  in the clone would make the clone declare a *different* signature-protected
  permission while `uses-permission` entries for the original names remain —
  strictly worse. The names are left alone and the risk is documented.
- **`androidx.car.app.connection` is not touched.** It is a `<queries>` entry, not
  a provider. (An earlier draft of this plan misread it as a provider and
  proposed renaming it; that was wrong and is corrected here.) Consequence:
  **Android Auto / Car App keeps working on both apps**, because the clone's
  manifest is otherwise valid for it.
- **No bytecode patching, no `Fingerprints.kt` entries, no extension.** The DEX
  evidence says authorities are derived at runtime, so there is nothing to patch.

---

## 4. Files

| Action | Path |
|---|---|
| delete | `patches/src/main/kotlin/app/spotifycustomizer/patches/spotify/PlaceholderPatch.kt` |
| delete | `extensions/extension/src/main/java/app/spotifycustomizer/extension/extension/PlaceholderExtension.java` |
| create | `patches/src/main/kotlin/app/spotifycustomizer/patches/spotify/ChangeAppNamePatch.kt` |
| create | `patches/src/main/kotlin/app/spotifycustomizer/patches/spotify/CloneAppPatch.kt` |
| create | `patches/src/main/kotlin/app/spotifycustomizer/patches/shared/PackageName.kt` |
| edit | `patches/src/main/kotlin/app/spotifycustomizer/patches/shared/Constants.kt` — pin `AppTarget("9.1.84.2231", 146291969)`, drop `isExperimental` |
| create | `.github/workflows/build.yml` — compile check on push/PR (see T0) |
| keep | `extensions/extension/` module, now unreferenced |
| keep | `patches/.../spotify/Fingerprints.kt` — still empty by design |
| **never touch** | `patches-list.json`, `patches-bundle.json`, `CHANGELOG.md`, README patches section (semantic-release owns these) |

---

## 5. Ordered tasks

- [x] **T0 — Prerequisite: a build path.** Local Gradle is blocked: no
      `~/.gradle/gradle.properties`, and the local `gh` token has `repo` +
      `workflow` but **not** `read:packages`, so plugin resolution fails. (Auth
      is now wired: Termux's `gh`/`git` configs were copied into `/root` so
      `gh auth status` reports `xeriomy`.)

      **The existing `release.yml` is not a build path for feature branches.**
      Verified from run `36183971931`: on a `dev` push the `Release` step fails
      with `SemanticReleaseError: The release branches are invalid in the
      branches configuration`, which aborts the job *before* `Verify project
      compiles` — that step reports `skipped`, not `success`. So pushing this
      branch proves nothing.

      Fix: add `.github/workflows/build.yml`, modelled on the patcher's own
      `build_pull_request.yml` — triggers on `push` and `pull_request`, runs
      `./gradlew :patches:buildAndroid` with `GITHUB_TOKEN`, and `continue-on-error`
      is *not* used so a real compile failure is visible. The automatic
      `GITHUB_TOKEN` carries `packages: read`, which is what the Morphe registry
      needs.
- [x] **T1 — Write `PackageName.kt`.** `ORIGINAL_PACKAGE`, `isValidPackageName`,
      `rewritePackageDerivedValue(value, newPackage)`. Pure functions.
      *Also needed* `Dom.kt` with `getAndroidAttribute` / `setAndroidAttribute` —
      see the namespace note in §9.
- [x] **T2 — Write `ChangeAppNamePatch.kt`.** Includes the quote-stripping helper.
- [x] **T3 — Write `CloneAppPatch.kt`.** Manifest package, `<application>`-scoped
      provider sweep, `media_provider_authority` rewrite. Throws `PatchException`
      if no provider authority was rewritten, so a future Spotify release that
      renames them fails loudly instead of shipping a clone that cannot install.
- [x] **T4 — Pin the verified version in `Constants.kt`.** `AppTarget("9.1.84.2231", 146291969)`.
- [x] **T5 — Delete the two placeholder files.**
- [x] **T6 — Verify compile via the T0 workflow.** Run `36640984138`: `Build
      patches` green, produced `patches-1.0.0.mpp`. Inspected the bundle: both
      patch classes present with their option keys, no `PlaceholderPatch`
      class, `xeriomy.x.spotifyx` default and versionCode `146291969` both
      present in the bytecode.
- [ ] **T7 — Apply and test.** Download the artifact from run `36640984138`
      (`patches-1.0.0.mpp`) or rebuild locally once registry auth exists, apply
      it to the 9.1.84.2231 APK with Morphe Desktop, then verify on device:
      - clone installs while the original is present (the actual acceptance test)
      - both launch, both can sign in
      - share sheet, profile picture, playlist artwork do not crash
        ← this is the specific regression the "manifest-only" decision accepts
      - Android Auto still works on the clone
      - launcher shows the custom name
- [ ] **T8 — Commit** on this branch with a `feat:` message. Merge to `dev`
      (no squash) only after T7 passes.

---

## 6. Decisions

| Decision | Choice | Why |
|---|---|---|
| Clone scope | manifest package + all 9 `<application>` provider authorities + `media_provider_authority` | Minimum that survives `INSTALL_FAILED_CONFLICTING_PROVIDER` |
| Option defaults | `"Spotify"` / `"xeriomy.x.spotifyx"`, both editable | Patch works if the user enables it and types nothing; project-specific name is identifiable in `pm list packages` and bug reports |
| Placeholder | delete both files, keep the `extensions/` module | Module is template scaffolding for future patches |
| Patch type | `resourcePatch`, no bytecode | Verified: authorities derive at runtime from the package name |
| `androidx.car.app.connection` | leave alone | It is a `<queries>` entry; renaming would break Car App visibility for no install benefit |
| Patch enabled by default | no | Both are opt-in; a clone the user did not ask for is surprising |

---

## 7. Risks

1. **Runtime-authority assumption (accepted).** The conclusion that
   `getPackageName() + suffix` is how Spotify builds authorities rests on the
   suffixes existing as standalone DEX strings, not on reading the call sites.
   T7's share/profile/artwork checks are the real test. *If it is wrong*, the fix
   is a fingerprint on the `FileProvider` call sites — the first fingerprint in
   this repo, and a larger piece of work. Rollback is clean: the patch is
   isolated in `CloneAppPatch.kt`.
2. **Signature-protected permissions (accepted, low).** The clone declares
   `com.spotify.music.permission.*` with `protectionLevel="signature"`. Signed
   with a different key, the clone may be denied permissions the original owns.
   Impact is bounded: those names appear **0 times** in the DEX and
   `SECURED_BROADCAST` guards exactly one manifest element. Worst case is a
   rejected internal broadcast. Documented in the patch KDoc.
3. **Version coupling.** `media_provider_authority` and the authority suffixes
   are Spotify-internal. A future release could rename them and the patch would
   silently stop renaming. T3's "did we rewrite at least one authority" assertion
   catches the loud case; the `MediaProvider` resource is the one that could
   still be missed. The pinned `AppTarget` in T4 bounds the blast radius.
4. **CI is compile-only.** Even once T0's workflow exists, a green run proves
   the Kotlin compiles and the `.mpp` is produced. It does not prove the patch
   applies to Spotify, which is T7's job on a real device.
5. **Device testing is manual (T7).** No emulator instrumentation in this repo.
   T7 is a checklist for a human with a device.

---

## 8. Assumptions

- Morphe Patcher `1.13.0` / plugins `app.morphe.patches:1.3.4`, per
  `gradle/libs.versions.toml` and `settings.gradle.kts` — verified against the
  patcher's own docs and `Option.kt` / `ResourcePatchContext.kt` sources.
- `document(path)` and `get(path, copy)` are the correct `ResourcePatchContext`
  entry points; confirmed in `ResourcePatchContext.kt`.
- `stringOption(key, default, description, required, validator)` exists with that
  signature; confirmed in `Option.kt`.
- The `Document` serializer escapes `textContent` correctly for both platforms.
  Note in the patcher's own source: Android gets UTF-16 output written as UTF-8
  specifically to avoid escaping surrogate pairs. This is the patcher's
  responsibility, not ours.
- `AppTarget("9.1.84.2231", 146291969)` — **confirmed** against
  `Compatibility.kt` in patcher `main`. `versionCodes` is keyed by `SupportedAbi`
  (not a `VersionCode` enum, as an earlier draft of this plan assumed); the
  two-arg constructor fills all four ABIs.
- `resourcePatch(name, description, default, block)` and
  `stringOption(key, default, description, required, validator)` — **confirmed**
  against `Patch.kt` and `Option.kt`. Note the option's first parameter is
  `key`, not `name` as the public docs show.
- Patcher version drift: the docs and sources read are from `morphe-patcher`
  `main`, while the build resolves `1.13.0`. T6's CI run passed, so the
  signatures used are correct for `1.13.0`.
- The clone is signed by the user (Morphe Desktop), not by Spotify, so the two
  apps genuinely coexist.

---

## 9. Notes for future patches

### 9.1 `android:` attributes are not namespace-aware

Morphe parses manifests and resources with
`DocumentBuilderFactory.newInstance()`, which has `isNamespaceAware() == false`
by JAXP default. In such a document an `android:authorities` attribute carries
**no** namespace URI and its node name is the literal string
`android:authorities`.

Consequences:

- `getAttribute("authorities")` returns `""` — it looks for an unprefixed name.
- `getAttributeNS(ANDROID_NS, "authorities")` also returns `""`.
- `getAttribute("android:authorities")` is the one that works.
- `setAttributeNS(ANDROID_NS, "android:authorities", v)` would **add a second
  attribute** rather than replace the existing one, producing a manifest with
  two `authorities` attributes and no obvious error.

The first draft of `CloneAppPatch.kt` had exactly this bug: it read with
`getAttributeNS` (always empty, so nothing would have been rewritten — caught by
the `rewrittenAuthorities == 0` assertion) and wrote with `setAttributeNS`
(which would have duplicated the attribute). Neither failure is loud.

`Dom.kt` now centralises this in `getAndroidAttribute` / `setAndroidAttribute`,
which try both spellings and write through whichever the document actually
uses. Any future patch that touches a namespaced attribute should use those
helpers rather than calling the DOM directly.

### 9.2 Verified against a real APK, not just compiled

Compiling proves the Kotlin is valid; it does not prove the patch does the
right thing to Spotify. The rewrite logic was replayed against the decoded
9.1.84.2231 manifest and `strings.xml` before committing:

- 8 provider authorities rewritten, all under `<application>`
- 3 `<queries>` providers left untouched
  (`ASAA`, `amzn_appstore`, `androidx.car.app.connection`)
- `media_provider_authority` rewritten to `<newPackage>.mediaapi`

The string-escaping rule was checked against aapt2's own output: Spotify's
`strings.xml` contains 728 quote-delimited raw strings, 682 of which contain
apostrophes and none of which has an unescaped inner double quote. That is
exactly the rule `escapeAndroidStringResource` implements.
