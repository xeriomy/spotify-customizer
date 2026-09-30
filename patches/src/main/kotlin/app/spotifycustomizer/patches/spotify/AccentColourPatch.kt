package app.spotifycustomizer.patches.spotify

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.spotifycustomizer.patches.shared.Colors
import app.spotifycustomizer.patches.shared.Constants.COMPATIBILITY_SPOTIFY

/**
 * Recolours the app's accent from Spotify's brand green to a colour of the
 * user's choosing.
 *
 * A literal substitution rather than a list of resource names. Verified
 * against Spotify 9.1.84.2231, the brand green `1ed760` occurs 66 times
 * across `res/`: 28 times in `res/values/colors.xml` and 37 times across 23
 * drawable files, and nowhere in the dex files.
 *
 * Targeting a named subset of the colour resources was tried first and
 * rejected: it would have left the 37 green drawables alone, so checkboxes,
 * play and pause buttons and the notification indicator would stay green
 * against recoloured text. Recolouring only some of the occurrences does not
 * read as a retheme.
 *
 * Only the six RGB characters are replaced, so each value keeps its own alpha.
 * `cat_accessory_green_disabled` is `#661ed760` in unpatched Spotify and stays
 * 40% alpha whatever colour it becomes.
 *
 * This is a resource patch. It changes the app as shipped; it does not add a
 * live setting the user can change later. A runtime accent switch would need
 * a theme overlay applied per activity, because a compiled colour resource
 * cannot be changed at runtime.
 */
@Suppress("unused")
val accentColourPatch = resourcePatch(
    name = "Accent colour",
    description = "Changes the app's accent colour to a custom colour.",
    default = false
) {
    compatibleWith(COMPATIBILITY_SPOTIFY)

    val accentOption = stringOption(
        key = "Accent colour",
        default = Colors.DEFAULT_ACCENT,
        description = "A hex colour such as #RRGGBB. Applied with Spotify's own alpha values.",
        validator = { Colors.normalise(it) != null }
    )

    execute {
        val accent = requireNotNull(Colors.normalise(accentOption.value)) {
            "The 'Accent colour' option was not a valid hex colour."
        }
        val newRgb = accent.removePrefix("#")

        // Two file kinds, and deliberately not "every XML under res/".
        //
        // res/values/strings.xml contains `#1ED760` inside an HTML
        // `<font color=...>` in Spotify's legal notice, and the same string
        // exists in all 141 locale variants. Those are not the app's accent and
        // must not be recoloured. Narrowing to the two kinds that actually
        // define colour avoids touching them at all.
        val candidates = listApkEntries("res/")
            .filter { path ->
                path.endsWith("values/colors.xml") ||
                    path.contains("/drawable") && path.endsWith(".xml")
            }

        var filesChanged = 0
        var occurrences = 0

        candidates.forEach { path ->
            val file = get(path, copy = true)
            val original = file.readText()
            if (!original.contains(Colors.SPOTIFY_GREEN_RGB)) return@forEach

            val updated = Colors.replaceRgb(original, newRgb)
            if (updated != original) {
                file.writeText(updated)
                filesChanged++
                occurrences += original.windowed(Colors.SPOTIFY_GREEN_RGB.length)
                    .count { it == Colors.SPOTIFY_GREEN_RGB }
            }
        }

        if (filesChanged == 0) {
            throw PatchException(
                "No resource contained the Spotify brand green (#${Colors.SPOTIFY_GREEN_RGB}). " +
                    "Spotify has changed how its accent is defined; this patch needs updating for " +
                    "the new app version."
            )
        }
    }
}
