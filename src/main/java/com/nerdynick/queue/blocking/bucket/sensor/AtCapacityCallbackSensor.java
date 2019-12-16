package com.nerdynick.queue.blocking.bucket.sensor;

import java.util.concurrent.TimeUnit;

public class AtCapacityCallbackSensor implements BucketSensor {
	
	public AtCapacityCallbackSensor() {
		
	}

	@Override
	public void onOffer() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onOffer(long count) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTake() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean canTake() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canOffer() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canOfferWait(long timeout, TimeUnit unit) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canOfferWait() {
		// TODO Auto-generated method stub
		return false;
	}

}
