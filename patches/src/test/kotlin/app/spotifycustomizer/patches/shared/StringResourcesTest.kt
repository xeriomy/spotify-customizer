package app.spotifycustomizer.patches.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for Android string resource escaping.
 *
 * The rule mirrors what aapt2 accepted in Spotify 9.1.84.2231: a value wrapped
 * in literal double quotes is a *raw* string, so an apostrophe needs no
 * escaping, but an unescaped inner double quote is not allowed. Checked
 * against the APK: 728 quote-delimited raw strings, 682 containing an
 * apostrophe, none with an unescaped inner quote.
 */
class StringResourcesTest {
    @Test
    fun wrapsPlainTextInQuotes() {
        assertEquals("\"Spotify\"", StringResources.escape("Spotify"))
    }

    @Test
    fun leavesAnApostropheIntact() {
        // The whole point of wrapping: a raw string may contain an apostrophe.
        assertEquals("\"Bob's Spotify\"", StringResources.escape("Bob's Spotify"))
    }

    @Test
    fun stripsInnerDoubleQuotes() {
        // A raw string may not contain an unescaped double quote.
        assertEquals("\"ab\"", StringResources.escape("a\"b\""))
        assertEquals("\"\"", StringResources.escape("\"\""))
    }

    @Test
    fun escapesAValueThatIsOnlyQuotes() {
        val result = StringResources.escape("\"\"\"")
        assertTrue(result.startsWith("\"") && result.endsWith("\""))
        assertEquals("\"\"", result)
    }

    @Test
    fun handlesAnEmptyValue() {
        assertEquals("\"\"", StringResources.escape(""))
    }

    @Test
    fun handlesAnAlreadyRawLookingValue() {
        // The user typing quotes into the option should not produce a nested
        // raw string; the quotes are stripped rather than escaped.
        assertEquals("\"already wrapped\"", StringResources.escape("\"already wrapped\""))
    }

    @Test
    fun preservesSpecialXmlCharactersAndTheTextBetweenThem() {
        // XML escaping is the DOM serializer's job, not ours. These characters
        // must survive untouched so the serializer can handle them; only the
        // double quotes are removed.
        val input = "A&B <tag> \"q\" 'a'"
        assertEquals("\"A&B <tag> q 'a'\"", StringResources.escape(input))
    }

    @Test
    fun preservesNewlines() {
        // A newline is legal in a raw string and must not be mangled here.
        assertEquals("\"line1\nline2\"", StringResources.escape("line1\nline2"))
    }

    @Test
    fun outputIsAlwaysDelimitedByExactlyTwoQuotes() {
        listOf("a", "a\"b", "\"", "Bob's", "", "x\"\"y").forEach { input ->
            val result = StringResources.escape(input)
            assertTrue(result.length >= 2, "too short for '$input'")
            assertTrue(result.startsWith("\""), "missing open quote for '$input'")
            assertTrue(result.endsWith("\""), "missing close quote for '$input'")
            // No unescaped inner quote, which is what makes it a valid raw string.
            val inner = result.substring(1, result.length - 1)
            assertTrue(!inner.contains("\""), "inner quote survived for '$input'")
        }
    }
}
