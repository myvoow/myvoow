// Compile-only stub: the real class is in the app.
package com.applovin.mediation;

import com.applovin.mediation.nativeAds.MaxNativeAd;
import com.applovin.sdk.AppLovinSdkUtils;

public interface MaxAd {
    String getAdReviewCreativeId();
    String getAdUnitId();
    String getAdValue(String key);
    String getAdValue(String key, String defaultValue);
    String getCreativeId();
    String getDspId();
    String getDspName();
    MaxAdFormat getFormat();
    MaxNativeAd getNativeAd();
    String getNetworkName();
    String getNetworkPlacement();
    String getPlacement();
    long getRequestLatencyMillis();
    double getRevenue();
    String getRevenuePrecision();
    AppLovinSdkUtils.Size getSize();
    MaxAdWaterfallInfo getWaterfall();
}
