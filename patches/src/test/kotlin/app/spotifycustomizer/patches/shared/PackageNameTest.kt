package app.spotifycustomizer.patches.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for the package-name rewrite rules.
 *
 * These encode knowledge that cost three crash-and-diagnose cycles to
 * establish, so they are pinned here deliberately. See the notes on each case.
 */
class PackageNameTest {
    private val original = PackageName.ORIGINAL_PACKAGE
    private val target = "com.spotify.music.xeriomy"

    // --- isValidPackageName ---

    @Test
    fun acceptsAWellFormedName() {
        assertTrue(PackageName.isValidPackageName(target))
        assertTrue(PackageName.isValidPackageName("app.spotifycustomizer.clone"))
        assertTrue(PackageName.isValidPackageName("x.y"))
        assertTrue(PackageName.isValidPackageName("a1._b2.c3"))
    }

    @Test
    fun rejectsNull() {
        assertFalse(PackageName.isValidPackageName(null))
    }

    /**
     * A clone whose package equals the original is not a clone; it is an update
     * of the installed app. The option validator relies on this.
     */
    @Test
    fun rejectsTheOriginalPackageName() {
        assertFalse(PackageName.isValidPackageName(original))
    }

    @Test
    fun rejectsMalformedNames() {
        // Mirrors the patcher's own PACKAGE_NAME_REGEX. Each of these would
        // produce an APK that fails to install with no useful diagnostic.
        val malformed = listOf(
            "",                      // empty
            "nodots",                // needs at least two segments
            ".leading",              // segment starts with a dot
            "trailing.",             // segment starts with a dot
            "double..dot",           // empty segment
            "1starts.with.digit",    // segment starts with a digit
            "has space.here",        // space
            "has-dash.here",         // dash
            "trailing.dot.",         // trailing dot
        )
        malformed.forEach {
            assertFalse(PackageName.isValidPackageName(it), "expected '$it' to be rejected")
        }
    }

    // --- rewritePackageDerivedValue ---

    @Test
    fun rewritesTheBarePackageName() {
        assertEquals(target, PackageName.rewritePackageDerivedValue(original, target))
    }

    @Test
    fun rewritesAuthorityStyleValues() {
        // The eight provider authorities under <application> in 9.1.84.2231.
        listOf(
            "com.spotify.music.androidx-startup",
            "com.spotify.music.share",
            "com.spotify.music.pushnotificationsv2",
            "com.spotify.music.profile",
            "com.spotify.music.vtec",
            "com.spotify.music.calimage",
            "com.spotify.music.early-initialization",
            "com.spotify.music.imagepicker",
        ).forEach { value ->
            assertEquals(
                target + value.removePrefix(original),
                PackageName.rewritePackageDerivedValue(value, target),
                "expected '$value' to be rewritten"
            )
        }
    }

    @Test
    fun rewritesPermissionNames() {
        // The four custom permissions, in all three places each appears.
        listOf(
            "com.spotify.music.permission.SECURED_BROADCAST",
            "com.spotify.music.permission.INTERNAL_BROADCAST",
            "com.spotify.music.permission.C2D_MESSAGE",
            "com.spotify.music.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
        ).forEach { value ->
            assertEquals(
                target + value.removePrefix(original),
                PackageName.rewritePackageDerivedValue(value, target)
            )
        }
    }

    /**
     * The regression that matters most.
     *
     * These three live under <queries> and describe *other* apps, not ours.
     * Rewriting them would break the Samsung, Amazon and Car App integrations
     * and gain nothing, because they are visibility declarations rather than
     * components this app owns. They must come back byte-identical.
     */
    @Test
    fun leavesQueriesProvidersUntouched() {
        listOf(
            "com.sec.android.app.samsungapps.provider.ASAA",
            "amzn_appstore",
            "androidx.car.app.connection",
        ).forEach { value ->
            assertEquals(
                value,
                PackageName.rewritePackageDerivedValue(value, target),
                "expected the <queries> provider '$value' to be left alone"
            )
        }
    }

    /**
     * Class names live in the unchanged dex files. Rewriting a component's
     * android:name would point the manifest at classes that do not exist.
     */
    @Test
    fun leavesUnrelatedValuesUntouched() {
        listOf(
            "com.spotify.music.SpotifyApplication",
            "com.spotify.music.SpotifyMainActivity",
            "com.spotify.premiumdestination.upsell.activity.upsell.NotificationsIntentReceiver",
            "androidx.startup.InitializationProvider",
            "com.spotify.music.sso.afterlogindummytask",
            "android.permission.INTERNET",
        ).forEach { value ->
            assertEquals(
                value,
                PackageName.rewritePackageDerivedValue(value, target),
                "expected '$value' to be left alone"
            )
        }
    }

    @Test
    fun passesNullThrough() {
        assertEquals(null, PackageName.rewritePackageDerivedValue(null, target))
    }

    @Test
    fun isIdempotentForTheTargetItself() {
        // Rewriting twice must not double-prefix.
        val once = PackageName.rewritePackageDerivedValue("$original.share", target)!!
        val twice = PackageName.rewritePackageDerivedValue(once, target)
        assertEquals(once, twice)
    }
}
