package com.matejdr.admanager;

import android.app.Activity;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableNativeArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.matejdr.admanager.customClasses.CustomTargeting;
import com.matejdr.admanager.enums.TargetingEnums;
import com.matejdr.admanager.enums.TargetingEnums.TargetingTypes;
import com.matejdr.admanager.utils.Targeting;

import java.util.ArrayList;
import java.util.List;

public class RNAdManagerRewardedAd extends ReactContextBaseJavaModule {

    public static final String REACT_CLASS = "CTKRewardedAd";

    public static final String EVENT_AD_LOADED = "rewardAdLoaded";
    public static final String EVENT_AD_FAILED_TO_LOAD = "rewardAdFailedToLoad";
    public static final String EVENT_AD_OPENED = "rewardAdOpened";
    public static final String EVENT_AD_CLOSED = "rewardAdClosed";
    public static final String EVENT_REWARD = "rewardEarned";

    RewardedAd mRewardedAd;
    String[] testDevices;
    ReadableMap targeting;

    CustomTargeting[] customTargeting;
    String[] categoryExclusions;
    String[] keywords;
    String contentURL;
    String publisherProvidedID;
    Location location;
    String TAG = "adsAsc";

    String adUnitId;
    AdManagerAdRequest adRequest;
    ReactApplicationContext reactContext;

    private Promise mRequestAdPromise;

    public RNAdManagerRewardedAd(ReactApplicationContext reactContext) {
        super(reactContext);

        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    private void sendEvent(String eventName, @Nullable WritableMap params) {
        getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, params);
    }

    @ReactMethod
    public void setAdUnitID(String adUnitID) {
        if (adUnitId == null) {
            this.adUnitId = adUnitID;
        }
    }

    @ReactMethod
    public void setTestDevices(ReadableArray testDevices) {
        ReadableNativeArray nativeArray = (ReadableNativeArray) testDevices;
        ArrayList<Object> list = nativeArray.toArrayList();
        this.testDevices = list.toArray(new String[list.size()]);
    }

    @ReactMethod
    public void setTargeting(ReadableMap targetingObjects) {
        this.targeting = targetingObjects;

        ReadableMapKeySetIterator targetings = targetingObjects.keySetIterator();

        if (targetings.hasNextKey()) {
            for (
                ReadableMapKeySetIterator it = targetingObjects.keySetIterator();
                it.hasNextKey();
            ) {
                String targetingType = it.nextKey();

                if (targetingType.equals(TargetingEnums.getEnumString(TargetingTypes.CUSTOMTARGETING))) {
                    ReadableMap customTargetingObject = targetingObjects.getMap(targetingType);
                    CustomTargeting[] customTargetingArray = Targeting.getCustomTargeting(customTargetingObject);
                    this.customTargeting = customTargetingArray;
                }

                if (targetingType.equals(TargetingEnums.getEnumString(TargetingTypes.CATEGORYEXCLUSIONS))) {
                    ReadableArray categoryExclusionsArray = targetingObjects.getArray(targetingType);
                    ReadableNativeArray nativeArray = (ReadableNativeArray) categoryExclusionsArray;
                    ArrayList<Object> list = nativeArray.toArrayList();
                    this.categoryExclusions = list.toArray(new String[list.size()]);
                }

                if (targetingType.equals(TargetingEnums.getEnumString(TargetingTypes.KEYWORDS))) {
                    ReadableArray keywords = targetingObjects.getArray(targetingType);
                    ReadableNativeArray nativeArray = (ReadableNativeArray) keywords;
                    ArrayList<Object> list = nativeArray.toArrayList();
                    this.keywords = list.toArray(new String[list.size()]);
                }

                if (targetingType.equals(TargetingEnums.getEnumString(TargetingTypes.CONTENTURL))) {
                    String contentURL = targetingObjects.getString(targetingType);
                    this.contentURL = contentURL;
                }

                if (targetingType.equals(TargetingEnums.getEnumString(TargetingTypes.PUBLISHERPROVIDEDID))) {
                    String publisherProvidedID = targetingObjects.getString(targetingType);
                    this.publisherProvidedID = publisherProvidedID;
                }

                if (targetingType.equals(TargetingEnums.getEnumString(TargetingTypes.LOCATION))) {
                    ReadableMap locationObject = targetingObjects.getMap(targetingType);
                    Location location = Targeting.getLocation(locationObject);
                    this.location = location;
                }
            }
        }
    }

    private AdManagerAdRequest buildAdRequest() {
        AdManagerAdRequest.Builder adRequestBuilder = new AdManagerAdRequest.Builder();

        if (testDevices != null) {
            List<String> testDevicesList = new ArrayList<>();

            for (int i = 0; i < testDevices.length; i++) {
                String testDevice = testDevices[i];
                if (testDevice == "SIMULATOR") {
                    testDevice = AdManagerAdRequest.DEVICE_ID_EMULATOR;
                }
                testDevicesList.add(testDevice);
            }
            RequestConfiguration requestConfiguration
                = new RequestConfiguration.Builder()
                .setTestDeviceIds(testDevicesList)
                .build();
            MobileAds.setRequestConfiguration(requestConfiguration);
        }

        if (customTargeting != null && customTargeting.length > 0) {
            for (int i = 0; i < customTargeting.length; i++) {
                String key = customTargeting[i].key;
                if (!key.isEmpty()) {
                    if (customTargeting[i].value != null && !customTargeting[i].value.isEmpty()) {
                        adRequestBuilder.addCustomTargeting(key, customTargeting[i].value);
                    } else if (customTargeting[i].values != null && !customTargeting[i].values.isEmpty()) {
                        adRequestBuilder.addCustomTargeting(key, customTargeting[i].values);
                    }
                }
            }
        }
        if (categoryExclusions != null && categoryExclusions.length > 0) {
            for (int i = 0; i < categoryExclusions.length; i++) {
                String categoryExclusion = categoryExclusions[i];
                if (!categoryExclusion.isEmpty()) {
                    adRequestBuilder.addCategoryExclusion(categoryExclusion);
                }
            }
        }
        if (keywords != null && keywords.length > 0) {
            for (int i = 0; i < keywords.length; i++) {
                String keyword = keywords[i];
                if (!keyword.isEmpty()) {
                    adRequestBuilder.addKeyword(keyword);
                }
            }
        }
        if (contentURL != null) {
            adRequestBuilder.setContentUrl(contentURL);
        }
        if (publisherProvidedID != null) {
            adRequestBuilder.setPublisherProvidedId(publisherProvidedID);
        }
        // if (location != null) {
        //     adRequestBuilder.setLocation(location);
        // }

        adRequest = adRequestBuilder.build();

        this.adRequest = adRequest;

        return adRequest;
    }

    @ReactMethod
    public void requestAd(final Promise promise) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (mRewardedAd != null) {
                    promise.reject("E_AD_ALREADY_LOADED", "Ad is already loaded.");
                } else {
                    mRequestAdPromise = promise;
                    buildAdRequest();

                    RewardedAd.load(reactContext, adUnitId, adRequest, new RewardedAdLoadCallback() {
                        @Override
                        public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                            WritableMap event = Arguments.createMap();
                            event.putString("type", "rewardedads");
                            sendEvent(EVENT_AD_LOADED, event);
                            if (mRequestAdPromise != null) {
                                mRequestAdPromise.resolve(null);
                                mRequestAdPromise = null;
                            }

                            mRewardedAd = rewardedAd;
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            String errorString = "ERROR_UNKNOWN";
                            String errorMessage = "Unknown error";
                            switch (loadAdError.getCode()) {
                                case AdManagerAdRequest.ERROR_CODE_INTERNAL_ERROR:
                                    errorString = "ERROR_CODE_INTERNAL_ERROR";
                                    errorMessage = "Internal error, an invalid response was received from the ad server.";
                                    break;
                                case AdManagerAdRequest.ERROR_CODE_INVALID_REQUEST:
                                    errorString = "ERROR_CODE_INVALID_REQUEST";
                                    errorMessage = "Invalid ad request, possibly an incorrect ad unit ID was given.";
                                    break;
                                case AdManagerAdRequest.ERROR_CODE_NETWORK_ERROR:
                                    errorString = "ERROR_CODE_NETWORK_ERROR";
                                    errorMessage = "The ad request was unsuccessful due to network connectivity.";
                                    break;
                                case AdManagerAdRequest.ERROR_CODE_NO_FILL:
                                    errorString = "ERROR_CODE_NO_FILL";
                                    errorMessage = "The ad request was successful, but no ad was returned due to lack of ad inventory.";
                                    break;
                            }
                            WritableMap event = Arguments.createMap();
                            WritableMap error = Arguments.createMap();
                            event.putString("message", errorMessage);
                            sendEvent(EVENT_AD_FAILED_TO_LOAD, event);
                            if (mRequestAdPromise != null) {
                                mRequestAdPromise.reject(errorString, errorMessage);
                                mRequestAdPromise = null;
                            }
                            mRewardedAd = null;
                        }

                    });
                }
            }
        });
    }

    @ReactMethod
    public void showAd(final Promise promise) {

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (mRewardedAd != null) {
                    mRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdClicked() {
                          // Called when a click is recorded for an ad.
                          Log.d(TAG, "Ad was clicked.");
                        }

                         @Override
                        public void onAdDismissedFullScreenContent() {
                          // Called when ad is dismissed.
                          // Set the ad reference to null so you don't show the ad a second time.
                          Log.d(TAG, "Ad dismissed fullscreen content.");
                          mRewardedAd = null;
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(AdError adError) {
                          // Called when ad fails to show.
                          Log.e(TAG, "Ad failed to show fullscreen content.");
                          mRewardedAd = null;
                        }

                        @Override
                        public void onAdImpression() {
                          // Called when an impression is recorded for an ad.
                          Log.d(TAG, "Ad recorded an impression.");
                        }

                        @Override
                        public void onAdShowedFullScreenContent() {
                          // Called when ad is shown.
                          Log.d(TAG, "Ad showed fullscreen content.");
                        }
                      });

                    final Activity activity = getCurrentActivity();
                    mRewardedAd.show(activity, new OnUserEarnedRewardListener() {
                        @Override
                        public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                          // Handle the reward.
                          Log.d(TAG, "The user earned the reward.");
                          int rewardAmount = rewardItem.getAmount();
                          String rewardType = rewardItem.getType();
                          Log.d(TAG, "The user earned the reward."  +rewardType);

                          WritableMap event = Arguments.createMap();
                          event.putString("message", rewardType + " - " + String.valueOf(rewardAmount));
                          sendEvent(EVENT_REWARD, event);
                        }
                    });
                    promise.resolve(null);
                } else {
                    promise.reject("E_AD_NOT_READY", "The rewarded ad wasn't ready yet.");
                }
            }
        });
    }

    @ReactMethod
    public void isReady(final Callback callback) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                callback.invoke(mRewardedAd);
            }
        });
    }
}
