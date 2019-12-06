package com.nerdynick.gauges;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class TimeTicker {
	private final AtomicLong lastTick;
	private final long tickInterval;
	private final TimeUnit unit;
	private final Clock clock;
	
	public TimeTicker(long tickInterval, TimeUnit unit, Clock clock) {
		this.unit = unit;
		this.tickInterval = unit.toNanos(tickInterval);
		this.clock = clock;
		this.lastTick = new AtomicLong(clock.get());
	}
	
	public boolean tickIfNeeded() {
		long now = clock.get();
		long lTick = lastTick.get();
		long diff = now-lTick;
		if(diff >= tickInterval) {
			if(lastTick.compareAndSet(lTick, now)) {
				return true;
			}
		}
		return false;
	}
	
	public long getLapsTime() {
		long diff = clock.get()-lastTick.get();
		return unit.convert(diff, TimeUnit.NANOSECONDS);
	}
}
