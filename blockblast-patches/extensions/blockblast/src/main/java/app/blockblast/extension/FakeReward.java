/*
 * Block Blast patches (Morphe). Licensed under the GNU General Public License v3.0.
 */
package app.blockblast.extension;

import com.applovin.mediation.MaxReward;

/** The reward of a fake rewarded ad. Block Blast only checks that a reward is given. */
final class FakeReward implements MaxReward {
    @Override
    public int getAmount() {
        return 1;
    }

    @Override
    public String getLabel() {
        return "";
    }
}
