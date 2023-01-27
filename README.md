# @ascendeum_ads/react-native-gam-aps

A react-native module for Google Ad Manager With APS Banners, Interstitials and Native ads.

The banner types are implemented as components while the interstitial and rewarded video have an imperative API.

Native ads are implemented as wrapper for a native view.

## Installation

You can use npm or Yarn to install the latest version.

**npm:**

    npm i --save @ascendeum_ads/react-native-gam-aps

**Yarn:**

    yarn add @ascendeum_ads/react-native-gam-aps

### iOS

For iOS you will have to add the [Google Mobile Ads SDK](https://developers.google.com/ad-manager/mobile-ads-sdk/ios/quick-start#import_the_mobile_ads_sdk) and amazon publisher services setup to your Xcode project.


### Android

On Android the Ad Manager library code is part of Play Services, which is automatically added when this library is linked.

But you still have to manually update your `AndroidManifest.xml`, as described in the [Google Mobile Ads SDK documentation](https://developers.google.com/ad-manager/mobile-ads-sdk/android/quick-start#import_the_mobile_ads_sdk).
and described in  amazon publisher services setup for android.

###GAM

**iOS**

Activate as Ad Manager app by editing your Info.plist
```
+ <key>GADIsAdManagerApp</key>
+ <true/>
```
Add transport security rules in Info.plist
```
<key>NSAppTransportSecurity</key>
<dict>
<key>NSAllowsArbitraryLoads</key>
<true/>
<key>NSAllowsArbitraryLoadsForMedia</key>
<true/>
<key>NSAllowsArbitraryLoadsInWebContent</key>
<true/>
</dict>
```

**Android(())

Activate as Ad Manager app
```
<manifest>
  <application>

+   <meta-data android:name="com.google.android.gms.ads.AD_MANAGER_APP" android:value="true"/>

  </application>
</manifest>
```

## Usage

```jsx
import {
  Banner,
  Interstitial,
  PublisherBanner,
  NativeAdsManager,
} from '@ascendeum_ads/react-native-gam-aps'

// Display a DFP Publisher banner
<Banner
  adSize="fullBanner"
  adUnitID="your-ad-unit-id"
  apsSlotId = "your-ad-aps-slot-uuid"
  adsRefresh= "pass 1/0 for ads refresh or not"
  testDevices={[PublisherBanner.simulatorId]}
  onAdFailedToLoad={error => console.error(error)}
  onAppEvent={event => console.log(event.name, event.info)}
/>

// Display an interstitial
Interstitial.setAdUnitID('your-ad-unit-id');
Interstitial.setTestDevices([Interstitial.simulatorId]);
Interstitial.requestAd().then(() => Interstitial.showAd());

// Native ad
import NativeAdView from './NativeAdView';
const adsManager = new NativeAdsManager('your-ad-unit-id', [
    Interstitial.simulatorId,
]);
<NativeAdView
    targeting={{
        customTargeting: {group: 'user_test'},
        categoryExclusions: ['media'],
        contentURL: 'test://',
        publisherProvidedID: 'provider_id',
    }}
    style={{width: '100%'}}
    adsManager={adsManager}
    validAdTypes={['native', 'template']}
    customTemplateIds={['your-template-id-1', 'your-template-id-2']}
    onAdLoaded={ad => {
        console.log(ad);
    }}
    onAdFailedToLoad={error => {
        console.log(error);
    }}
/>
```

See the NativeAdView component in the [example NativeAdView](example/NativeAdView.js).
For a full example reference to the [example project](example).

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT
