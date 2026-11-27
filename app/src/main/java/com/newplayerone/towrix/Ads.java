package com.newplayerone.towrix;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Anuncios con recompensa (AdMob) + consentimiento (UMP).
 * El juego los usa a través del puente JavaScript "AndroidAds":
 *   AndroidAds.isRewardedReady()          ¿hay un video listo?
 *   AndroidAds.showRewarded("lugar")      muestra el video; al cerrar llama a
 *                                          window.onRewardedResult("lugar", true|false)
 *   AndroidAds.privacyOptionsRequired()   ¿hay que ofrecer "Privacidad" en el menú?
 *   AndroidAds.showPrivacyOptions()       abre el formulario de privacidad
 * Cuando un video queda listo se llama a window.onRewardedReady(true).
 *
 * Los IDs vienen del AndroidManifest (ver app/build.gradle y gradle.properties).
 */
class Ads {
    private static final String TAG = "TowrixAds";

    private final Activity activity;
    private final WebView web;
    private final ConsentInformation consent;
    private final AtomicBoolean sdkStarted = new AtomicBoolean(false);
    private final String rewardedUnitId;

    private RewardedAd rewardedAd;
    private boolean loading;
    private volatile boolean ready;

    Ads(Activity activity, WebView web) {
        this.activity = activity;
        this.web = web;
        this.consent = UserMessagingPlatform.getConsentInformation(activity);
        this.rewardedUnitId = readMeta(activity, "com.newplayerone.towrix.ADMOB_REWARDED_ID");
    }

    /** Llamar en cada inicio de la app (lo pide Google): consentimiento y luego anuncios. */
    void start() {
        ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();
        consent.requestConsentInfoUpdate(
                activity,
                params,
                () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity, formError -> {
                    if (formError != null) Log.w(TAG, "Consentimiento: " + formError.getMessage());
                    if (consent.canRequestAds()) startSdk();
                }),
                requestError -> Log.w(TAG, "Consentimiento: " + requestError.getMessage()));
        // Si ya había consentimiento de una sesión anterior, se puede partir de inmediato
        if (consent.canRequestAds()) startSdk();
    }

    private void startSdk() {
        if (sdkStarted.getAndSet(true)) return;
        // Contenido de anuncios apto para mayores de 13 años (público objetivo declarado en Play)
        MobileAds.setRequestConfiguration(new RequestConfiguration.Builder()
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_T)
                .build());
        new Thread(() -> {
            MobileAds.initialize(activity, status -> { });
            activity.runOnUiThread(this::loadRewarded);
        }).start();
    }

    private void loadRewarded() {
        if (rewardedAd != null || loading || rewardedUnitId == null || !consent.canRequestAds()) return;
        loading = true;
        RewardedAd.load(activity, rewardedUnitId, new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(RewardedAd ad) {
                rewardedAd = ad;
                loading = false;
                ready = true;
                web.evaluateJavascript("window.onRewardedReady&&window.onRewardedReady(true)", null);
            }

            @Override
            public void onAdFailedToLoad(LoadAdError error) {
                rewardedAd = null;
                loading = false;
                ready = false;
                Log.w(TAG, "No se cargó el video: " + error.getMessage());
                // Reintento suave en 60 s
                web.postDelayed(Ads.this::loadRewarded, 60_000);
            }
        });
    }

    private void sendResult(String place, boolean earned) {
        web.evaluateJavascript("window.onRewardedResult&&window.onRewardedResult('" + place + "'," + earned + ")", null);
    }

    // ---------- Puente JavaScript (se llama desde un hilo del WebView) ----------

    @JavascriptInterface
    public boolean isRewardedReady() {
        return ready;
    }

    @JavascriptInterface
    public void showRewarded(String placeRaw) {
        final String place = placeRaw == null ? "" : placeRaw.replaceAll("[^a-z0-9_]", "");
        activity.runOnUiThread(() -> {
            RewardedAd ad = rewardedAd;
            if (ad == null) { sendResult(place, false); loadRewarded(); return; }
            rewardedAd = null;
            ready = false;
            final boolean[] earned = {false};
            ad.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    sendResult(place, earned[0]);
                    loadRewarded();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(AdError adError) {
                    sendResult(place, false);
                    loadRewarded();
                }
            });
            ad.show(activity, rewardItem -> earned[0] = true);
        });
    }

    @JavascriptInterface
    public boolean privacyOptionsRequired() {
        return consent.getPrivacyOptionsRequirementStatus()
                == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
    }

    @JavascriptInterface
    public void showPrivacyOptions() {
        activity.runOnUiThread(() -> UserMessagingPlatform.showPrivacyOptionsForm(activity, formError -> {
            if (consent.canRequestAds()) startSdk();
        }));
    }

    private static String readMeta(Activity a, String key) {
        try {
            ApplicationInfo ai = a.getPackageManager().getApplicationInfo(a.getPackageName(), PackageManager.GET_META_DATA);
            Bundle b = ai.metaData;
            return b != null ? b.getString(key) : null;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }
}
