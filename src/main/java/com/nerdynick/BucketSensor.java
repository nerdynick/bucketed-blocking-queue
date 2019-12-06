package com.nerdynick;

import java.util.concurrent.TimeUnit;

public interface BucketSensor {
	public void onOffer();
	public void onOffer(long count);
	public void onTake();
	
	public boolean canTake();
	public boolean canOffer();
	public boolean canOfferWait(long timeout, TimeUnit unit);
	public boolean canOfferWait();
}