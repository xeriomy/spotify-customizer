package app.spotifycustomizer.patches.spotify

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.spotifycustomizer.patches.shared.Constants.COMPATIBILITY_SPOTIFY
import app.spotifycustomizer.patches.shared.PackageName.isValidPackageName
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21t

/**
 * The new package name for the clone.
 *
 * Declared once and shared by the two halves of the clone feature, which have
 * to agree on the value. [cloneAppPatch] owns and exposes the option; this
 * patch reads the same option back out of it, so a user cannot end up with a
 * manifest and a bytecode patch that disagree about the package name.
 */
internal val clonePackageName = stringOption(
    key = "Package name",
    default = "xeriomy.x.spotifyx",
    description = "The new package name. Must be a valid package name.",
    validator = { isValidPackageName(it) }
)

/**
 * Teaches the app that its own renamed process is legitimate.
 *
 * Spotify classifies the running process against a hardcoded allowlist before
 * initialising anything, and aborts with
 * `AssertionError: The process name ... is not allowed to start` on anything it
 * does not recognise. Renaming the package puts the process name on that path,
 * so this rewrites the allowlist entry to the new name.
 *
 * This depends on [cloneAppPatch], so the Morphe app enables it automatically
 * when "Clone app" is selected. It runs *after* that patch, which is what the
 * dependency guarantees, and takes the package name from that patch's option
 * rather than declaring a second one the user would have to fill in twice.
 */
@Suppress("unused")
val cloneAppProcessNamePatch = bytecodePatch(
    name = "Clone app process name",
    description = "Allows the app to start under its new package name.",
    default = false
) {
    compatibleWith(COMPATIBILITY_SPOTIFY)

    dependsOn(cloneAppPatch)

    execute {
        val target = cloneAppPatch.options[clonePackageName.name]?.value as? String
            ?: throw PatchException(
                "The '${clonePackageName.name}' option was not set on the " +
                    "\"${cloneAppPatch.name}\" patch."
            )

        // Accessing instructionMatches resolves the fingerprint.
        val allowlistEntry = processNameAllowlistFingerprint.instructionMatches[0]
        val register = allowlistEntry.getInstruction<BuilderInstruction21t>().registerA

        // `const-string v2, "com.spotify.music"` becomes the new package name.
        //
        // Only the main-process entry is touched. `.gdbprocess` and
        // `robolectric.ui` keep their values, which is why this replaces the
        // one instruction the first filter matched rather than searching the
        // method for the literal again.
        processNameAllowlistFingerprint.method.replaceInstruction(
            allowlistEntry.index,
            """const-string v$register, "$target""""
        )
    }
}
