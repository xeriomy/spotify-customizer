package app.spotifycustomizer.patches.spotify

import app.morphe.patcher.patch.bytecodePatch
import app.spotifycustomizer.patches.shared.Constants.COMPATIBILITY_SPOTIFY

/**
 * Placeholder patch to verify the patch collection builds end to end.
 *
 * Does nothing at patch time (no fingerprints, no bytecode edits).
 * Future Spotify patches will live in this package, following the
 * current Morphe conventions:
 *
 * - one patch per file, named after what it does,
 * - fingerprints in `Fingerprints.kt`,
 * - shared compatibility in `...patches.shared.Constants`.
 */
@Suppress("unused")
val placeholderPatch = bytecodePatch(
    name = "Placeholder patch",
    description = "Placeholder patch to verify the patch collection builds. Does nothing.",
    default = false
) {
    compatibleWith(COMPATIBILITY_SPOTIFY)

    extendWith("extensions/extension.mpe")

    execute {
        // Intentionally a no-op. Do not add Spotify fingerprints here
        // until a real, harmless UI patch is designed.
    }
}
