/*
 * Block Blast patches (Morphe). Licensed under the GNU General Public License v3.0.
 */
package app.blockblast.extension;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import com.applovin.mediation.MaxAdFormat;
import com.applovin.mediation.MaxAdListener;
import com.applovin.mediation.MaxRewardedAdListener;
import com.applovin.mediation.ads.MaxAppOpenAd;
import com.applovin.mediation.ads.MaxInterstitialAd;
import com.applovin.mediation.ads.MaxRewardedAd;
import com.applovin.sdk.AppLovinSdk;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Replaces AppLovin MAX, the mediation every ad of Block Blast goes through, with fake ads.
 *
 * <ul>
 *   <li>The SDK is never initialized: the init listener gets a fake configuration, so no ad
 *       network (Pangle, Moloco, Vungle...) is started.</li>
 *   <li>Fullscreen ads (rewarded, interstitial, app open) "load" at once and "close" as soon as
 *       they are shown, without displaying anything.</li>
 *   <li>Rewarded ads grant their reward: revives and the other rewards need no video.</li>
 *   <li>Banners and native ads never load and stay hidden.</li>
 * </ul>
 *
 * The patches call these methods from the MAX classes. The ad objects are typed as Object in the
 * signatures so the patches do not depend on the stub types.
 */
@SuppressWarnings("unused")
public final class Ads {
    private static final String TAG = "BlockBlastPatches";

    /** Time between the fake "displayed" and "hidden" callbacks, like a skipped ad. */
    private static final long SHOW_DURATION_MS = 300;

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    /** Listener of each MAX ad object, set by the app through setListener. */
    private static final Map<Object, Object> LISTENERS =
            Collections.synchronizedMap(new WeakHashMap<Object, Object>());

    private Ads() {
    }

    // region SDK

    /** Replaces AppLovinSdk.initialize(configuration, listener). */
    public static void initialize(final Object listener) {
        Log.i(TAG, "AppLovin MAX initialization skipped");
        if (!(listener instanceof AppLovinSdk.SdkInitializationListener)) return;
        MAIN.post(new Runnable() {
            @Override
            public void run() {
                try {
                    ((AppLovinSdk.SdkInitializationListener) listener)
                            .onSdkInitialized(new FakeSdkConfiguration());
                } catch (Throwable t) {
                    Log.e(TAG, "onSdkInitialized failed", t);
                }
            }
        });
    }

    // endregion

    // region Fullscreen ads

    /** Called first in setListener of MaxRewardedAd, MaxInterstitialAd and MaxAppOpenAd. */
    public static void setListener(Object ad, Object listener) {
        if (ad == null) return;
        if (listener == null) {
            LISTENERS.remove(ad);
        } else {
            LISTENERS.put(ad, listener);
        }
    }

    /** Replaces loadAd() of the fullscreen ads: reports a loaded ad. */
    public static void load(final Object ad) {
        final Object listener = LISTENERS.get(ad);
        if (!(listener instanceof MaxAdListener)) return;
        final FakeMaxAd fakeAd = fakeAd(ad);
        MAIN.post(new Runnable() {
            @Override
            public void run() {
                try {
                    ((MaxAdListener) listener).onAdLoaded(fakeAd);
                } catch (Throwable t) {
                    Log.e(TAG, "onAdLoaded failed", t);
                }
            }
        });
    }

    /**
     * Replaces every showAd overload of the fullscreen ads: reports the ad as displayed, then
     * rewarded (rewarded ads only), then hidden.
     */
    public static void show(final Object ad) {
        final Object listener = LISTENERS.get(ad);
        if (!(listener instanceof MaxAdListener)) return;
        final FakeMaxAd fakeAd = fakeAd(ad);
        final MaxAdListener adListener = (MaxAdListener) listener;

        MAIN.post(new Runnable() {
            @Override
            public void run() {
                try {
                    adListener.onAdDisplayed(fakeAd);
                } catch (Throwable t) {
                    Log.e(TAG, "onAdDisplayed failed", t);
                }
            }
        });
        MAIN.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (ad instanceof MaxRewardedAd && listener instanceof MaxRewardedAdListener) {
                    try {
                        ((MaxRewardedAdListener) listener).onUserRewarded(fakeAd, new FakeReward());
                    } catch (Throwable t) {
                        Log.e(TAG, "onUserRewarded failed", t);
                    }
                }
                try {
                    adListener.onAdHidden(fakeAd);
                } catch (Throwable t) {
                    Log.e(TAG, "onAdHidden failed", t);
                }
            }
        }, SHOW_DURATION_MS);
    }

    private static FakeMaxAd fakeAd(Object ad) {
        String adUnitId = "";
        MaxAdFormat format = MaxAdFormat.INTERSTITIAL;
        try {
            if (ad instanceof MaxRewardedAd) {
                adUnitId = ((MaxRewardedAd) ad).getAdUnitId();
                format = MaxAdFormat.REWARDED;
            } else if (ad instanceof MaxInterstitialAd) {
                adUnitId = ((MaxInterstitialAd) ad).getAdUnitId();
            } else if (ad instanceof MaxAppOpenAd) {
                adUnitId = ((MaxAppOpenAd) ad).getAdUnitId();
                format = MaxAdFormat.APP_OPEN;
            }
        } catch (Throwable t) {
            Log.e(TAG, "getAdUnitId failed", t);
        }
        return new FakeMaxAd(adUnitId == null ? "" : adUnitId, format);
    }

    // endregion

    // region Banners

    /** Replaces MaxAdView.loadAd(): the banner never loads and takes no space. */
    public static void hideBanner(View banner) {
        if (banner != null) banner.setVisibility(View.GONE);
    }

    // endregion
}
