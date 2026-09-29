package app.spotifycustomizer.patches.shared

import org.w3c.dom.Document
import org.w3c.dom.Element

/**
 * The Android namespace URI.
 */
private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"

/**
 * Small DOM helpers shared by the resource patches in this collection.
 *
 * These exist mostly to keep the patches readable: `NodeList.item()` returns a
 * `Node`, which has no `attributes` property, so every lookup needs an explicit
 * cast and a loop. Doing it here once means the patches read as intent rather
 * than as DOM plumbing.
 */

/**
 * Reads a namespaced `android:` attribute.
 *
 * Morphe parses manifests and resources with
 * `DocumentBuilderFactory.newInstance()`, which is **not** namespace-aware by
 * default. In such a document an `android:authorities` attribute carries no
 * namespace URI and its node name is the literal `android:authorities`, so
 * `getAttributeNS` returns an empty string and only `getAttribute` finds it.
 *
 * Both spellings are tried so this keeps working if the patcher ever enables
 * namespace awareness.
 *
 * @param name The local attribute name, e.g. `authorities`.
 * @return The attribute value, or null when the element does not declare it.
 */
fun Element.getAndroidAttribute(name: String): String? {
    val prefixed = getAttribute("android:$name")
    if (prefixed.isNotEmpty()) return prefixed

    return getAttributeNS(ANDROID_NS, name).ifEmpty { null }
}

/**
 * Writes a namespaced `android:` attribute, replacing it in place.
 *
 * Writes through whichever spelling the document actually uses, so the
 * attribute is updated rather than duplicated. Writing with `setAttributeNS`
 * into a non-namespace-aware document would add a *second* attribute
 * alongside the existing one instead of replacing it.
 *
 * Does nothing when the element does not declare the attribute; check with
 * [getAndroidAttribute] first.
 *
 * @param name The local attribute name, e.g. `authorities`.
 * @param value The new value.
 */
fun Element.setAndroidAttribute(name: String, value: String) {
    if (hasAttribute("android:$name")) {
        setAttribute("android:$name", value)
    } else if (hasAttributeNS(ANDROID_NS, name)) {
        setAttributeNS(ANDROID_NS, "android:$name", value)
    }
}

/**
 * Finds a descendant element by tag name and attribute value.
 *
 * @param tagName The element tag to match, e.g. `string`.
 * @param attributeName The attribute to compare, e.g. `name`.
 * @param attributeValue The value that attribute must have, e.g. `app_name`.
 * @return The first matching element, or null when there is none.
 */
fun Document.findElementByAttribute(
    tagName: String,
    attributeName: String,
    attributeValue: String,
): Element? {
    val nodes = getElementsByTagName(tagName)
    for (i in 0 until nodes.length) {
        val node = nodes.item(i)
        if (node is Element && node.getAttribute(attributeName) == attributeValue) {
            return node
        }
    }
    return null
}

/**
 * Finds a direct child element by tag name.
 *
 * @param tagName The element tag to match, e.g. `application`.
 * @return The first matching child element, or null when there is none.
 */
fun Element.findChildElement(tagName: String): Element? {
    val children = childNodes
    for (i in 0 until children.length) {
        val child = children.item(i)
        if (child is Element && child.tagName == tagName) {
            return child
        }
    }
    return null
}

/**
 * Collects this element's descendants with the given tag name, at any depth.
 *
 * @param tagName The element tag to match, e.g. `provider`.
 * @return The matching descendant elements, in document order.
 */
fun Element.descendantElements(tagName: String): List<Element> {
    val result = mutableListOf<Element>()
    val children = childNodes
    for (i in 0 until children.length) {
        val child = children.item(i)
        if (child is Element) {
            if (child.tagName == tagName) result += child
            result += child.descendantElements(tagName)
        }
    }
    return result
}
