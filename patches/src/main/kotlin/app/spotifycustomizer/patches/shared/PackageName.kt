package app.spotifycustomizer.patches.shared

/**
 * Helpers for deriving a new package name and rewriting package-derived values
 * in a patched Spotify APK.
 *
 * Spotify builds its ContentProvider authorities at runtime by concatenating
 * `getPackageName()` with a short suffix (`.share`, `.profile`, `.vtec`, ...).
 * Those suffixes exist as standalone strings in the APK's dex files rather than
 * as fully-qualified literals, so renaming the package in the manifest is
 * sufficient: the app derives the matching new authority on its own. This is
 * why the clone patch does no bytecode work.
 */
object PackageName {
    /**
     * The original Spotify package name, as declared in the manifest of the
     * APKs this collection targets.
     */
    const val ORIGINAL_PACKAGE = "com.spotify.music"

    /**
     * Android's own package-name rule, mirrored from
     * `Compatibility.kt` in morphe-patcher:
     * `^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z][a-zA-Z0-9_]*)+$`
     *
     * Validating early matters: an invalid package name yields an APK that
     * fails to install with no useful diagnostic, so failing loudly at patch
     * time is strictly better.
     */
    private val PACKAGE_NAME_REGEX =
        Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")

    /**
     * @param value The candidate package name.
     * @return Whether [value] is a usable replacement for [ORIGINAL_PACKAGE].
     */
    fun isValidPackageName(value: String?): Boolean =
        value != null &&
            value.matches(PACKAGE_NAME_REGEX) &&
            value != ORIGINAL_PACKAGE

    /**
     * Rewrites a package-derived manifest value onto [newPackage].
     *
     * Only values derived from the original package are touched. Everything
     * else is returned unchanged — that is what keeps the `<queries>` providers
     * (`com.sec.android.app.samsungapps.provider.ASAA`, `amzn_appstore`,
     * `androidx.car.app.connection`) intact. They are visibility declarations
     * for other apps, not ours, and rewriting them would break the Samsung,
     * Amazon and Car App integrations for no install benefit.
     *
     * @param value The current value, e.g. `com.spotify.music.share`.
     * @param newPackage The replacement package name.
     * @return The rewritten value, or [value] itself when it is not derived
     *   from the original package.
     */
    fun rewritePackageDerivedValue(value: String?, newPackage: String): String? {
        if (value == null) return null
        if (value == ORIGINAL_PACKAGE) return newPackage
        if (value.startsWith("$ORIGINAL_PACKAGE.")) {
            return newPackage + value.removePrefix(ORIGINAL_PACKAGE)
        }
        return value
    }
}
