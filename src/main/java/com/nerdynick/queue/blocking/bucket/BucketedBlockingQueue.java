package com.nerdynick.queue.blocking.bucket;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.nerdynick.queue.blocking.bucket.sensor.BucketSensor;

/**
 * A {@link BlockingQueue} Implementation that queues items into buckets based on
 * a supplied {@link Function} return value.
 * 
 * @author Nikoleta Verbeck
 *
 * @param <K> Type of the Bucketing Key
 * @param <E> Type of Elements being stored in the bucketed queues
 */
public class BucketedBlockingQueue<K, E> implements BlockingQueue<E> {
	private static final Logger LOG = LoggerFactory.getLogger(BucketedBlockingQueue.class);

	private final Supplier<BlockingQueue<E>> queueSupplier;
	private final BiFunction<K, Bucket<K, E>, BucketSensor> bucketSensor;
	private final Function<E, K> keySupplier;

	private final LoadingCache<K, Bucket<K, E>> bucketsByKey;
	private final List<Bucket<K, E>> allBuckets = new LinkedList<Bucket<K,E>>();
	private final AtomicInteger tick = new AtomicInteger(Integer.MIN_VALUE);
	private final Lock lock = new ReentrantLock();

	private class BucketIterator implements Iterator<E> {
		final Iterator<Bucket<K, E>> bIter = allBuckets.iterator();
		Iterator<E> currentIter;

		public boolean hasNext() {
			if (currentIter == null || !currentIter.hasNext()) {
				if (!this.getNextIter()) {
					return false;
				}
			}
			return currentIter.hasNext();
		}

		private boolean getNextIter() {
			while (bIter.hasNext()) {
				currentIter = bIter.next().iterator();
				if (currentIter.hasNext()) {
					return true;
				}
			}
			return false;
		}

		public E next() {
			return currentIter.next();
		}

	}

	/**
	 * Default {@link Supplier} for supplying a Bounded {@link BlockingQueue} during
	 * {@link Bucket} creation.
	 * 
	 * @param <E>   Element types stored in the BlockingQueue
	 * @param limit Bounding limit for the BlockingQueue
	 * @return Supplier
	 */
	public static <E> Supplier<BlockingQueue<E>> DefaultQueue(int limit) {
		return () -> {
			return new LinkedBlockingDeque<>(limit);
		};
	}

	/**
	 * Default {@link Supplier} for suplying a Unbounded {@link BlockingQueue} during
	 * {@link Bucket} creation.
	 * 
	 * @param <E> Element types stored in the BlockingQueue
	 * @return Supplier
	 */
	public static <E> Supplier<BlockingQueue<E>> DefaultQueue() {
		return () -> {
			return new LinkedBlockingDeque<>();
		};
	}
	
	public BucketedBlockingQueue(
			final Function<E, K> keySupplier,
			final BiFunction<K, Bucket<K, E>, BucketSensor> bucketSensor, 
			final long expireTime, final TimeUnit expireUnit) {
		this(keySupplier, DefaultQueue(), bucketSensor, expireTime, expireUnit);
	}
	public BucketedBlockingQueue(
			final Function<E, K> keySupplier, 
			final int queueLimit,
			final BiFunction<K, Bucket<K, E>, BucketSensor> bucketSensor, 
			final long expireTime, final TimeUnit expireUnit) {
		this(keySupplier, DefaultQueue(queueLimit), bucketSensor, expireTime, expireUnit);
	}

	/**
	 * 
	 * @param keySupplier   {@link Function} to provide the bucketing key given the
	 *                      element being inserted
	 * @param queueSupplier {@link Supplier} to provde a new, dedicated,
	 *                      {@link BlockingQueue} for each new {@link Bucket}
	 * @param bucketSensor  {@link BiFunction} to provide a {@link BucketSensor} for
	 *                      each new {@link Bucket}
	 * @param expireTime    Time to expire and remove a bucket after it's last
	 *                      add/put/offer
	 * @param expireUnit    {@link TimeUnit} for expire time
	 */
	public BucketedBlockingQueue(
			final Function<E, K> keySupplier, 
			final Supplier<BlockingQueue<E>> queueSupplier,
			final BiFunction<K, Bucket<K, E>, BucketSensor> bucketSensor, 
			final long expireTime, final TimeUnit expireUnit) {
		this.keySupplier = keySupplier;
		this.queueSupplier = queueSupplier;
		this.bucketSensor = bucketSensor;

		this.bucketsByKey = CacheBuilder.newBuilder()
				.initialCapacity(10)
				.expireAfterAccess(expireTime, expireUnit)
				.removalListener(new RemovalListener<K, Bucket<K, E>>() {
					@Override
					public void onRemoval(RemovalNotification<K, Bucket<K,E>> notification) {
						BucketedBlockingQueue.this.allBuckets.remove(notification.getValue());
					}
				}).build(new CacheLoader<K, Bucket<K, E>>() {
					@Override
					public Bucket<K,E> load(K key) throws Exception {
						final Bucket<K, E> b = new Bucket<K, E>(
							BucketedBlockingQueue.this.queueSupplier.get(),
							BucketedBlockingQueue.this.bucketSensor, 
							key
						);
						BucketedBlockingQueue.this.allBuckets.add(b);
						return b;
					}
				});
	}

	public int size() {
		int i = 0;
		for (Bucket<K, E> b : this.allBuckets) {
			i += b.size();
		}
		return i;
	}

	public boolean isEmpty() {
		for (Bucket<K, E> b : this.allBuckets) {
			if (!b.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	public boolean contains(Object o) {
		for (Bucket<K, E> b : this.allBuckets) {
			if (b.contains(o)) {
				return true;
			}
		}
		return false;
	}

	public Iterator<E> iterator() {
		return new BucketIterator();
	}

	public Object[] toArray() {
		final ArrayList<E> els = new ArrayList<E>();

		Iterator<E> iter = this.iterator();
		while (iter.hasNext()) {
			els.add(iter.next());
		}
		return els.toArray();
	}

	public <T> T[] toArray(T[] a) {
		final ArrayList<E> els = new ArrayList<E>();

		Iterator<E> iter = this.iterator();
		while (iter.hasNext()) {
			els.add(iter.next());
		}
		return els.toArray(a);
	}
	
	public void removeBucket(K key) {
		this.bucketsByKey.invalidate(key);
	}

	public boolean remove(Object o) {
		boolean removed = false;
		for (Bucket<K, E> b : this.allBuckets) {
			if (b.remove(o)) {
				removed = true;
			}
		}
		return removed;
	}

	public boolean containsAll(Collection<?> c) {
		throw new RuntimeException("Not Implemented");
	}

	public boolean addAll(Collection<? extends E> c) {
		for (E e : c) {
			this.add(e);
		}
		return true;
	}

	public boolean removeAll(Collection<?> c) {
		boolean removed = false;
		for(Bucket<K,E> b: this.allBuckets) {
			if(b.removeAll(c)) {
				removed = true;
			}
		}
		return removed;
	}

	public boolean retainAll(Collection<?> c) {
		boolean removed = false;
		for(Bucket<K,E> b: this.allBuckets) {
			if(b.retainAll(c)) {
				removed = true;
			}
		}
		return removed;
	}

	public void clear() {
		for (Bucket<K, E> b : this.allBuckets) {
			b.clear();
		}
	}

	/**
	 * Ticks the counter and return the bucket to work on next.
	 * 
	 * @return
	 */
	private int tick() {
		int t = Math.abs(tick.incrementAndGet());
		return (t % this.allBuckets.size());
	}

	/**
	 * Get the next bucket, if on is available. 
	 * Otherwise return null when no bucket can be found.
	 * 
	 * @return Next available bucket or null
	 */
	protected Bucket<K, E> getNextBucket() {
		try {
			return this.getNextBucket(-1);
		} catch (InterruptedException e) {
			return null;
		}
	}

	/**
	 * Get the next bucket. 
	 * Waiting up to `wait` to find an available one.
	 * 
	 * @param wait How long to wait for a bucket to be available. 
	 * 0 will wait tell available or interrupted. 
	 * -1 will return right away.
	 * Anything else is will be the wait time in MS.
	 * @return Next available bucket or null
	 * @throws InterruptedException
	 */
	private Bucket<K, E> getNextBucket(long wait) throws InterruptedException {
		if (this.allBuckets.isEmpty()) {
			return null;
		}

		int iterations = 0;
		Bucket<K, E> b = null;
		do {
			final int t = this.tick();
			b = this.allBuckets.get(t);

			if (b == null || !b.canTake() || !b.isEmpty()) {
				b = null;
				iterations++;
	
				if (iterations >= this.allBuckets.size()) {
					iterations = 0;
					if (wait == 0) {
						this.allBuckets.wait();
					} else if (wait < 0) {
						LOG.trace("Exhausted all buckets. None are available or have elements.");
						break;
					} else {
						this.allBuckets.wait(wait);
					}
				}
			}
		} while (b == null);
		
		return b;
	}

	public void put(E e) throws InterruptedException {
		final K key = this.keySupplier.apply(e);
		try {
			this.bucketsByKey.get(key).put(e);
			this.allBuckets.notify();
		} catch (ExecutionException e1) {
			throw new IllegalStateException("Failed to add Element to Bucket Queue", e1);
		}
	}

	public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
		final K key = this.keySupplier.apply(e);
		try {
			boolean success = this.bucketsByKey.get(key).offer(e, timeout, unit);
			this.allBuckets.notify();
			return success;
		} catch (ExecutionException e1) {
			throw new IllegalStateException("Failed to add Element to Bucket Queue", e1);
		}
	}

	public boolean add(E e) {
		final K key = this.keySupplier.apply(e);
		try {
			boolean success = this.bucketsByKey.get(key).add(e);
			this.allBuckets.notify();
			return success;
		} catch (ExecutionException e1) {
			throw new IllegalStateException("Failed to add Element to Bucket Queue", e1);
		}
	}

	public boolean offer(E e) {
		final K key = this.keySupplier.apply(e);
		try {
			boolean success = this.bucketsByKey.get(key).offer(e);
			this.allBuckets.notify();
			return success;
		} catch (ExecutionException e1) {
			throw new IllegalStateException("Failed to add Element to Bucket Queue", e1);
		}
	}

	public E take() throws InterruptedException {
		this.lock.lockInterruptibly();
		try {
			final Bucket<K, E> b = this.getNextBucket(0);
			if (b != null) {
				return b.poll();
			}
			return null;
		} finally {
			this.lock.unlock();
		}
	}

	public E poll() {
		if (!this.lock.tryLock()) {
			return null;
		}
		try {
			final Bucket<K, E> b = this.getNextBucket();
			if (b != null) {
				return b.poll();
			}
			return null;
		} finally {
			this.lock.unlock();
		}
	}

	public E poll(long timeout, TimeUnit unit) throws InterruptedException {
		if (!this.lock.tryLock(timeout, unit)) {
			return null;
		}
		try {
			final Bucket<K, E> b = this.getNextBucket(unit.toMillis(timeout));
			if (b != null) {
				return b.poll();
			}
			return null;
		} finally {
			this.lock.unlock();
		}
	}

	public E remove() {
		this.lock.lock();
		try {
			final Bucket<K, E> b = this.getNextBucket();
			if (b != null) {
				return b.remove();
			}
			throw new NoSuchElementException("No buckets to remove from");
		} finally {
			this.lock.unlock();
		}
	}

	public E element() {
		this.lock.lock();
		try {
			final Bucket<K, E> b = this.getNextBucket();
			if (b != null) {
				return b.element();
			}
			throw new NoSuchElementException("No buckets to remove from");
		} finally {
			this.lock.unlock();
		}
	}

	public E peek() {
		this.lock.lock();
		try {
			final Bucket<K, E> b = this.getNextBucket();
			if (b != null) {
				return b.peek();
			}
			return null;
		} finally {
			this.lock.unlock();
		}
	}

	public int remainingCapacity() {
		return Integer.MAX_VALUE;
	}

	public int drainTo(Collection<? super E> c) {
		throw new RuntimeException("Not implemented");
	}

	public int drainTo(Collection<? super E> c, int maxElements) {
		throw new RuntimeException("Not implemented");
	}

}
