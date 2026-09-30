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
        assertTrue(PackageName.isValidPackageName("a1.b2.c3"))
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
            "_leading.underscore",   // segment starts with an underscore
            "has space.here",        // space
            "has-dash.here",         // dash
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
     * Values that are not derived from the package come back unchanged.
     */
    @Test
    fun leavesUnrelatedValuesUntouched() {
        listOf(
            "com.spotify.premiumdestination.upsell.activity.upsell.NotificationsIntentReceiver",
            "androidx.startup.InitializationProvider",
            "com.spotify.mobile.android.mediaapi",
            "android.permission.INTERNET",
            "com.sec.android.app.samsungapps.provider.ASAA",
        ).forEach { value ->
            assertEquals(
                value,
                PackageName.rewritePackageDerivedValue(value, target),
                "expected '$value' to be left alone"
            )
        }
    }

    /**
     * The helper cannot tell a class name from an authority, and does not try.
     *
     * `com.spotify.music.SpotifyApplication` *is* rewritten, because it starts
     * with the original package. That is correct for the values the clone patch
     * routes through this function — the package attribute, provider
     * authorities, and permission names — and wrong for a component's
     * `android:name`, which names a class in the unchanged dex files.
     *
     * So the safety lives entirely at the call site. This test exists to make
     * that contract explicit: if someone later widens the set of attributes
     * passed through this function, this test is the thing that should make
     * them think again.
     */
    @Test
    fun rewritesClassNamesTooWhichIsWhyCallSitesMustBeScoped() {
        assertEquals(
            "com.spotify.music.xeriomy.SpotifyApplication",
            PackageName.rewritePackageDerivedValue(
                "com.spotify.music.SpotifyApplication", target
            )
        )
    }

    @Test
    fun passesNullThrough() {
        assertEquals(null, PackageName.rewritePackageDerivedValue(null, target))
    }

    /**
     * Not idempotent, by design of the callers rather than of this function.
     *
     * The default target itself starts with the original package, so running
     * the rewrite twice would prefix it twice. That is unreachable in practice:
     * the clone patch refuses an APK whose manifest package is no longer the
     * original, so it cannot be applied to its own output. Pinned here so the
     * behaviour is a decision on record rather than a surprise.
     */
    @Test
    fun isNotIdempotentWhenTheTargetSharesThePrefix() {
        val once = PackageName.rewritePackageDerivedValue("$original.share", target)!!
        val twice = PackageName.rewritePackageDerivedValue(once, target)
        assertEquals("$target.share.xeriomy", twice)
    }

    @Test
    fun isIdempotentWhenTheTargetDoesNotShareThePrefix() {
        val other = "app.spotifycustomizer.clone"
        val once = PackageName.rewritePackageDerivedValue("$original.share", other)!!
        val twice = PackageName.rewritePackageDerivedValue(once, other)
        assertEquals(once, twice)
    }
}
