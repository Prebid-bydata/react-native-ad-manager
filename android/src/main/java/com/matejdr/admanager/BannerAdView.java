package com.matejdr.admanager;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import com.amazon.device.ads.AdError;
import com.amazon.device.ads.AdRegistration;
import com.amazon.device.ads.DTBAdCallback;
import com.amazon.device.ads.DTBAdNetwork;
import com.amazon.device.ads.DTBAdNetworkInfo;
import com.amazon.device.ads.DTBAdRequest;
import com.amazon.device.ads.DTBAdResponse;
import com.amazon.device.ads.DTBAdSize;
import com.amazon.device.ads.DTBAdUtil;
import com.amazon.device.ads.MRAIDPolicy;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.facebook.react.views.view.ReactViewGroup;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
import com.google.android.gms.ads.admanager.AdManagerAdView;
import com.google.android.gms.ads.admanager.AppEventListener;
import com.matejdr.admanager.customClasses.CustomTargeting;
import com.matejdr.admanager.utils.Targeting;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

class BannerAdView extends ReactViewGroup implements AppEventListener, LifecycleEventListener {

    protected AdManagerAdView adManagerAdView;
    protected DTBAdRequest loader;
    Activity currentActivityContext = null;

    String[] testDevices;
    AdSize[] validAdSizes;
    String adUnitID;
    String apsSlotId;
    AdSize adSize;
    String adsRefresh;
    int adsCount = 0;
    String TAG = "adsAsc";

    // Targeting
    Boolean isBannerAdsOn = true;
    Boolean hasTargeting = false;
    CustomTargeting[] customTargeting;
    String[] categoryExclusions;
    String[] keywords;
    String contentURL;
    String publisherProvidedID;
    Location location;
    String correlator;

    int top;
    int left;
    int width;
    int height;

    public BannerAdView(final Context context, ReactApplicationContext applicationContext) {
        super(context);
        currentActivityContext = applicationContext.getCurrentActivity();
        applicationContext.addLifecycleEventListener(this);
        this.createAdView();
    }

    private boolean isFluid() {
        return AdSize.FLUID.equals(this.adSize);
    }

    @Override
    public void requestLayout() {
        super.requestLayout();
        post(new MeasureAndLayoutRunnable());
    }

    private void createAdView() {
        if (this.adManagerAdView != null)
            this.adManagerAdView.destroy();
        if (this.currentActivityContext == null)
            return;
        Log.d(TAG, "createAdView:");
        this.adManagerAdView = new AdManagerAdView(currentActivityContext);

        if (isFluid()) {
            AdManagerAdView.LayoutParams layoutParams = new AdManagerAdView.LayoutParams(
                    ReactViewGroup.LayoutParams.MATCH_PARENT,
                    ReactViewGroup.LayoutParams.WRAP_CONTENT);
            this.adManagerAdView.setLayoutParams(layoutParams);
        }

        this.adManagerAdView.setAppEventListener(this);
        this.adManagerAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                Log.d(TAG, "ad received");
                if (isFluid()) {
                    top = 0;
                    left = 0;
                    width = getWidth();
                    height = getHeight();
                } else {
                    top = adManagerAdView.getTop();
                    left = adManagerAdView.getLeft();
                    width = adManagerAdView.getAdSize().getWidthInPixels(getContext());
                    height = adManagerAdView.getAdSize().getHeightInPixels(getContext());
                }

                if (!isFluid()) {
                    sendOnSizeChangeEvent();
                }

                int width = adManagerAdView.getAdSize().getWidthInPixels(getContext());
                int height = adManagerAdView.getAdSize().getHeightInPixels(getContext());
                int left = adManagerAdView.getLeft();
                int top = adManagerAdView.getTop();
                adManagerAdView.measure(width, height);
                adManagerAdView.layout(left, top, left + width, top + height);
                sendOnSizeChangeEvent();
                WritableMap ad = Arguments.createMap();
                ad.putString("type", "banner");

                ad.putString("isFluid", String.valueOf(isFluid()));

                WritableMap measurements = Arguments.createMap();
                measurements.putInt("adWidth", width);
                measurements.putInt("adHeight", height);
                measurements.putInt("width", getMeasuredWidth());
                measurements.putInt("height", getMeasuredHeight());
                measurements.putInt("left", left);
                measurements.putInt("top", top);
                ad.putMap("measurements", measurements);
                adsCount = adsCount + 1;
                sendEvent(RNAdManagerBannerViewManager.EVENT_AD_LOADED, ad);
            }

            @Override
            public void onAdFailedToLoad(LoadAdError adError) {
                String errorMessage = "Unknown error";
                switch (adError.getCode()) {
                    case AdManagerAdRequest.ERROR_CODE_INTERNAL_ERROR:
                        errorMessage = "Internal error, an invalid response was received from the ad server.";
                        break;
                    case AdManagerAdRequest.ERROR_CODE_INVALID_REQUEST:
                        errorMessage = "Invalid ad request, possibly an incorrect ad unit ID was given.";
                        break;
                    case AdManagerAdRequest.ERROR_CODE_NETWORK_ERROR:
                        errorMessage = "The ad request was unsuccessful due to network connectivity.";
                        break;
                    case AdManagerAdRequest.ERROR_CODE_NO_FILL:
                        errorMessage = "The ad request was successful, but no ad was returned due to lack of ad inventory.";
                        break;
                }
                Log.d(TAG, "onAdFailedToLoad: " + errorMessage);
                WritableMap event = Arguments.createMap();
                WritableMap error = Arguments.createMap();
                error.putString("message", errorMessage);
                event.putMap("error", error);
                sendEvent(RNAdManagerBannerViewManager.EVENT_AD_FAILED_TO_LOAD, event);
            }

            @Override
            public void onAdOpened() {
                WritableMap event = Arguments.createMap();
                sendEvent(RNAdManagerBannerViewManager.EVENT_AD_OPENED, event);
            }

            @Override
            public void onAdClosed() {
                WritableMap event = Arguments.createMap();
                sendEvent(RNAdManagerBannerViewManager.EVENT_AD_CLOSED, event);
            }

            @Override
            public void onAdClicked() {
                Log.d(TAG, "onAdClicked ");
            }
        });
        this.addView(this.adManagerAdView);
    }

    private void sendOnSizeChangeEvent() {
        int width;
        int height;
        WritableMap event = Arguments.createMap();
        AdSize adSize = this.adManagerAdView.getAdSize();
        width = adSize.getWidth();
        height = adSize.getHeight();
        event.putString("type", "banner");
        event.putDouble("width", width);
        event.putDouble("height", height);
        sendEvent(RNAdManagerBannerViewManager.EVENT_SIZE_CHANGE, event);
    }

    private void sendEvent(String name, @Nullable WritableMap event) {
        ReactContext reactContext = (ReactContext) getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                getId(),
                name,
                event);
        Log.d(TAG, "sendEvent: " + name);
    }

    public void loadBanner() {
        Log.d(TAG, "loadBanner: ");
        ArrayList<AdSize> adSizes = new ArrayList<AdSize>();
        if (this.adSize != null) {
            adSizes.add(this.adSize);
        }
        if (this.validAdSizes != null) {
            for (int i = 0; i < this.validAdSizes.length; i++) {
                if (!adSizes.contains(this.validAdSizes[i])) {
                    adSizes.add(this.validAdSizes[i]);
                }
            }
        }
        if (adSizes.size() == 0) {
            adSizes.add(AdSize.BANNER);
        }
        AdSize[] adSizesArray = adSizes.toArray(new AdSize[adSizes.size()]);
        this.adManagerAdView.setAdSizes(adSizesArray);

        makeInternalAdsRequest(adSizesArray);
    }

    private void makeInternalAdsRequest(AdSize[] adSizesArray) {
        Log.d(TAG, "makeInternalAdsRequest: " + adSizesArray);
        loader = new DTBAdRequest();
        loader.setSizes(new DTBAdSize(adSizesArray[0].getWidth(), adSizesArray[0].getHeight(), this.apsSlotId));
        if (this.adsRefresh.equals("1")) {
            loader.setAutoRefresh(30);
        }
        loader.loadAd(new DTBAdCallback() {
            @Override
            public void onFailure(AdError adError) {
                if (isBannerAdsOn) {
                    Log.d(TAG, "aps failed");
                    final AdManagerAdRequest.Builder adRequestBuilder = new AdManagerAdRequest.Builder();
                    List<String> testDevicesList = new ArrayList<>();
                    if (testDevices != null) {
                        for (int i = 0; i < testDevices.length; i++) {
                            String testDevice = testDevices[i];
                            if (testDevice == "SIMULATOR") {
                                testDevice = AdManagerAdRequest.DEVICE_ID_EMULATOR;
                            }
                            testDevicesList.add(testDevice);
                        }
                        RequestConfiguration requestConfiguration = new RequestConfiguration.Builder()
                                .setTestDeviceIds(testDevicesList)
                                .build();
                        MobileAds.setRequestConfiguration(requestConfiguration);
                    }
                    if (correlator == null) {
                        correlator = (String) Targeting.getCorelator(adUnitID);
                    }
                    Bundle bundle = new Bundle();
                    bundle.putString("correlator", correlator);
                    adRequestBuilder.addNetworkExtrasBundle(AdMobAdapter.class, bundle);
                    // Targeting
                    if (hasTargeting) {
                        if (customTargeting != null && customTargeting.length > 0) {
                            adRequestBuilder.addCustomTargeting("refreshIteration", String.valueOf(adsCount));
                            for (int i = 0; i < customTargeting.length; i++) {
                                String key = customTargeting[i].key;
                                if (!key.isEmpty()) {
                                    if (customTargeting[i].value != null && !customTargeting[i].value.isEmpty()) {
                                        adRequestBuilder.addCustomTargeting(key, customTargeting[i].value);
                                    } else if (customTargeting[i].values != null
                                            && !customTargeting[i].values.isEmpty()) {
                                        adRequestBuilder.addCustomTargeting(key, customTargeting[i].values);
                                    }
                                }
                            }
                        }
                    }
                    Log.d(TAG, "send targeting to ad server " + adRequestBuilder.build().getCustomTargeting());
                    this.adManagerAdView.loadAd(adRequestBuilder.build());
                }
            }

            @Override
            public void onSuccess(DTBAdResponse dtbAdResponse) {
                if (isBannerAdsOn) {
                    Log.d(TAG, "aps success");
                    final AdManagerAdRequest.Builder requestBuilder = DTBAdUtil.INSTANCE
                            .createAdManagerAdRequestBuilder(dtbAdResponse);
                    List<String> testDevicesList = new ArrayList<>();
                    if (testDevices != null) {
                        for (int i = 0; i < testDevices.length; i++) {
                            String testDevice = testDevices[i];
                            if (testDevice == "SIMULATOR") {
                                testDevice = AdManagerAdRequest.DEVICE_ID_EMULATOR;
                            }
                            testDevicesList.add(testDevice);
                        }
                        RequestConfiguration requestConfiguration = new RequestConfiguration.Builder()
                                .setTestDeviceIds(testDevicesList)
                                .build();
                        MobileAds.setRequestConfiguration(requestConfiguration);
                    }
                    if (correlator == null) {
                        correlator = (String) Targeting.getCorelator(adUnitID);
                    }
                    Bundle bundle = new Bundle();
                    bundle.putString("correlator", correlator);
                    requestBuilder.addNetworkExtrasBundle(AdMobAdapter.class, bundle);
                    // Targeting
                    if (hasTargeting) {
                        if (customTargeting != null && customTargeting.length > 0) {
                            requestBuilder.addCustomTargeting("refreshIteration", String.valueOf(adsCount));
                            for (int i = 0; i < customTargeting.length; i++) {
                                String key = customTargeting[i].key;
                                if (!key.isEmpty()) {
                                    if (customTargeting[i].value != null && !customTargeting[i].value.isEmpty()) {
                                        requestBuilder.addCustomTargeting(key, customTargeting[i].value);
                                    } else if (customTargeting[i].values != null
                                            && !customTargeting[i].values.isEmpty()) {
                                        requestBuilder.addCustomTargeting(key, customTargeting[i].values);
                                    }
                                }
                            }
                        }
                    }
                    Log.d(TAG, "send targeting to ad server " + requestBuilder.build().getCustomTargeting());
                    this.adManagerAdView.loadAd(requestBuilder.build());
                }
            }
        });
    }

    // private void requestBannerAds(DTBAdResponse dtbAdResponse,AdSize[]
    // adSizesArray){
    // Log.d(TAG, "requestBannerAds after auctions : "+adSizesArray);
    // AdManagerAdRequest.Builder adRequestBuilder = null;
    // if(dtbAdResponse != null){
    // adRequestBuilder =
    // DTBAdUtil.INSTANCE.createAdManagerAdRequestBuilder(dtbAdResponse);
    // }else {
    // adRequestBuilder = new AdManagerAdRequest.Builder();
    // }
    // List<String> testDevicesList = new ArrayList<>();
    // if (testDevices != null) {
    // for (int i = 0; i < testDevices.length; i++) {
    // String testDevice = testDevices[i];
    // if (testDevice == "SIMULATOR") {
    // testDevice = AdManagerAdRequest.DEVICE_ID_EMULATOR;
    // }
    // testDevicesList.add(testDevice);
    // }
    // RequestConfiguration requestConfiguration
    // = new RequestConfiguration.Builder()
    // .setTestDeviceIds(testDevicesList)
    // .build();
    // MobileAds.setRequestConfiguration(requestConfiguration);
    // }

    // if (correlator == null) {
    // correlator = (String) Targeting.getCorelator(adUnitID);
    // }
    // Bundle bundle = new Bundle();
    // bundle.putString("correlator", correlator);

    // adRequestBuilder.addNetworkExtrasBundle(AdMobAdapter.class, bundle);

    // // Targeting
    // if (hasTargeting) {
    // if (customTargeting != null && customTargeting.length > 0) {
    // adRequestBuilder.addCustomTargeting("refreshIteration",String.valueOf(adsCount));
    // for (int i = 0; i < customTargeting.length; i++) {
    // String key = customTargeting[i].key;
    // if (!key.isEmpty()) {
    // if (customTargeting[i].value != null && !customTargeting[i].value.isEmpty())
    // {
    // adRequestBuilder.addCustomTargeting(key, customTargeting[i].value);
    // } else if (customTargeting[i].values != null &&
    // !customTargeting[i].values.isEmpty()) {
    // adRequestBuilder.addCustomTargeting(key, customTargeting[i].values);
    // }
    // }
    // }
    // }
    // if (categoryExclusions != null && categoryExclusions.length > 0) {
    // for (int i = 0; i < categoryExclusions.length; i++) {
    // String categoryExclusion = categoryExclusions[i];
    // if (!categoryExclusion.isEmpty()) {
    // adRequestBuilder.addCategoryExclusion(categoryExclusion);
    // }
    // }
    // }
    // if (keywords != null && keywords.length > 0) {
    // for (int i = 0; i < keywords.length; i++) {
    // String keyword = keywords[i];
    // if (!keyword.isEmpty()) {
    // adRequestBuilder.addKeyword(keyword);
    // }
    // }
    // }
    // if (contentURL != null) {
    // adRequestBuilder.setContentUrl(contentURL);
    // }
    // if (publisherProvidedID != null) {
    // adRequestBuilder.setPublisherProvidedId(publisherProvidedID);
    // }
    // if (location != null) {
    // adRequestBuilder.setLocation(location);
    // }
    // }
    // this.adView.loadAd(adRequestBuilder.build());
    // }

    public void setAdUnitID(String adUnitID) {
        if (this.adUnitID != null) {
            // We can only set adUnitID once, so when it was previously set we have
            // to recreate the view
            this.createAdView();
        }
        this.adUnitID = adUnitID;
        this.adManagerAdView.setAdUnitId(adUnitID);
    }

    public void setAdsRefresh(String adsRefresh) {
        this.adsRefresh = adsRefresh;
    }

    public void setApsSlotId(String apsSlotId) {
        this.apsSlotId = apsSlotId;
    }

    public void setTestDevices(String[] testDevices) {
        this.testDevices = testDevices;
    }

    // Targeting
    public void setCustomTargeting(CustomTargeting[] customTargeting) {
        this.customTargeting = customTargeting;
    }

    public void setCategoryExclusions(String[] categoryExclusions) {
        this.categoryExclusions = categoryExclusions;
    }

    public void setKeywords(String[] keywords) {
        this.keywords = keywords;
    }

    public void setContentURL(String contentURL) {
        this.contentURL = contentURL;
    }

    public void setPublisherProvidedID(String publisherProvidedID) {
        this.publisherProvidedID = publisherProvidedID;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setAdSize(AdSize adSize) {
        this.adSize = adSize;
    }

    public void setValidAdSizes(AdSize[] adSizes) {
        this.validAdSizes = adSizes;
    }

    public void setCorrelator(String correlator) {
        this.correlator = correlator;
    }

    @Override
    public void onAppEvent(String name, String info) {
        WritableMap event = Arguments.createMap();
        event.putString("name", name);
        event.putString("info", info);
        sendEvent(RNAdManagerBannerViewManager.EVENT_APP_EVENT, event);
    }

    @Override
    public void onHostResume() {
        if (this.adManagerAdView != null) {
            if (!isBannerAdsOn) {
                Log.d(TAG, "rnResume");
                isBannerAdsOn = true;
                this.adManagerAdView.resume();
            }
        }
    }

    @Override
    public void onHostPause() {
        if (this.adManagerAdView != null) {
            Log.d(TAG, "rnPause");
            this.adManagerAdView.pause();
            isBannerAdsOn = false;
        }
    }

    @Override
    public void onHostDestroy() {
        if (this.adManagerAdView != null) {
            Log.d(TAG, "rnDestroy");
            this.currentActivityContext = null;
            this.adManagerAdView.destroy();
            this.loader.stop();
        }
    }

    private class MeasureAndLayoutRunnable implements Runnable {
        @Override
        public void run() {
            if (isFluid()) {
                adManagerAdView.measure(
                        MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.EXACTLY));
            } else {
                adManagerAdView.measure(width, height);
            }
            adManagerAdView.layout(left, top, left + width, top + height);
        }
    }
}
