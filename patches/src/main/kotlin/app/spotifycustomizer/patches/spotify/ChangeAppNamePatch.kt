package app.spotifycustomizer.patches.spotify

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.spotifycustomizer.patches.shared.Constants.COMPATIBILITY_SPOTIFY
import app.spotifycustomizer.patches.shared.findElementByAttribute

/**
 * Renames the app, as shown under its launcher icon.
 *
 * Writes the user's value into the `app_name` string resource, which is what
 * the manifest's `android:label` resolves to. Verified against Spotify
 * 9.1.84.2231 (versionCode 146291969): `app_name` is declared exactly once, in
 * `res/values/strings.xml`, with no per-locale variants, and 29 manifest
 * elements reference it. Editing the single definition therefore renames all of
 * them at once and keeps them consistent.
 *
 * Only the launcher-visible name changes. The package, the app id and the
 * signing identity are untouched — see `CloneAppPatch` for those.
 */
@Suppress("unused")
val changeAppNamePatch = resourcePatch(
    name = "Change app name",
    description = "Changes the name shown under the app icon to a custom name.",
    default = false
) {
    compatibleWith(COMPATIBILITY_SPOTIFY)

    val appNameOption = stringOption(
        key = "App name",
        default = "Spotify",
        description = "The name shown under the app icon. Any text."
    )

    execute {
        val appName = requireNotNull(appNameOption.value) {
            "The 'App name' option was not set."
        }

        document("res/values/strings.xml").use { document ->
            val appNameElement = document.findElementByAttribute(
                tagName = "string",
                attributeName = "name",
                attributeValue = "app_name"
            ) ?: throw PatchException(
                "Could not find the 'app_name' string resource in res/values/strings.xml. " +
                    "Spotify has likely renamed it; this patch needs updating for the new app version."
            )

            appNameElement.textContent = escapeAndroidStringResource(appName)
        }
    }
}

/**
 * Escapes a value for use as the text of a `<string>` resource.
 *
 * The value is wrapped in literal double quotes, which makes it a *raw* string
 * in Android resource terms: an apostrophe in the user's input is then valid
 * without escaping. A raw string may not contain an unescaped double quote, so
 * those are stripped.
 *
 * Assigning to `textContent` and letting the serializer handle XML escaping is
 * deliberate — the XML is never assembled by hand here.
 *
 * @param value The raw user-supplied name.
 * @return A value safe to assign to a `<string>` element's text.
 */
private fun escapeAndroidStringResource(value: String): String =
    "\"" + value.replace("\"", "") + "\""
