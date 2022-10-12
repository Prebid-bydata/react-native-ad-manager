import {
  arrayOf,
  bool,
  func,
  instanceOf,
  number,
  object,
  oneOf,
  shape,
  string,
} from 'prop-types';
import React, { Component } from 'react';
import {
  findNodeHandle,
  requireNativeComponent,
  UIManager,
  ViewPropTypes,
} from 'react-native';
import { createErrorFromErrorData } from './utils';

class Banner extends Component {
  constructor() {
    super();
    this.handleSizeChange = this.handleSizeChange.bind(this);
    this.handleAppEvent = this.handleAppEvent.bind(this);
    this.handleAdFailedToLoad = this.handleAdFailedToLoad.bind(this);
    this.state = {
      style: {},
    };

    this.handleOnAdLoaded = ({ nativeEvent }) => {
      this.props.onAdLoaded &&
      this.props.onAdLoaded(nativeEvent);
      // console.log(nativeEvent)
    };
  }

  shouldComponentUpdate(nextProps, nextState) {
    if (Object.entries(this.state.style).toString() === Object.entries(nextState.style).toString()
    && Object.entries(this.props).toString() === Object.entries(nextProps).toString()) {
      return false;
    }
    return true;
  }

  componentDidMount() {
    this.loadBanner();
  }

  loadBanner() {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(this._bannerView),
      UIManager.getViewManagerConfig('CTKBannerView').Commands.loadBanner,
      null
    );
  }

  handleSizeChange({ nativeEvent }) {
    const { height, width, type } = nativeEvent;
    this.setState({ style: { width, height } });
    if (this.props.onSizeChange) {
      this.props.onSizeChange(nativeEvent);
    }
  }

  handleAppEvent(event) {
    if (this.props.onAppEvent) {
      const { name, info } = event.nativeEvent;
      this.props.onAppEvent({ name, info });
    }
  }

  handleAdFailedToLoad(event) {
    if (this.props.onAdFailedToLoad) {
      this.props.onAdFailedToLoad(
        createErrorFromErrorData(event.nativeEvent.error)
      );
    }
  }

  render() {
    return (
      <CTKBannerView
        {...this.props}
        style={[this.props.style, this.state.style]}
        onSizeChange={this.handleSizeChange}
        onAdLoaded={this.handleOnAdLoaded}
        onAdFailedToLoad={this.handleAdFailedToLoad}
        onAppEvent={this.handleAppEvent}
        ref={(el) => (this._bannerView = el)}
      />
    );
  }
}

Banner.simulatorId = 'SIMULATOR';

Banner.propTypes = {
  ...ViewPropTypes,

  /**
   * DFP iOS library banner size constants
   * (https://developers.google.com/admob/ios/banner)
   * banner (320x50, Standard Banner for Phones and Tablets)
   * fullBanner (468x60, IAB Full-Size Banner for Tablets)
   * largeBanner (320x100, Large Banner for Phones and Tablets)
   * mediumRectangle (300x250, IAB Medium Rectangle for Phones and Tablets)
   * leaderboard (728x90, IAB Leaderboard for Tablets)
   * skyscraper (120x600, Skyscraper size for the iPad. Mediation only. AdMob/Google does not offer this size)
   * fluid (An ad size that spans the full width of its container, with a height dynamically determined by the ad)
   * {\d}x{\d} (Dynamic size determined byt the user, 300x250, 300x100 etc.)
   *
   * banner is default
   */
  adSize: string,

  /**
   * Optional array specifying all valid sizes that are appropriate for this slot.
   */
  validAdSizes: arrayOf(string),

  /**
   * DFP ad unit ID
   */
  adUnitID: string,

  /**
   * Array of test devices. Use Banner.simulatorId for the simulator
   */
  testDevices: arrayOf(string),

  onSizeChange: func,

  /**
   * DFP library events
   */
  onAdLoaded: func,
  onAdFailedToLoad: func,
  onAdOpened: func,
  onAdClosed: func,
  onAppEvent: func,

  targeting: shape({
    /**
     * Arbitrary object of custom targeting information.
     */
    customTargeting: object,

    /**
     * Array of exclusion labels.
     */
    categoryExclusions: arrayOf(string),

    /**
     * Array of keyword strings.
     */
    keywords: arrayOf(string),

    /**
     * Applications that monetize content matching a webpage's content may pass
     * a content URL for keyword targeting.
     */
    contentURL: string,

    /**
     * You can set a publisher provided identifier (PPID) for use in frequency
     * capping, audience segmentation and targeting, sequential ad rotation, and
     * other audience-based ad delivery controls across devices.
     */
    publisherProvidedID: string,

    /**
     * The user’s current location may be used to deliver more relevant ads.
     */
    location: shape({
      latitude: number,
      longitude: number,
      accuracy: number,
    }),
    correlator: string,
  }),

  /**
   * Adrefresh
   */
   adsRefresh:string,
   
  /**
   * APS library events
  */
  apsSlotId:string,
};

const CTKBannerView = requireNativeComponent(
  'CTKBannerView',
  Banner
);

export default Banner;
