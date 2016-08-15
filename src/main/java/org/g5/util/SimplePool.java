package org.f8s.g5.util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Optional;

/**
 * I'm a simple object pool that uses a backing {@link ArrayBlockingQueue} to store pooled items.
 * 
 * My configuration options are pretty basic - I support:<ul>
 * <li>An initial size - these many items will be pre-created.</li>
 * <li>A maximum size - this is the upper limit on the number of pooled items I'll store.</li>
 * <li>A wait timeout - I'll wait these many seconds for an item to become available.
 * You may specify '0' to indicate you don't wish to wait at all. A negative value indicates wait indefinitely.</li>
 * </ul>
 *<p/>
 * You must set me up with a {@link Callable callable} that knows how to create objects to be stored here.
 * You may also, optionally, set me up with a cleanup callback that knows how to cleanup objects stored here.
 * This is required if you want to clear the pool. Otherwise, resources may be left hanging around after clearing the pool.
 * <p/>
 * Source code licensed under the GNU GPL v3.0 or later.
 * 
 * @author gerard.fernandes@gmail.com
 *
 * @param <T>
 */
public class SimplePool<T> implements SimplePoolMBean {

    private static final int WAIT_INDEFINITELY = -1;
    private final ArrayBlockingQueue<T> pool;
    private final int maximumSize;
    private final int waitTimeoutInSeconds;
    private final Callable<T> instanceBuilder;
    private final Optional<InstanceCleaner<T>> instanceCleaner;
    private final AtomicInteger leasedCount = new AtomicInteger(0);
    
    public static <T> SimplePool<T> defaultEmptyPool(int maxSize, Callable<T> instanceBuilder) {
        return new SimplePool<>(0, maxSize, instanceBuilder, null);
    }
    
    public static <T> SimplePool<T> defaultEmptyPool(int maxSize, Callable<T> instanceBuilder, InstanceCleaner<T> instanceCleaner) {
        return new SimplePool<>(0, maxSize, instanceBuilder, instanceCleaner);
    }
    
    public static <T> SimplePool<T> defaultEmptyNoWaitPool(int maxSize, Callable<T> instanceBuilder) {
        return new SimplePool<>(0, maxSize, 0, instanceBuilder, null);
    }
    
    public static <T> SimplePool<T> defaultEmptyNoWaitPool(int maxSize, Callable<T> instanceBuilder, InstanceCleaner<T> instanceCleaner) {
        return new SimplePool<>(0, maxSize, 0, instanceBuilder, instanceCleaner);
    }
    
    public static <T> SimplePool<T> minimumSizedDefaultPool(int initialSize, int maxSize, Callable<T> instanceBuilder) {
        return new SimplePool<>(initialSize, maxSize, instanceBuilder, null);
    }
    
    public static <T> SimplePool<T> minimumSizedDefaultPool(int initialSize, int maxSize, Callable<T> instanceBuilder, InstanceCleaner<T> instanceCleaner) {
        return new SimplePool<>(initialSize, maxSize, instanceBuilder, instanceCleaner);
    }
    
    public static <T> SimplePool<T> minimumSizedNoWaitPool(int initialSize, int maxSize, Callable<T> instanceBuilder) {
        return new SimplePool<>(initialSize, maxSize, 0, instanceBuilder, null);
    }
    
    public static <T> SimplePool<T> minimumSizedNoWaitPool(int initialSize, int maxSize, Callable<T> instanceBuilder, InstanceCleaner<T> instanceCleaner) {
        return new SimplePool<>(initialSize, maxSize, 0, instanceBuilder, instanceCleaner);
    }
    
    public static <T> SimplePool<T> minimumSizedTimeoutPool(int initialSize, int maxSize, int timeoutInSeconds, Callable<T> instanceBuilder) {
        return new SimplePool<>(initialSize, maxSize, timeoutInSeconds, instanceBuilder, null);
    }
    
    public static <T> SimplePool<T> minimumSizedTimeoutPool(int initialSize, int maxSize, int timeoutInSeconds, Callable<T> instanceBuilder, InstanceCleaner<T> instanceCleaner) {
        return new SimplePool<>(initialSize, maxSize, timeoutInSeconds, instanceBuilder, instanceCleaner);
    }
    
    private SimplePool(int initialSize, int maximumSize, Callable<T> instanceBuilder, InstanceCleaner<T> instanceCleaner) {
        this(initialSize, maximumSize, WAIT_INDEFINITELY, instanceBuilder, instanceCleaner);
    }
    
    private SimplePool(int initialSize, int maximumSize, int waitTimeoutInSeconds, Callable<T> instanceBuilder, InstanceCleaner<T> instanceCleaner) {
        this.maximumSize = maximumSize;
        this.pool = new ArrayBlockingQueue<>(this.maximumSize);
        this.waitTimeoutInSeconds = waitTimeoutInSeconds;
        this.instanceBuilder = instanceBuilder;
        this.instanceCleaner = Optional.fromNullable(instanceCleaner);
        try {
            for (int i = 0; i< initialSize; i++) {
                this.pool.add(this.instanceBuilder.call());
            }
        } catch (Exception e) {
             throw new IllegalStateException("Failed to initialise pool!", e);
        }
    }
    
    //retrieval needs to be synchronized as we don't want to allow multiple clients in here at once.
    public synchronized T get() throws InterruptedException {
        T value = this.pool.poll();
        if (value == null && this.leasedCount.get() < this.maximumSize) {
            try {
                this.pool.add(this.instanceBuilder.call());
                value = this.pool.poll();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create a new instance - current pool size="+this.pool.size());
            }
        } else if (this.leasedCount.get() == this.maximumSize) {
            if (this.waitTimeoutInSeconds < 0) {//wait for the next available instance, however long it takes...
                value = this.pool.take();
            } else if (this.waitTimeoutInSeconds > 0) {//wait for the specified, non-zero timeout value.
                value = this.pool.poll(this.waitTimeoutInSeconds, TimeUnit.SECONDS);
            }
        }
        if (value != null) {//if we have something, increment the leased counter...
            //this value can be null IFF there are no available instances AND the wait-timeout is 0 (i.e. - don't wait)
            leasedCount.incrementAndGet();
        }
        return value;
    }

    public void yield(T t) {
        if (leasedCount.get() > this.maximumSize) {
            //hmm... how did this happen?
            //we probably want to throw this instance away at this point...
        } else {
            this.pool.add(t);
            leasedCount.decrementAndGet();
        }
    }
    
    @Override
    public synchronized void clear() {
        for (T t: this.pool) {
            if (this.instanceCleaner.isPresent()) {
                this.instanceCleaner.get().cleanup(t);
            }
        }
        this.pool.clear();
    }
    
    public int size() {
        return this.pool.size();
    }
    
    public int maximumSize() {
        return this.maximumSize;
    }
    
    public int leasedCount() {
        return leasedCount.get();
    }
    
    @Override
    public int getPoolSize() {
        return size();
    }

    @Override
    public int getMaxPoolSize() {
        return maximumSize();
    }

    @Override
    public int getLeasedInstanceCount() {
        return leasedCount();
    }

    @Override
    public String getAvailableServiceConnections() {
        return this.pool.toString();
    }
    
    public static interface InstanceCleaner<T> {
        void cleanup(T t);
    }
    
}
