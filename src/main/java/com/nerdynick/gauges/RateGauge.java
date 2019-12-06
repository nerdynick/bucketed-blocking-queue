package com.nerdynick.gauges;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public class RateGauge {
	private final LongAdder adder = new LongAdder();
	private final TimeTicker ticker;
	
	public RateGauge() {
		this(Clock.defaultClock());
	}
	
	public RateGauge(Clock clock) {
		this(1, TimeUnit.SECONDS, clock);
	}
	
	public RateGauge(long tickInterval, TimeUnit unit) {
		this(tickInterval, unit, Clock.defaultClock());
	}
	
	public RateGauge(long tickInterval, TimeUnit unit, Clock clock) {
		this.ticker = new TimeTicker(tickInterval, unit, clock);
	}
	
	private void tickIfNeeded() {
		if(ticker.tickIfNeeded()) {
			adder.reset();
		}
	}
	
	public void incr() {
		tickIfNeeded();
		adder.increment();
	}
	public void add(long count) {
		tickIfNeeded();
		adder.add(count);
	}
	
	public long currentRate() {
		tickIfNeeded();
		return adder.sum();
	}
	
}