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

		@Override
		public void onTake() {}

		@Override
		public boolean canTake() {
			return gauge.currentRate() >= rate ? false : true;
		}

		@Override
		public boolean canOffer() {
			return true;
		}
	}
	
	public static BucketSensor RateLimited(final long rate) {
		return new RateLimited(rate);
	}
}
