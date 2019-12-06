package com.nerdynick.gauges;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.nerdynick.gauges.Clock.TestClock;

public class RateGaugeTest {
	@Test
	public void testBasics() {
		final TestClock clock = new TestClock(System.nanoTime());
		final RateGauge gauge = new RateGauge(1, TimeUnit.SECONDS, clock);
		
		assertEquals(0, gauge.currentRate());
		
		gauge.incr();
		assertEquals(1, gauge.currentRate());
		
		clock.add(1);
		assertEquals(1, gauge.currentRate());
		
		clock.add(TimeUnit.SECONDS.toNanos(1));
		assertEquals(0, gauge.currentRate());
	}
}
