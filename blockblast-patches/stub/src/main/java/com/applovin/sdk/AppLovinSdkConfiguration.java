// Compile-only stub: the real class is in the app.
package com.applovin.sdk;

import java.util.List;

public interface AppLovinSdkConfiguration {
    enum ConsentDialogState { UNKNOWN, APPLIES, DOES_NOT_APPLY }

    enum ConsentFlowUserGeography { UNKNOWN, GDPR, OTHER }

    ConsentDialogState getConsentDialogState();
    ConsentFlowUserGeography getConsentFlowUserGeography();
    String getCountryCode();
    List<String> getEnabledAmazonAdUnitIds();
    boolean isTestModeEnabled();
}
