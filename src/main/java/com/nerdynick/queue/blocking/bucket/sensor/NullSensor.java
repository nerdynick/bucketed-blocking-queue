package com.nerdynick.queue.blocking.bucket.sensor;

import java.util.concurrent.TimeUnit;

public class NullSensor implements BucketSensor {
	private static final NullSensor _instance = new NullSensor();
	public static NullSensor instance() {
		return _instance;
	}
	
	@Override
	public void onOffer() {}
	@Override
	public void onOffer(long count) {}
	@Override
	public void onTake() {}

	@Override
	public boolean canTake() {
		return true;
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
		return true;
	}
}