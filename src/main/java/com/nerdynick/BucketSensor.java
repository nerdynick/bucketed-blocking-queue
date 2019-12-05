package com.nerdynick;

public interface BucketSensor {
	default void onOffer() {}
	default void onTake() {}
	default boolean isReady() { return true;}
}