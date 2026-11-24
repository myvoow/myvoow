/*
 * Block Blast patches (Morphe). Licensed under the GNU General Public License v3.0.
 */
package app.blockblast.patches.telemetry

import app.blockblast.patches.network.enableBlockList
import app.blockblast.patches.network.networkBlockerPatch
import app.blockblast.patches.shared.Constants.COMPATIBILITY_BLOCK_BLAST
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element

private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"

/** Flags read by Firebase, Facebook and Bugsnag at startup. */
private val MANIFEST_FLAGS = mapOf(
    // Firebase / Google Analytics: off for good, no advertising id, no consent for ad storage.
    "firebase_analytics_collection_enabled" to "false",
    "firebase_analytics_collection_deactivated" to "true",
    "google_analytics_adid_collection_enabled" to "false",
    "google_analytics_ssaid_collection_enabled" to "false",
    "google_analytics_deferred_deep_link_enabled" to "false",
    "google_analytics_default_allow_analytics_storage" to "false",
    "google_analytics_default_allow_ad_storage" to "false",
    "google_analytics_default_allow_ad_user_data" to "false",
    "google_analytics_default_allow_ad_personalization_signals" to "false",
    "firebase_crashlytics_collection_enabled" to "false",
    "firebase_performance_collection_deactivated" to "true",
    // Facebook: no automatic app events, no advertising id.
    "com.facebook.sdk.AutoLogAppEventsEnabled" to "false",
    "com.facebook.sdk.AdvertiserIDCollectionEnabled" to "false",
    // Bugsnag: no session tracking.
    "com.bugsnag.android.AUTO_TRACK_SESSIONS" to "false",
)

/** Permissions that give the trackers an advertising id or install attribution. */
private val TRACKING_PERMISSIONS = setOf(
    "com.google.android.gms.permission.AD_ID",
    "android.permission.ACCESS_ADSERVICES_AD_ID",
    "android.permission.ACCESS_ADSERVICES_ATTRIBUTION",
    "android.permission.ACCESS_ADSERVICES_TOPICS",
    "android.permission.ACCESS_ADSERVICES_CUSTOM_AUDIENCE",
    "com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE",
    "com.applovin.array.apphub.permission.BIND_APPHUB_SERVICE",
    "com.amazon.privacypass.ATTEST",
)

private val telemetryManifestPatch = resourcePatch {
    compatibleWith(COMPATIBILITY_BLOCK_BLAST)

    execute {
        document("AndroidManifest.xml").use { document ->
            val manifest = document.documentElement

            // Without these permissions the advertising id reads as zeros (Android 13+).
            val permissions = manifest.getElementsByTagName("uses-permission")
            (0 until permissions.length).map { permissions.item(it) as Element }
                .filter { it.getAttributeNS(ANDROID_NS, "name") in TRACKING_PERMISSIONS }
                .forEach { it.parentNode.removeChild(it) }

            val application = manifest.getElementsByTagName("application").item(0) as Element
            val metaData = application.getElementsByTagName("meta-data")
            val existing = (0 until metaData.length).map { metaData.item(it) as Element }
                .filter { it.parentNode == application }
                .associateBy { it.getAttributeNS(ANDROID_NS, "name") }

            MANIFEST_FLAGS.forEach { (name, value) ->
                val element = existing[name] ?: document.createElement("meta-data").also {
                    it.setAttributeNS(ANDROID_NS, "android:name", name)
                    application.appendChild(it)
                }
                element.setAttributeNS(ANDROID_NS, "android:value", value)
            }
        }
    }
}

@Suppress("unused")
val disableTelemetryPatch = bytecodePatch(
    name = "Disable telemetry",
    description = "Blocks analytics, attribution and crash reporting: Firebase Analytics, Crashlytics, " +
        "AppsFlyer, Bugsnag, ThinkingData, TikTok, Facebook app events and the trackers of Hungry Studio. " +
        "Turns off their collection flags and removes the advertising id permissions. " +
        "Login, cloud save, purchases and Remote Config keep working.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_BLOCK_BLAST)

    dependsOn(networkBlockerPatch, telemetryManifestPatch)

    execute {
        enableBlockList("blockTelemetry")
    }
}
