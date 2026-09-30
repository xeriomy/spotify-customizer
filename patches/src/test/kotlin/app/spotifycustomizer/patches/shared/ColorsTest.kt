package app.spotifycustomizer.patches.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ColorsTest {
    // --- normalise ---

    @Test
    fun acceptsSixDigitHex() {
        assertEquals("#654321", Colors.normalise("#654321"))
        assertEquals("#654321", Colors.normalise("654321"))
    }

    @Test
    fun acceptsEightDigitHexAndDropsTheAlpha() {
        // The accent is applied with each resource's own alpha, so anything the
        // user types is discarded.
        assertEquals("#654321", Colors.normalise("#80654321"))
        assertEquals("#654321", Colors.normalise("80654321"))
    }

    @Test
    fun lowercases() {
        assertEquals("#aabbcc", Colors.normalise("#AABBCC"))
        assertEquals("#aabbcc", Colors.normalise("#FFAABBCC"))
    }

    @Test
    fun trimsSurroundingWhitespace() {
        assertEquals("#654321", Colors.normalise("  #654321  "))
    }

    @Test
    fun rejectsAnythingElse() {
        listOf(
            null,
            "",
            "#12345",       // five digits
            "#1234567",     // seven digits
            "#123456789",   // nine digits
            "red",
            "#gggggg",
            "#654321 extra",
            "0x654321",
        ).forEach { assertNull(Colors.normalise(it), "expected '$it' to be rejected") }
    }

    @Test
    fun theDefaultAccentIsSpotifyGreen() {
        // Enabling the patch without typing anything must be a no-op, not a
        // surprise recolour.
        assertEquals("#1ed760", Colors.normalise(Colors.DEFAULT_ACCENT))
    }

    // --- replaceRgb ---

    /**
     * The three literal forms that actually occur in Spotify 9.1.84.2231:
     * 64 of `#ff1ed760`, 1 of `#661ed760` and 3 of `#1ed760`. An earlier
     * version of this rule matched only the last of those and rewrote nothing
     * in practice, which the replay against the APK caught.
     */
    @Test
    fun rewritesEveryFormThatOccursInTheApk() {
        assertEquals("#ff654321", Colors.replaceRgb("#ff1ed760", "654321"))
        assertEquals("#66654321", Colors.replaceRgb("#661ed760", "654321"))
        assertEquals("#654321", Colors.replaceRgb("#1ed760", "654321"))
    }

    @Test
    fun ignoresUppercaseSoLegalTextIsUntouched() {
        // Spotify's legal notice contains `#1ED760` inside an HTML font tag.
        // aapt2 emits lowercase for real colour literals, so matching case
        // sensitively is both sufficient and keeps that string intact.
        assertEquals("#1ED760", Colors.replaceRgb("#1ED760", "654321"))
    }

    @Test
    fun preservesTheAlphaPrefix() {
        // cat_accessory_green_disabled is 40% alpha in unpatched Spotify and
        // must stay translucent whatever colour it becomes.
        assertEquals("#66654321", Colors.replaceRgb("#661ed760", "654321"))
    }

    @Test
    fun replacesEveryOccurrenceInAValue() {
        assertEquals(
            "#ff654321,#ff654321",
            Colors.replaceRgb("#ff1ed760,#ff1ed760", "654321")
        )
    }

    @Test
    fun leavesValuesWithoutTheLiteralAlone() {
        listOf(
            "#ff000000",
            "#ffffffff",
            "@color/spotifybrand_essential_base",
            "?attr/colorPrimary",
            "1ed760",              // no leading hash, so not a colour literal
            "color1ed760",         // not a colour at all
            "#ff1ed761",           // a different colour
            "#1ed7600",            // longer run: the digits are not the RGB tail
        ).forEach { value ->
            assertEquals(value, Colors.replaceRgb(value, "654321"), "expected '$value' untouched")
        }
    }

    @Test
    fun isANoOpWhenTheAccentIsTheOriginalGreen() {
        assertEquals("#ff1ed760", Colors.replaceRgb("#ff1ed760", "1ed760"))
    }
}
