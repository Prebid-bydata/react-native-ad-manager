import { NativeEventEmitter, NativeModules } from 'react-native';
import { createErrorFromErrorData } from './utils';
import type {
  IAdManagerEventBase,
  IAdManagerEventErrorPayload,
  IAdManagerEventLoadedRewardedInterstitial,
} from './AdManagerEvent';
import { LINKING_ERROR } from './Constants';
import type { IAdManagerTargeting } from './AdManagerTypes';

const CTKRewardedInterstitial = NativeModules.CTKRewardedInterstitial
  ? NativeModules.RewardedInterstitial
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

const eventEmitter = new NativeEventEmitter(CTKRewardedInterstitial);

const eventMap = {
  adLoaded: 'rewardinterstitialAdLoaded',
  adFailedToLoad: 'rewardinterstitialAdFailedToLoad',
  adOpened: 'rewardinterstitialAdOpened',
  adClosed: 'rewardinterstitialAdClosed',
  rewardEarned: 'rewardEarned'
};

type TAdManagerRewardInterstitialEvent =
  | 'adLoaded'
  | 'adFailedToLoad'
  | 'rewardEarned'
  | 'adOpened'
  | 'adClosed';

type TAdManagerRewardInterstitialHandler = (
  event: Error | IAdManagerEventBase | IAdManagerEventLoadedRewardedInterstitial
) => void;

const _subscriptions = new Map();

const addEventListener = (
  event: TAdManagerRewardInterstitialEvent,
  handler: TAdManagerRewardInterstitialHandler
) => {
  const mappedEvent = eventMap[event];
  if (mappedEvent) {
    let listener;
    if (event === 'adFailedToLoad') {
      listener = eventEmitter.addListener(
        mappedEvent,
        (error: IAdManagerEventErrorPayload) =>
          handler(createErrorFromErrorData(error))
      );
    } else if(event === 'rewardEarned') {
        listener = eventEmitter.addListener(
          mappedEvent,
          (error: IAdManagerEventErrorPayload) =>
            handler(createErrorFromErrorData(error))
        );
    } else {
      listener = eventEmitter.addListener(mappedEvent, handler);
    }
    _subscriptions.set(handler, listener);
    return {
      remove: () => removeEventListener(event, handler),
    };
  } else {
    console.warn(`Trying to subscribe to unknown event: "${event}"`);
    return {
      remove: () => {},
    };
  }
};

const removeEventListener = (
  _event: TAdManagerRewardInterstitialEvent,
  handler: TAdManagerRewardInterstitialHandler
) => {
  const listener = _subscriptions.get(handler);
  if (!listener) {
    return;
  }
  listener.remove();
  _subscriptions.delete(handler);
};

const removeAllListeners = () => {
  _subscriptions.forEach((listener, key, map) => {
    listener.remove();
    map.delete(key);
  });
};

const simulatorId = 'SIMULATOR';

const setAdUnitID = (adUnitID: string) => {
  CTKRewardedInterstitial.setAdUnitID(adUnitID);
};

const setTestDevices = (testDevices: string[]) => {
  CTKRewardedInterstitial.setTestDevices(testDevices);
};

const setTargeting = (targeting: IAdManagerTargeting) => {
    CTKRewardedInterstitial.setTargeting(targeting);
};

const requestAd = (): Promise<null> => {
  return CTKRewardedInterstitial.requestAd();
}

const showAd = (): Promise<null> => {
  return CTKRewardedInterstitial.showAd();
}

const isReady = (callback: (isReady: number) => void): Promise<null> => {
  return CTKRewardedInterstitial.isReady(callback);
}

export default {
  addEventListener,
  removeEventListener,
  removeAllListeners,
  simulatorId,
  setAdUnitID,
  setTestDevices,
  setTargeting,
  requestAd,
  showAd,
  isReady
}
