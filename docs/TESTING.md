# Testing a patch

Every change to a patch goes through the same loop. It is slow, because most of
the time is spent waiting for a build and then watching a phone, so it is worth
doing deliberately.

## The loop

1. **Change the patch code** on a feature branch, never on `dev` directly.
2. **Push.** The `Build` workflow compiles the bundle and uploads
   `patches-*.mpp` as an artifact. It also runs the unit tests.
3. **Download the artifact** from the run's summary page, or wait for CI to be
   green before touching a device.
4. **Apply** the `.mpp` to a base APK with Morphe Desktop or Manager.
5. **Test on a device** against the checklist below.
6. **Merge** to `dev` with a semantic commit message. `dev` gets a pre-release;
   merging `dev` into `main` produces the stable release.

Local Gradle usually will not work: resolving the Morphe Maven registry needs a
GitHub token with `read:packages`, which is not configured in a plain checkout.
Use CI, and read a local `./gradlew` failure as an auth problem rather than a
code problem.

## Deriving fingerprints

There are currently no fingerprints, because neither shipped patch modifies
bytecode. When one is needed:

- Never guess a class, method, string or resource name. Read it out of a
  legally obtained APK with `jadx` or `apktool`.
- Match on behaviour that is stable across releases — return types, string
  constants, called APIs — never on obfuscated names.
- Record the app version the fingerprint was verified against, and add it to
  the `AppTarget` list in `Constants.kt`.
- If a string is unique in the whole APK, say so in a comment. That is what
  makes a match unambiguous and worth relying on.

`unzip` the APK and grep the `classes*.dex` files directly when you need to
know whether a string appears at all. Two mistakes made here are written up
below, because both look like certainty and are not:

- A literal search cannot see a name assembled at runtime. AndroidX
  `ContextCompat` builds `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` as
  `getPackageName() + "." + name`, and grepping for the full name finds nothing.
- `grep -r` silently skips resource files it decides are binary. Counting
  occurrences with it understated the accent colour's real surface by roughly
  4x. Walk the tree in a script instead.

## Checking before pushing

The unit tests cover the pure logic. Replaying the rewrite against a decoded APK
covers the rest, and catches things compiling never will:

```sh
apktool d -f -o /tmp/apk base.apk          # decode once, keep it around
# apply the same rule the patch applies, over /tmp/apk/res, and print the counts
```

Two rules earned by doing this:

- **Assert what must be found.** Each patch throws if it rewrote nothing, so a
  Spotify release that renames things fails loudly instead of shipping a
  no-op.
- **Assert what must be left alone.** Check that untouched files are
  byte-identical, not merely that changed files look right. The accent colour
  patch is scoped away from `strings.xml` precisely because that file holds
  Spotify's legal notice, and a test now pins the scoping rule.

## Device checklist

- [ ] The patched app installs
- [ ] It launches
- [ ] For a clone: the original is still installed and still works
- [ ] The setting actually changed what it should
- [ ] Nothing else changed by accident — check a screen the patch should not
      have touched
- [ ] Uninstall and reinstall to confirm the change persists

## What has gone wrong so far

Three real failures, each of which is now a test or a guard.

**`INSTALL_FAILED_DUPLICATE_PERMISSION`.** Renaming the package is not enough:
two installed apps may not both *define* the same permission. Spotify declares
four, and each appears in three places — the `<permission>` declaration, the
`<uses-permission>` self-request, and an `android:permission` guard. All three
must move together or the component stops accepting its own broadcasts. The
plan had this as "accepted, low risk", reasoning only about runtime effects and
missing that Android rejects the install outright.

**`INSTALL_FAILED_CONFLICTING_PROVIDER`.** Provider authorities collide first,
and are derived from the package name at runtime, so renaming the manifest is
enough — but only because the suffixes exist as standalone strings in the dex.
`android:name` is a trap in the same patch: it is a permission name on
`<permission>` and `<uses-permission>`, and a *class name* everywhere else.
Rewriting the class names points the manifest at classes that do not exist.

**`AssertionError: The process name ... is not allowed to start`.** Spotify
classifies the running process against a hardcoded three-entry allowlist before
initialising, and an app's process name is its package name unless the manifest
says otherwise. The manifest-only fix is to set `android:process` on
`<application>`; two installed apps may share a process name because processes
are keyed by name *and* uid.

That last one also cost a detour worth recording: a bytecode patch was written
to rewrite the allowlist literal, and its `dependsOn` edge produced a
`NullPointerException` in the manager before patching began. Two top-level
declarations in different files referenced each other, and each file's static
initialiser assigns its property last, so one read the other as `null`. The
manifest route removed the second patch entirely.
