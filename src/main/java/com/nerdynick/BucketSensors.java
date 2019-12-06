package com.nerdynick;

import java.util.concurrent.TimeUnit;

import com.nerdynick.gauges.RateGauge;

public class BucketSensors {
	public static class RateLimited implements BucketSensor {
		final RateGauge gauge;
		final long rate;
		
		public RateLimited(final long rate, final TimeUnit unit) {
			gauge = new RateGauge(1, unit);
			this.rate = rate;
		}
		
		@Override
		public void onOffer() {
			gauge.incr();
		}
		@Override
		public void onOffer(long count) {
			gauge.add(count);
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
		

		@Override
		public boolean canOfferWait(long timeout, TimeUnit unit) {
			return true;
		}

		@Override
		public boolean canOfferWait() {
			return false;
		}

		public static RateLimited perHour(final long rate) {
			return new RateLimited(rate, TimeUnit.HOURS);
		}
		public static RateLimited perMinute(final long rate) {
			return new RateLimited(rate, TimeUnit.MINUTES);
		}
		public static RateLimited perSecond(final long rate) {
			return new RateLimited(rate, TimeUnit.SECONDS);
		}
		public static RateLimited perNano(final long rate) {
			return new RateLimited(rate, TimeUnit.NANOSECONDS);
		}
		
	}
}
