package com.nerdynick.gauges;

public abstract class Clock {
	public abstract long get();
	
	private static final Clock DEFAULT = new SystemClock();
	
	public static Clock defaultClock() {
		return DEFAULT;
	}
	
	public static class SystemClock extends Clock{
		@Override
		public long get() {
			return System.nanoTime();
		}
	}
	
	public static class TestClock extends Clock {
		private long time = 0;
		
		public TestClock(long startTime) {
			time = startTime;
		}
		
		@Override
		public long get() {
			return time;
		}
		
		public void set(long time) {
			this.time = time;
		}
		
		public void add(long time) {
			this.time += time;
		}
	}
}
