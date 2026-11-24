/*
 * Block Blast patches (Morphe). Licensed under the GNU General Public License v3.0.
 */
package app.blockblast.extension;

import android.util.Log;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Blocks the hosts of ad networks and trackers, whatever SDK talks to them.
 *
 * <p>The patches route every {@code URL.openConnection()} of the app through
 * {@link #openConnection(URL)} and make every OkHttp {@code Dns.lookup} call
 * {@link #checkHost(String)} first. A blocked request fails like a request without network, at
 * the point where the SDKs already expect network errors.
 */
@SuppressWarnings("unused")
public final class NetBlock {
    private static final String TAG = "BlockBlastPatches";

    /** Connections to blocked hosts go here instead: refused at once, as there is no server. */
    private static final String DEAD_URL = "https://127.0.0.1:1/";

    /** Ad networks and ad decision servers. A host matches itself and its subdomains. */
    private static final String[] AD_HOSTS = {
            // AppLovin MAX.
            "applovin.com", "applvn.com", "appl.vn", "applov.in", "safedk.com",
            // Networks mediated by MAX.
            "doubleclick.net", "googlesyndication.com", "googleadservices.com",
            "admob-gmats.uc.r.appspot.com", "imasdk.googleapis.com", "fundingchoicesmessages.google.com",
            "pangle.io", "pangleglobal.com", "tiktokpangle.us", "tiktokpangle-cdn-us.com", "i18n-pglstatp.com",
            "moloco.com", "adsmoloco.com",
            "vungle.com",
            "inmobi.com", "inmobicdn.net",
            "fyber.com", "inner-active.mobi",
            "unityads.unity3d.com",
            "pubmatic.com",
            "amazon-adsystem.com", "advertising.a2z.com",
            "bigo.sg", "adsbigo.com",
            "ironsrc.mobi", "ironsrc.com", "supersonicads.com",
            // Ad servers of Hungry Studio (hella, rtb, ecpm and floor prediction).
            "hella.afafb.com", "hella-cfg.afafb.com", "hella-cfg-test.afafb.com",
            "hella-decision.afafb.com", "hella-game-gateway-server.afafb.com",
            "rtb.afafb.com", "middle-rtb.afafb.com", "adx-bidder-api-test.afafb.com",
            "ecpm.afafb.com", "low-ecpm.afafb.com", "session-control.afafb.com", "adq.afafb.com",
            "ad-host-backup-europe.oss-eu-central-1.aliyuncs.com",
            "ad-host-backup-asia.oss-ap-southeast-1.aliyuncs.com",
            "ad-host-backup-america.oss-us-west-1.aliyuncs.com",
    };

    /** Analytics, attribution and crash reporting. */
    private static final String[] TELEMETRY_HOSTS = {
            // Firebase / Google Analytics, Crashlytics. Remote Config and messaging stay allowed.
            "app-measurement.com", "google-analytics.com", "analytics.google.com",
            "firebaselogging-pa.googleapis.com", "firebaselogging.googleapis.com",
            "crashlyticsreports-pa.googleapis.com", "firebase-settings.crashlytics.com",
            // AppsFlyer.
            "appsflyer.com", "appsflyersdk.com",
            // Bugsnag.
            "bugsnag.com", "bugsnag.smartbear.com",
            // TikTok business SDK, ByteDance APM.
            "analytics.tiktok.com", "analytics.us.tiktok.com", "business-api.tiktok.com",
            "log.byteoversea.com",
            // ThinkingData.
            "thinkingdata.cn", "thinkingdata.io",
            // Facebook app events. Graph API calls for login and sharing stay allowed.
            "an.facebook.com",
            // SDK metrics of the ad networks.
            "telemetry.sdk.inmobi.com", "crash-metrics.sdk.inmobi.com", "sdkopmetrics-us.dsp-api.moloco.com",
            "logs.ads.vungle.com", "events.ads.vungle.com",
            // Hungry Studio: ThinkingData server (gamedot), APM, ad metrics.
            "gamedot-v3.afafb.com", "apm-receiver.afafb.com", "mediation-receiver.afafb.com",
            "cross-sync.afafb.com", "cross-sync-test.afafb.com",
    };

    private static final Set<String> LOGGED = Collections.synchronizedSet(new HashSet<String>());

    private NetBlock() {
    }

    /** Replaced with {@code return true} by the "Remove ads" patch. */
    public static boolean blockAds() {
        return false;
    }

    /** Replaced with {@code return true} by the "Disable telemetry" patch. */
    public static boolean blockTelemetry() {
        return false;
    }

    public static boolean isBlocked(String host) {
        if (host == null || host.isEmpty()) return false;
        String normalized = host.toLowerCase(Locale.ROOT);
        if (normalized.endsWith(".")) normalized = normalized.substring(0, normalized.length() - 1);

        boolean blocked = (blockAds() && matches(normalized, AD_HOSTS))
                || (blockTelemetry() && matches(normalized, TELEMETRY_HOSTS));
        if (blocked && LOGGED.add(normalized)) {
            Log.i(TAG, "Blocked " + normalized);
        }
        return blocked;
    }

    private static boolean matches(String host, String[] domains) {
        for (String domain : domains) {
            if (host.equals(domain) || host.endsWith("." + domain)) return true;
        }
        return false;
    }

    /** Replaces {@code url.openConnection()}. */
    public static URLConnection openConnection(URL url) throws IOException {
        if (url != null && isBlocked(url.getHost())) return new URL(DEAD_URL).openConnection();
        return url.openConnection();
    }

    /** Replaces {@code url.openConnection(proxy)}. */
    public static URLConnection openConnection(URL url, Proxy proxy) throws IOException {
        if (url != null && isBlocked(url.getHost())) return new URL(DEAD_URL).openConnection(proxy);
        return url.openConnection(proxy);
    }

    /** Called first in every OkHttp {@code Dns.lookup(hostname)}. */
    public static void checkHost(String host) throws UnknownHostException {
        if (isBlocked(host)) throw new UnknownHostException("Blocked by patch: " + host);
    }
}
