package app.spotifycustomizer.patches.spotify

// No fingerprints. Neither patch modifies bytecode, and a patch that does not
// have one is stronger for it: nothing to break when Spotify is obfuscated
// differently next release.
//
// This file is not dead weight, though. Spotify does have checks that a future
// patch may need to reach, and the notable one is in `p.x35.L()` in
// classes3.dex of 9.1.84.2231: it reads the process name via
// `Application.getProcessName()` and throws
// `AssertionError("The process name ... is not allowed to start")` unless it
// is `com.spotify.music`, `<package>.gdbprocess` or `robolectric.ui`.
//
// The clone patch sidesteps that from the manifest instead of patching the
// allowlist: see `android:process` in `CloneAppPatch`. Fingerprinting it would
// work, and the string constants are stable enough to match on, but it would
// cost a second patch (a patch has exactly one type, and the manifest work
// needs a resource patch) and therefore a second entry in the user's list.
//
// If a future change does need to reach it, match on the return type plus the
// six string constants in `p.x35.L()`, declared in the order they appear in the
// method. The assertion text occurs exactly once across all 18 dex files.
//
// Do not guess class names, method names, strings or opcodes here. Every
// fingerprint must be derived from a legally obtained Spotify APK using jadx or
// Morphe tooling, and must record the app version it was verified against.
