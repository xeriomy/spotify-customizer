package app.spotifycustomizer.patches.shared

/**
 * Helpers for rewriting Android colour values.
 *
 * Verified against Spotify 9.1.84.2231: the brand green `1ed760` appears 66
 * times across `res/` — 28 in `res/values/colors.xml` and 37 across 23
 * drawable files, plus one raw JSON. It appears nowhere in the dex files, so
 * no bytecode work is involved.
 *
 * Values are written as `#RRGGBB` or `#AARRGGBB`, so the accent is rewritten by
 * replacing the six RGB characters and leaving any alpha prefix alone. That is
 * what keeps a translucent state translucent: `cat_accessory_green_disabled`
 * is `#661ed760` and stays 40% alpha whatever colour it becomes.
 */
object Colors {
    /** Spotify's brand green as it appears in the APK. */
    const val SPOTIFY_GREEN_RGB = "1ed760"

    /** The default accent, so enabling the patch without typing anything is a no-op. */
    const val DEFAULT_ACCENT = "#$SPOTIFY_GREEN_RGB"

    /**
     * Matches the brand green as the RGB tail of an Android colour literal,
     * capturing any alpha in front of it.
     *
     * The three forms that occur in 9.1.84.2231 are `#1ed760`, `#661ed760` and
     * `#ff1ed760`, so matching only a bare `#1ed760` finds one of the three and
     * silently misses the rest. The lookahead rejects a match followed by more
     * hex characters, which would be a different, longer colour that merely
     * contains these digits.
     *
     * Case-sensitive on purpose. aapt2 emits lowercase, and the one uppercase
     * occurrence of the brand colour in the whole APK is `#1ED760` inside an
     * HTML `<font color=...>` in Spotify's legal notice, which this patch must
     * not touch.
     */
    private val SPOTIFY_GREEN_IN_COLOUR = Regex(
        "#([0-9a-fA-F]{0,2})$SPOTIFY_GREEN_RGB(?![0-9a-fA-F])"
    )

    /**
     * Accepts `#RRGGBB` and `#AARRGGBB`, with or without the leading `#`,
     * in either case.
     */
    private val HEX_COLOR = Regex("^#?([0-9a-fA-F]{6}|[0-9a-fA-F]{8})$")

    /**
     * Normalises a user-supplied colour to lowercase `#RRGGBB`, discarding any
     * alpha the user typed, since the accent is applied with each resource's
     * own alpha.
     *
     * @param value The user-supplied value.
     * @return The normalised colour, or null when [value] is not a hex colour.
     */
    fun normalise(value: String?): String? {
        // matchEntire rather than matchEntries: the patcher's Kotlin stdlib
        // predates matchEntries, and HEX_COLOR is anchored anyway.
        val match = HEX_COLOR.matchEntire(value?.trim() ?: return null) ?: return null

        val digits = match.groupValues[1]
        val rgb = if (digits.length == 8) digits.substring(2) else digits

        return "#${rgb.lowercase()}"
    }

    /**
     * Rewrites the RGB of every Android colour literal in [current] that uses
     * Spotify's brand green, keeping each value's own alpha.
     *
     * `#ff1ed760` becomes `#ff654321`, `#661ed760` becomes `#66654321`, and a
     * bare `#1ed760` becomes `#654321`. A run of text that is not a colour
     * literal is left alone — an attribute reference such as
     * `@color/spotifybrand_essential_base` resolves elsewhere and must not be
     * rewritten here.
     *
     * @param current The value as it appears in the file.
     * @param newRgb The replacement RGB, six hex characters, no leading `#`.
     * @return The rewritten value, or [current] when it holds no such literal.
     */
    fun replaceRgb(current: String, newRgb: String): String =
        SPOTIFY_GREEN_IN_COLOUR.replace(current) { match ->
            "#${match.groupValues[1]}$newRgb"
        }
}
