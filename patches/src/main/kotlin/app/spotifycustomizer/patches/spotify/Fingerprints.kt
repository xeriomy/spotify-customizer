package app.spotifycustomizer.patches.spotify

import app.morphe.patcher.fingerprint.Fingerprint
import app.morphe.patcher.fingerprint.methodCall
import app.morphe.patcher.fingerprint.string

/**
 * Matches the method that classifies which kind of process the app runs in.
 *
 * Spotify refuses to start in a process whose name it does not recognise. The
 * method reads the current process name and compares it against a three-entry
 * allowlist — the original package name, `<package>.gdbprocess`, and
 * `robolectric.ui` — throwing
 * `AssertionError("The process name ... is not allowed to start")` when none
 * match. It is reached from `EarlyInitializationProvider.onCreate`, so the app
 * dies before any UI appears.
 *
 * The comparison is a prefix test rather than an equality test, but the patch
 * replaces the literal outright rather than relying on that: prefix behaviour
 * is an implementation detail of an obfuscated helper, and a patch that breaks
 * when Spotify tightens the comparison is worse than one that does not.
 *
 * Only the return type and string constants are matched. The declaring class is
 * obfuscated (`p.x35` in 9.1.84.2231) and renames between releases, whereas
 * these strings are part of the app's behaviour and do not. The assertion text
 * occurs exactly once across all 18 dex files, which is what makes the match
 * unambiguous.
 *
 * Verified against Spotify 9.1.84.2231 (versionCode 146291969).
 */
internal val processNameAllowlistFingerprint = Fingerprint(
    name = "processNameAllowlistFingerprint",
    returnType = "Lp/zhr0;",
    filters = listOf(
        // The allowlist entry for the main process. This is the literal the
        // clone patch replaces, and it is the first filter so its match index
        // is instructionMatches[0].
        string("com.spotify.music"),
        // Reads the process name at runtime via the public API.
        methodCall(
            definingClass = "Landroid/app/Application;",
            name = "getProcessName",
            returnType = "Ljava/lang/String;"
        ),
        // The second allowlist entry.
        string(".gdbprocess"),
        // The third, only ever reached under Robolectric.
        string("robolectric.ui"),
        // The failure message. Unique in the APK.
        string("The process name "),
        string(" is not allowed to start"),
    )
)
