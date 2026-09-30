package app.spotifycustomizer.patches.shared

import org.w3c.dom.Document
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for the DOM helpers.
 *
 * Morphe parses documents with `DocumentBuilderFactory.newInstance()`, which is
 * **not** namespace-aware. In such a document an `android:authorities`
 * attribute carries no namespace URI and its node name is the literal
 * `android:authorities`. The first draft of the clone patch used
 * `getAttributeNS` / `setAttributeNS`, which silently read nothing and would
 * have *added* a second attribute rather than replacing the first. These tests
 * pin the behaviour that prevents a repeat.
 */
class DomTest {
    private fun parse(xml: String): Document =
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(xml.byteInputStream())

    private val sample = """
        <manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.spotify.music">
          <application>
            <provider android:authorities="com.spotify.music.share" android:name="a.B"/>
            <receiver android:permission="com.spotify.music.permission.SECURED_BROADCAST"/>
            <plain value="untouched"/>
          </application>
        </manifest>
    """.trimIndent()

    private fun provider(doc: Document): Element =
        doc.getElementsByTagName("provider").item(0) as Element

    @Test
    fun readsAPrefixedAttribute() {
        assertEquals("com.spotify.music.share", provider(parse(sample)).getAndroidAttribute("authorities"))
    }

    @Test
    fun returnsNullForAMissingAttribute() {
        val plain = parse(sample).getElementsByTagName("plain").item(0) as Element
        assertNull(plain.getAndroidAttribute("authorities"))
    }

    /**
     * The bug this exists to prevent: the old value must be replaced, not
     * shadowed by a second attribute.
     */
    @Test
    fun replacesInPlaceRatherThanDuplicating() {
        val element = provider(parse(sample))

        element.setAndroidAttribute("authorities", "com.spotify.music.xeriomy.share")

        assertEquals(1, countRawAttributes(element, "authorities"))
        assertEquals("com.spotify.music.xeriomy.share", element.getAndroidAttribute("authorities"))
    }

    @Test
    fun setAndroidAttributeDoesNotCreateAMissingAttribute() {
        // Deliberate: callers use this only for values they have already read,
        // so a missing attribute means Spotify changed and should be loud.
        val plain = parse(sample).getElementsByTagName("plain").item(0) as Element

        plain.setAndroidAttribute("authorities", "should-not-appear")

        assertNull(plain.getAndroidAttribute("authorities"))
    }

    @Test
    fun putAndroidAttributeCreatesAMissingAttribute() {
        // android:process is absent in unpatched Spotify, so this is the one
        // helper that may add.
        val application = parse(sample).documentElement.getElementsByTagName("application").item(0) as Element

        assertNull(application.getAndroidAttribute("process"))

        application.putAndroidAttribute("process", "com.spotify.music")

        assertEquals("com.spotify.music", application.getAndroidAttribute("process"))
        assertEquals(1, countRawAttributes(application, "process"))
    }

    @Test
    fun putAndroidAttributeReplacesAnExistingValue() {
        val element = provider(parse(sample))

        element.putAndroidAttribute("authorities", "replaced")

        assertEquals(1, countRawAttributes(element, "authorities"))
        assertEquals("replaced", element.getAndroidAttribute("authorities"))
    }

    @Test
    fun readsAPermissionGuard() {
        val receiver = parse(sample).getElementsByTagName("receiver").item(0) as Element
        assertEquals(
            "com.spotify.music.permission.SECURED_BROADCAST",
            receiver.getAndroidAttribute("permission")
        )
    }

    // --- element lookup ---

    @Test
    fun findsAChildByTagName() {
        val root = parse(sample).documentElement
        assertEquals("application", root.findChildElement("application")?.tagName)
        assertNull(root.findChildElement("activity"))
    }

    @Test
    fun doesNotMatchDescendantsWhenLookingForAChild() {
        // findChildElement must not reach past one level.
        val root = parse(sample).documentElement
        assertNull(root.findChildElement("provider"))
    }

    @Test
    fun collectsDescendantsByTagName() {
        val application = parse(sample).documentElement
            .getElementsByTagName("application").item(0) as Element

        val providers = application.descendantElements("provider")
        assertEquals(1, providers.size)
        assertEquals("com.spotify.music.share", providers.first().getAndroidAttribute("authorities"))
    }

    @Test
    fun findsAnElementByAnUnprefixedAttribute() {
        // This is the shape the production code uses: `name` on a <string> in
        // strings.xml, which carries no prefix.
        val doc = parse(sample)
        val found = doc.findElementByAttribute("plain", "value", "untouched")
        assertEquals("plain", found?.tagName)
        assertNull(doc.findElementByAttribute("plain", "value", "does.not.Exist"))
    }

    /**
     * A prefixed attribute is *not* found by this helper, which is correct
     * given the parser is not namespace-aware: the node name is literally
     * `android:value`, so `getAttribute("value")` is empty. Callers that want
     * a namespaced attribute use [getAndroidAttribute] instead.
     */
    @Test
    fun doesNotFindAPrefixedAttribute() {
        val doc = parse(sample)
        assertNull(doc.findElementByAttribute("provider", "name", "a.B"))
    }

    @Test
    fun allDescendantElementsIncludesEveryDepth() {
        val root = parse(sample).documentElement
        val all = root.allDescendantElements().map { it.tagName }
        assertTrue(all.containsAll(listOf("application", "provider", "receiver", "plain")), "got $all")
    }

    /**
     * Counts attributes whose name ends with the given local name, whether the
     * DOM recorded them prefixed or namespaced. A duplicate would show up here
     * as 2.
     */
    private fun countRawAttributes(element: Element, localName: String): Int {
        var count = 0
        val attributes = element.attributes
        for (i in 0 until attributes.length) {
            val attribute = attributes.item(i)
            val name = attribute.nodeName
            if (name == localName || name == "android:$localName" || attribute.localName == localName) {
                count++
            }
        }
        return count
    }
}
