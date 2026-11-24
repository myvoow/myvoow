/*
 * Block Blast patches (Morphe). Licensed under the GNU General Public License v3.0.
 */
package app.blockblast.patches.ads

import app.blockblast.patches.network.enableBlockList
import app.blockblast.patches.network.networkBlockerPatch
import app.blockblast.patches.shared.Constants.ADS_CLASS
import app.blockblast.patches.shared.Constants.COMPATIBILITY_BLOCK_BLAST
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.returnEarly
import com.android.tools.smali.dexlib2.AccessFlags

private const val APPLOVIN_SDK = "Lcom/applovin/sdk/AppLovinSdk;"
private const val MAX_AD_VIEW = "Lcom/applovin/mediation/ads/MaxAdView;"
private const val MAX_NATIVE_AD_LOADER = "Lcom/applovin/mediation/nativeAds/MaxNativeAdLoader;"

/** The fullscreen ad classes of AppLovin MAX. Rewarded ads also get their reward. */
private val FULLSCREEN_ADS = listOf(
    "Lcom/applovin/mediation/ads/MaxRewardedAd;",
    "Lcom/applovin/mediation/ads/MaxInterstitialAd;",
    "Lcom/applovin/mediation/ads/MaxAppOpenAd;",
)

@Suppress("unused")
val removeAdsPatch = bytecodePatch(
    name = "Remove ads",
    description = "Removes every ad: interstitials, app open ads, banners and rewarded videos. " +
        "Rewarded ads give their reward instantly, so revives and the other rewards need no video. " +
        "AppLovin MAX and the ad networks it mediates are never started, and the ad servers are blocked.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_BLOCK_BLAST)

    dependsOn(networkBlockerPatch)

    execute {
        // AppLovinSdk.initialize(configuration, listener): the SDK and the networks it mediates
        // stay off, the listener gets a fake configuration.
        requireClass(APPLOVIN_SDK).methods.apply {
            single { it.name == "initialize" && it.parameterTypes.size == 2 }.addInstructions(
                0,
                """
                    invoke-static/range { p2 .. p2 }, $ADS_CLASS->initialize(Ljava/lang/Object;)V
                    return-void
                """,
            )
            firstOrNull { it.name == "isInitialized" && it.parameterTypes.isEmpty() }?.returnEarly(true)
        }

        // Rewarded, interstitial and app open ads load at once and close as soon as they are shown.
        FULLSCREEN_ADS.forEach { type ->
            requireClass(type).methods.filter { it.isPublicInstance() }.forEach { method ->
                when {
                    method.name == "setListener" && method.parameterTypes.size == 1 ->
                        method.addInstructions(
                            0,
                            "invoke-static/range { p0 .. p1 }, $ADS_CLASS->" +
                                "setListener(Ljava/lang/Object;Ljava/lang/Object;)V",
                        )

                    method.name == "loadAd" && method.parameterTypes.isEmpty() ->
                        method.callAndReturn("load")

                    method.name == "isReady" && method.parameterTypes.isEmpty() ->
                        method.returnEarly(true)

                    method.name == "showAd" ->
                        method.callAndReturn("show")
                }
            }
        }

        // Banners never load and stay hidden.
        requireClass(MAX_AD_VIEW).methods.filter { it.isPublicInstance() }.forEach { method ->
            when {
                method.name == "loadAd" && method.parameterTypes.isEmpty() ->
                    method.addInstructions(
                        0,
                        """
                            invoke-static/range { p0 .. p0 }, $ADS_CLASS->hideBanner(Landroid/view/View;)V
                            return-void
                        """,
                    )

                method.name == "startAutoRefresh" -> method.returnEarly()
            }
        }

        // Native ads never load.
        mutableClassDefByOrNull(MAX_NATIVE_AD_LOADER)?.methods
            ?.filter { it.isPublicInstance() && it.name == "loadAd" }
            ?.forEach { it.returnEarly() }

        enableBlockList("blockAds")
    }
}

private fun BytecodePatchContext.requireClass(type: String) =
    mutableClassDefByOrNull(type) ?: throw PatchException("AppLovin MAX class $type not found")

private fun MutableMethod.isPublicInstance() =
    AccessFlags.PUBLIC.isSet(accessFlags) && !AccessFlags.STATIC.isSet(accessFlags) && implementation != null

/** `Ads.<name>(this); return;` at the start of the method. */
private fun MutableMethod.callAndReturn(name: String) = addInstructions(
    0,
    """
        invoke-static/range { p0 .. p0 }, $ADS_CLASS->$name(Ljava/lang/Object;)V
        return-void
    """,
)
