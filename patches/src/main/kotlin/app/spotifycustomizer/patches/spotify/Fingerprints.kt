package app.spotifycustomizer.patches.spotify

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

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
 * `cloneAppProcessNamePatch` rewrites the package-derived literal to the new
 * name, so the renamed process is accepted.
 *
 * Only the return type and string constants are matched. The declaring class is
 * obfuscated (`p.x35` in 9.1.84.2231) and renames between releases, whereas
 * these strings are part of the app's behaviour and do not. The assertion text
 * occurs exactly once across all 18 dex files, which is what makes the match
 * unambiguous.
 *
 * The filters are declared in the order the instructions appear in the method,
 * which the patcher requires: the package literal, then the process-name read,
 * then the other two allowlist entries, then the failure message.
 *
 * Verified against Spotify 9.1.84.2231 (versionCode 146291969).
 */
internal val processNameAllowlistFingerprint = Fingerprint(
    returnType = "Lp/zhr0;",
    filters = listOf(
        // The allowlist entry for the main process. This is the literal the
        // clone patch replaces, and it is the first filter so its match is
        // `instructionMatches[0]`.
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
