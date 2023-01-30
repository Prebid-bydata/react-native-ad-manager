import { NativeEventEmitter, NativeModules } from 'react-native';
import { createErrorFromErrorData } from './utils';
import type {
  IAdManagerEventBase,
  IAdManagerEventErrorPayload,
  IAdManagerEventLoadedRewardedAd,
} from './AdManagerEvent';
import { LINKING_ERROR } from './Constants';
import type { IAdManagerTargeting } from './AdManagerTypes';

const CTKRewardedAd = NativeModules.CTKRewardedAd
  ? NativeModules.CTKRewardedAd
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

const eventEmitter = new NativeEventEmitter(CTKRewardedAd);

const eventMap = {
  adLoaded: 'rewardAdLoaded',
  adFailedToLoad: 'rewardAdFailedToLoad',
  adOpened: 'rewardAdOpened',
  adClosed: 'rewardAdClosed',
};

type TAdManagerRewardedEvent =
  | 'adLoaded'
  | 'adFailedToLoad'
  | 'adOpened'
  | 'adClosed';

type TAdManagerRewardAdHandler = (
  event: Error | IAdManagerEventBase | IAdManagerEventLoadedRewardedAd
) => void;

const _subscriptions = new Map();

const addEventListener = (
  event: TAdManagerRewardedEvent,
  handler: TAdManagerRewardAdHandler
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
  _event: TAdManagerRewardedEvent,
  handler: TAdManagerRewardAdHandler
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
    CTKRewardedAd.setAdUnitID(adUnitID);
};

const setTestDevices = (testDevices: string[]) => {
    CTKRewardedAd.setTestDevices(testDevices);
};

const setTargeting = (targeting: IAdManagerTargeting) => {
    CTKRewardedAd.setTargeting(targeting);
};

const requestAd = (): Promise<null> => {
  return CTKRewardedAd.requestAd();
}

const showAd = (): Promise<null> => {
  return CTKRewardedAd.showAd();
}

const isReady = (callback: (isReady: number) => void): Promise<null> => {
  return CTKRewardedAd.isReady(callback);
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
