// Compile-only stub: the real class is in the app.
package com.applovin.mediation;

import java.util.List;

@SuppressWarnings("rawtypes")
public interface MaxAdWaterfallInfo {
    long getLatencyMillis();
    MaxAd getLoadedAd();
    String getName();
    List getNetworkResponses();
    String getTestName();
}
