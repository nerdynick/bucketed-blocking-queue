package com.nerdynick;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public class BucketSensors {
	public static class RateGauge {
		private static long tickInterval = TimeUnit.SECONDS.toNanos(1);
		LongAdder adder = new LongAdder();
		long lastTick = 0;
		
		private long tick() {
			long now = System.nanoTime();
			long diff = now-lastTick;
			if(diff > tickInterval) {
				lastTick = now;
				adder.reset();
			}
			return diff;
		}
		
		public void incr() {
			tick();
			adder.increment();
		}
		
		public long currentRate() {
			long diff = tick();
			return adder.sum()/TimeUnit.NANOSECONDS.toSeconds(diff);
		}
		
	}
	
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
