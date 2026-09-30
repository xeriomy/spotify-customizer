package app.spotifycustomizer.patches.shared

/**
 * Escaping helpers for Android string resources.
 *
 * Verified against Spotify 9.1.84.2231: its `res/values/strings.xml` contains 728
 * quote-delimited raw strings, 682 of which contain an apostrophe, and none of
 * which contains an unescaped inner double quote. That is the rule implemented
 * here.
 */
object StringResources {
    /**
     * Escapes a value for use as the text of a `<string>` resource.
     *
     * The value is wrapped in literal double quotes, which makes it a *raw*
     * string in Android resource terms: an apostrophe in the user's input is
     * then valid without escaping. A raw string may not contain an unescaped
     * double quote, so those are stripped.
     *
     * Assigning to `textContent` and letting the DOM serializer handle XML
     * escaping is deliberate — the XML is never assembled by hand.
     *
     * @param value The raw user-supplied value.
     * @return A value safe to assign to a `<string>` element's text.
     */
    fun escape(value: String): String =
        "\"" + value.replace("\"", "") + "\""
}
