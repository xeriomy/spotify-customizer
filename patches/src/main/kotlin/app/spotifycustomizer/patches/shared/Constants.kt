package app.spotifycustomizer.patches.shared

import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    /**
     * The Spotify release this collection was last verified against, read from
     * a legally obtained APK: versionCode 146291969, minSdk 24.
     *
     * Add newer versions as they are verified; do not guess.
     */
    private const val VERIFIED_VERSION = "9.1.84.2231"
    private const val VERIFIED_VERSION_CODE = 146291969

    /**
     * Spotify compatibility declaration.
     *
     * Two targets, and the distinction matters:
     *
     *  - the version this collection was verified against, and
     *  - a catch-all, so the patches are still offered after Spotify updates
     *    instead of the whole collection going dark until someone re-verifies
     *    against a fresh APK.
     *
     * The catch-all is safe because the patches verify themselves. Each one
     * throws a `PatchException` naming exactly what went missing when Spotify's
     * internals shift — no provider authority was rewritten, no permission
     * declaration was renamed, a required string resource is gone. So an
     * unexpected version either applies correctly or fails loudly and visibly,
     * rather than producing an APK that looks fine and breaks on device.
     *
     * The two-argument `AppTarget` constructor fills `versionCodes` for every
     * supported ABI, which is what a universal APK needs. It also hardcodes
     * `description = null`, so only the catch-all can carry a description; the
     * user-facing note below is what makes the caveat visible.
     */
    val COMPATIBILITY_SPOTIFY = Compatibility(
        name = "Spotify",
        packageName = "com.spotify.music",
        description = "Spotify music client.",
        appIconColor = 0x1ED760,
        targets = listOf(
            AppTarget(VERIFIED_VERSION, VERIFIED_VERSION_CODE),
            AppTarget(
                version = null,
                description = "Not verified. These patches check for what they depend " +
                    "on and fail with a clear error if Spotify has changed it."
            )
        )
    )
}
