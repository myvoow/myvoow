# Block Blast patches

Morphe patches for **Block Blast!** (`com.block.juggle`), developed with 11.0.6.

| Patch | What it does |
| --- | --- |
| **Remove ads** | Fakes AppLovin MAX, which mediates every ad in the game. MAX and the ad networks behind it are never started. Interstitials and app open ads close as soon as they would show, banners and native ads never load, and ad servers (including Hungry Studio's own) are blocked. Rewarded ads give their reward instantly, so revives and the other rewarded bonuses cost no video. |
| **Disable telemetry** | Blocks Firebase Analytics, Crashlytics, AppsFlyer, Bugsnag, ThinkingData, TikTok, Facebook app events, SDK metrics and Hungry Studio's trackers (gamedot, apm-receiver, mediation-receiver, cross-sync). Turns off the collection flags in the manifest and removes the advertising id and install referrer permissions. Login, cloud save, purchases, push and Remote Config still work. |

## How it works

- `extensions/blockblast`: Java code merged into the app.
  - `Ads` stands in for the MAX SDK: a fake init callback, and fullscreen ads that load and close at once, with the reward for rewarded ads.
  - `NetBlock` filters by host. Every `URL.openConnection()` in the app is routed through it, and every OkHttp `Dns.lookup` checks with it first, so it works on obfuscated SDKs too.
- `patches`: the Morphe patches that hook the AppLovin MAX API (`MaxRewardedAd`, `MaxInterstitialAd`, `MaxAppOpenAd`, `MaxAdView`, `MaxNativeAdLoader`, `AppLovinSdk`) and edit the manifest.
- `stub`: compile-only signatures of the AppLovin MAX API.

## Limits

- **Revives are free, not unlimited.** Revives are capped by the game logic, which runs in encrypted Cocos JavaScript (`assets/**/index.*.js`, `HEK:` header). The patches only remove the ad in front of each revive.
- **Anti-tamper.** The app is packed with NetEase Yidun (`com.netease.nis.wrapper`, `libnesec.so`), which loads part of the code from `assets/nedata.db`. A re-signed APK can be flagged or refused by its checks. These patches do not touch the packer.
- Not tested on a device yet. The patches apply cleanly to 11.0.6, and the output was checked by decompiling it.

## Install

On your phone, open <https://morphe.software/add-source?github=myvoow/blockblast-patches>, or paste `https://github.com/myvoow/blockblast-patches` in Morphe Manager when you add a patch source. Then choose Block Blast! and give it the XAPK.

## Build and release

Every push to `main` runs `release.yml`: it builds `patches-<version>.mpp`, creates the GitHub Release and updates `patches-bundle.json`, which Morphe Manager reads. The repository must be public. Use semantic commit messages (`feat:`, `fix:`, `chore:`).

Locally, with a GitHub token that has `read:packages` (Morphe is published on GitHub Packages):

```sh
export GITHUB_ACTOR=<user> GITHUB_TOKEN=<token>
./gradlew :patches:buildAndroid
```
