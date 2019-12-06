package com.nerdynick;

public interface BucketSensor {
	public void onOffer();
	public void onTake();
	public boolean canTake();
	public boolean canOffer();
}