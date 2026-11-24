/*
 * Block Blast patches (Morphe). Licensed under the GNU General Public License v3.0.
 */
package app.blockblast.extension;

import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdFormat;
import com.applovin.mediation.MaxAdWaterfallInfo;
import com.applovin.mediation.nativeAds.MaxNativeAd;
import com.applovin.sdk.AppLovinSdkUtils;

import java.util.Collections;
import java.util.List;

/**
 * The ad the fake callbacks report. Implements every method of MaxAd: the ad code of Block Blast
 * reads the network, revenue, creative id and waterfall of each ad for its logs, so none returns
 * null where the app expects a value.
 */
@SuppressWarnings({"unused", "rawtypes"})
final class FakeMaxAd implements MaxAd {
    private final String adUnitId;
    private final MaxAdFormat format;

    FakeMaxAd(String adUnitId, MaxAdFormat format) {
        this.adUnitId = adUnitId;
        this.format = format;
    }

    @Override
    public String getAdReviewCreativeId() {
        return "";
    }

    @Override
    public String getAdUnitId() {
        return adUnitId;
    }

    @Override
    public String getAdValue(String key) {
        return "";
    }

    @Override
    public String getAdValue(String key, String defaultValue) {
        return defaultValue;
    }

    @Override
    public String getCreativeId() {
        return "";
    }

    @Override
    public String getDspId() {
        return "";
    }

    @Override
    public String getDspName() {
        return "";
    }

    @Override
    public MaxAdFormat getFormat() {
        return format;
    }

    @Override
    public MaxNativeAd getNativeAd() {
        return null;
    }

    @Override
    public String getNetworkName() {
        return "AppLovin";
    }

    @Override
    public String getNetworkPlacement() {
        return "";
    }

    @Override
    public String getPlacement() {
        return "";
    }

    @Override
    public long getRequestLatencyMillis() {
        return 0;
    }

    @Override
    public double getRevenue() {
        return 0;
    }

    @Override
    public String getRevenuePrecision() {
        return "undefined";
    }

    @Override
    public AppLovinSdkUtils.Size getSize() {
        return AppLovinSdkUtils.Size.ZERO;
    }

    @Override
    public MaxAdWaterfallInfo getWaterfall() {
        final MaxAd ad = this;
        return new MaxAdWaterfallInfo() {
            @Override
            public long getLatencyMillis() {
                return 0;
            }

            @Override
            public MaxAd getLoadedAd() {
                return ad;
            }

            @Override
            public String getName() {
                return "Default";
            }

            @Override
            public List getNetworkResponses() {
                return Collections.emptyList();
            }

            @Override
            public String getTestName() {
                return "";
            }
        };
    }

    @Override
    public String toString() {
        return "FakeMaxAd{adUnitId=" + adUnitId + "}";
    }
}
