package app.spotifycustomizer.patches.spotify

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.spotifycustomizer.patches.shared.Constants.COMPATIBILITY_SPOTIFY
import app.spotifycustomizer.patches.shared.PackageName.ORIGINAL_PACKAGE
import app.spotifycustomizer.patches.shared.PackageName.isValidPackageName
import app.spotifycustomizer.patches.shared.PackageName.rewritePackageDerivedValue
import app.spotifycustomizer.patches.shared.descendantElements
import app.spotifycustomizer.patches.shared.findChildElement
import app.spotifycustomizer.patches.shared.findElementByAttribute
import app.spotifycustomizer.patches.shared.getAndroidAttribute
import app.spotifycustomizer.patches.shared.setAndroidAttribute

/**
 * The string resource holding the media provider's authority.
 *
 * Unlike the other providers, `MediaProvider` does not declare its authority
 * literally in the manifest — it references this resource. The manifest
 * attribute is therefore left alone and the resource *value* is rewritten
 * instead, which is what the running app actually reads.
 */
private const val MEDIA_AUTHORITY_RESOURCE = "media_provider_authority"

/** The value that resource holds in unpatched Spotify 9.1.84.2231. */
private const val ORIGINAL_MEDIA_AUTHORITY = "com.spotify.mobile.android.mediaapi"

/**
 * Makes the app installable alongside the original Spotify.
 *
 * Renames the package, which is what Android treats as an app's identity, so
 * the patched APK is a distinct app rather than an update of the original.
 * Three things have to move together or the install fails with
 * `INSTALL_FAILED_CONFLICTING_PROVIDER`, because two installed apps may not
 * declare the same provider authority:
 *
 *  1. the manifest `package` attribute,
 *  2. the nine provider authorities declared under `<application>`,
 *  3. the `media_provider_authority` string resource.
 *
 * No bytecode is patched. Spotify builds its authorities at runtime from
 * `getPackageName()` plus a short suffix, so it derives the new authorities
 * itself once the manifest is renamed.
 *
 * Known limitation: the app declares `com.spotify.music.permission.*` custom
 * permissions with `protectionLevel="signature"`. Signed with a different key
 * than the original, the clone may be denied permissions the original holds.
 * Impact is bounded — those permission names appear nowhere in the dex files,
 * and `SECURED_BROADCAST` guards a single manifest element — so the realistic
 * worst case is one rejected internal broadcast. They are left alone
 * deliberately: renaming them would leave the `uses-permission` entries
 * pointing at names the clone no longer declares.
 */
@Suppress("unused")
val cloneAppPatch = resourcePatch(
    name = "Clone app",
    description = "Changes the package name so the app installs alongside the original.",
    default = false
) {
    compatibleWith(COMPATIBILITY_SPOTIFY)

    val packageNameOption = stringOption(
        key = "Package name",
        default = "xeriomy.x.spotifyx",
        description = "The new package name. Must be a valid package name.",
        validator = { isValidPackageName(it) }
    )

    execute {
        val target = requireNotNull(packageNameOption.value) {
            "The 'Package name' option was not set."
        }

        // The option validator already rejects these, but a patch that trusts
        // only the UI would still produce an un-installable APK if the option
        // were ever set programmatically.
        if (!isValidPackageName(target)) {
            throw PatchException(
                "\"$target\" is not a usable package name. It needs at least two dot-separated " +
                    "segments, each starting with a letter, and it must not be the original " +
                    "package name ($ORIGINAL_PACKAGE)."
            )
        }

        document("AndroidManifest.xml").use { manifest ->
            val root = manifest.documentElement
                ?: throw PatchException("AndroidManifest.xml has no root <manifest> element.")

            val declaredPackage = root.getAttribute("package")
            if (declaredPackage != ORIGINAL_PACKAGE) {
                throw PatchException(
                    "Expected the manifest package to be $ORIGINAL_PACKAGE but found " +
                        "\"$declaredPackage\". This patch only targets unpatched Spotify APKs."
                )
            }

            // 1. The app id itself.
            root.setAttribute("package", target)

            // 2. Provider authorities, scoped to <application>. The <queries>
            //    providers describe other apps and must not be rewritten.
            val application = root.findChildElement("application")
                ?: throw PatchException("AndroidManifest.xml has no <application> element.")

            var rewrittenAuthorities = 0
            application.descendantElements("provider").forEach { provider ->
                val currentAuthority = provider.getAndroidAttribute("authorities")
                    ?: return@forEach

                val rewritten = rewritePackageDerivedValue(currentAuthority, target)
                if (rewritten != null && rewritten != currentAuthority) {
                    provider.setAndroidAttribute("authorities", rewritten)
                    rewrittenAuthorities++
                }
            }

            if (rewrittenAuthorities == 0) {
                throw PatchException(
                    "No provider authority was rewritten. Spotify 9.1.84.2231 declares eight " +
                        "authorities derived from $ORIGINAL_PACKAGE; finding none means the app " +
                        "has changed and the clone would collide with the original on install."
                )
            }
        }

        // 3. The media provider's authority lives in a string resource.
        document("res/values/strings.xml").use { document ->
            val authorityElement = document.findElementByAttribute(
                tagName = "string",
                attributeName = "name",
                attributeValue = MEDIA_AUTHORITY_RESOURCE
            ) ?: throw PatchException(
                "Could not find the '$MEDIA_AUTHORITY_RESOURCE' string resource. The media " +
                    "provider's authority would collide with the original app on install."
            )

            if (authorityElement.textContent == ORIGINAL_MEDIA_AUTHORITY) {
                authorityElement.textContent = "$target.mediaapi"
            }
        }
    }
}
