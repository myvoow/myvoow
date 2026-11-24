/*
 * Block Blast patches (Morphe). Licensed under the GNU General Public License v3.0.
 */
package app.blockblast.patches.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

internal object Constants {
    const val EXTENSION = "extensions/blockblast.mpe"

    /** Descriptor prefix of the extension classes, which the patches must not rewrite. */
    const val EXTENSION_PREFIX = "Lapp/blockblast/extension/"

    const val ADS_CLASS = "${EXTENSION_PREFIX}Ads;"
    const val NET_BLOCK_CLASS = "${EXTENSION_PREFIX}NetBlock;"

    val COMPATIBILITY_BLOCK_BLAST = Compatibility(
        name = "Block Blast!",
        packageName = "com.block.juggle",
        // APKPure publishes Block Blast as an XAPK (base + config splits).
        apkFileType = ApkFileType.XAPK,
        appIconColor = 0x3B6FE0,
        targets = listOf(
            AppTarget(
                version = "11.0.6",
                description = "Version the patches were developed with",
            ),
            // The patches only use AppLovin MAX API names and framework calls, which survive
            // obfuscation, so later releases are expected to work.
            AppTarget(
                version = null,
                isExperimental = true,
            ),
        ),
    )
}
