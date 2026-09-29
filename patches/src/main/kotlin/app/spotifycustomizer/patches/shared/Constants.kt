package app.spotifycustomizer.patches.shared

import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    /**
     * Spotify compatibility declaration.
     *
     * Pinned to the release the patches in this collection were developed
     * against, read from a legally obtained APK:
     * Spotify 9.1.84.2231, versionCode 146291969, minSdk 24.
     *
     * The two-argument `AppTarget` constructor fills `versionCodes` for every
     * supported ABI, which is what a universal APK needs. The target is no
     * longer experimental, so the manager will offer it normally.
     *
     * Add newer versions above this one as they are verified; do not guess.
     */
    val COMPATIBILITY_SPOTIFY = Compatibility(
        name = "Spotify",
        packageName = "com.spotify.music",
        description = "Spotify music client.",
        appIconColor = 0x1ED760,
        targets = listOf(
            AppTarget("9.1.84.2231", 146291969)
        )
    )
}
