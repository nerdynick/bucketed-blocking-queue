package com.nerdynick;

import com.nerdynick.gauges.RateGauge;

public class BucketSensors {
	public static class RateLimited implements BucketSensor {
		final RateGauge gauge = new RateGauge();
		final long rate;
		
		public RateLimited(final long rate) {
			this.rate = rate;
		}
		
		public void onOffer() {
			gauge.incr();
		}

		public boolean isReady() {
			return gauge.currentRate() >= rate ? false : true;
		}
	}
	
	public static BucketSensor RateLimited(final long rate) {
		return new RateLimited(rate);
	}
}
