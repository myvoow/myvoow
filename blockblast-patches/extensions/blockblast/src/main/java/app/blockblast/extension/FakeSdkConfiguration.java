/*
 * Block Blast patches (Morphe). Licensed under the GNU General Public License v3.0.
 */
package app.blockblast.extension;

import com.applovin.sdk.AppLovinSdkConfiguration;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** The configuration the app gets instead of initializing AppLovin MAX. */
final class FakeSdkConfiguration implements AppLovinSdkConfiguration {
    @Override
    @SuppressWarnings("deprecation")
    public ConsentDialogState getConsentDialogState() {
        return ConsentDialogState.DOES_NOT_APPLY;
    }

    @Override
    public ConsentFlowUserGeography getConsentFlowUserGeography() {
        return ConsentFlowUserGeography.OTHER;
    }

    @Override
    public String getCountryCode() {
        String country = Locale.getDefault().getCountry();
        return country == null || country.isEmpty() ? "US" : country.toUpperCase(Locale.ROOT);
    }

    @Override
    public List<String> getEnabledAmazonAdUnitIds() {
        return Collections.emptyList();
    }

    @Override
    public boolean isTestModeEnabled() {
        return false;
    }
}
