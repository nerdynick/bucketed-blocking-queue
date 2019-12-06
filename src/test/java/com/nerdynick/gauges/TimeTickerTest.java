package com.nerdynick.gauges;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.nerdynick.gauges.Clock.TestClock;

public class TimeTickerTest {

	@Test
	public void testTick() {
		final TestClock clock = new TestClock(System.nanoTime());
		final TimeTicker ticker = new TimeTicker(1, TimeUnit.SECONDS, clock);
		
		assertFalse("Ticker ticket when it shouldn't have", ticker.tickIfNeeded());
		
		clock.set(clock.get()+TimeUnit.SECONDS.toNanos(1));
		assertTrue("Ticker didn't tick when it should have", ticker.tickIfNeeded());
		
		clock.set(clock.get()+TimeUnit.SECONDS.toNanos(1)+1);
		assertTrue("Ticker didn't tick when it should have", ticker.tickIfNeeded());
	}

}
