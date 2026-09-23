package app.spotifycustomizer.patches.shared

import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    /**
     * Spotify compatibility declaration.
     *
     * No specific Spotify version is pinned yet. The single `null` target
     * means "any version" and is marked experimental until the first real
     * patch is developed and verified against a concrete Spotify release.
     *
     * When the first real patch lands, add concrete AppTarget versions here
     * (newest to oldest) instead of guessing.
     */
    val COMPATIBILITY_SPOTIFY = Compatibility(
        name = "Spotify",
        packageName = "com.spotify.music",
        description = "Spotify music client.",
        appIconColor = 0x1ED760,
        targets = listOf(
            AppTarget(
                version = null,
                isExperimental = true
            )
        )
    )
}
