package org.g5.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.g5.util.SimplePool.minimumSizedDefaultPool;
import static org.g5.util.SimplePool.minimumSizedTimeoutPool;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class SimplePoolTest {

    @Test
    void shouldCreateAndReturnObjectsCorrectly() throws Exception {
        SimplePool<String> testStringPool = minimumSizedDefaultPool(2, 3, new Callable<String>() {
            @Override
            public String call() throws Exception {
                return "Value";
            }
        });
        
        assertThat(testStringPool.size()).isEqualTo(2);
        assertThat(testStringPool.leasedCount()).isEqualTo(0);
        
        String data1 = testStringPool.get();
        String data2 = testStringPool.get();
        
        assertThat(testStringPool.size()).isEqualTo(0);
        assertThat(testStringPool.leasedCount()).isEqualTo(2);
        
        String data3 = testStringPool.get();
        assertThat(testStringPool.size()).isEqualTo(0);
        assertThat(testStringPool.leasedCount()).isEqualTo(3);
        
        testStringPool.yield(data1);
        testStringPool.yield(data2);
        testStringPool.yield(data3);
        
        assertThat(testStringPool.size()).isEqualTo(3);
        assertThat(testStringPool.leasedCount()).isEqualTo(0);

    }
    
    @Test
    void shouldHandleMultithreadedAccess() throws Exception {
        List<String> collectedItems = new ArrayList<>();
        assertThat(collectedItems.size()).isEqualTo(0);
        SimplePool<String> testStringPool = minimumSizedDefaultPool(2, 5, new Callable<String>() {
            @Override
            public String call() throws Exception {
                return "Value";
            }
        });
        ThreadPoolExecutor executorSvc = (ThreadPoolExecutor) Executors.newFixedThreadPool(10, new ThreadFactory(){
            AtomicInteger counter = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "SimplePoolUser-"+counter.incrementAndGet());
            }});
        for (int i = 0; i < 6; i++) {
            executorSvc.submit(new PoolUser(testStringPool, collectedItems));
        }
        Thread.sleep(150);//give the executor worker threads a chance to kick-off...
        assertThat(testStringPool.size()).isEqualTo(0);
        assertThat(testStringPool.leasedCount()).isEqualTo(5);
        assertThat(collectedItems.size()).isEqualTo(5);
        
        assertThat(executorSvc.getCompletedTaskCount()).isEqualTo(5L);
        assertThat(executorSvc.getActiveCount()).isEqualTo(1);
        
        testStringPool.yield(collectedItems.remove(0));
        Thread.sleep(50);//give the executor worker thread a chance to continue...
        testStringPool.yield(collectedItems.remove(0));
        
        assertThat(testStringPool.size()).isEqualTo(1);
        assertThat(testStringPool.leasedCount()).isEqualTo(4);
        assertThat(collectedItems.size()).isEqualTo(4);
        
        assertThat(executorSvc.getCompletedTaskCount()).isEqualTo(6L);
        assertThat(executorSvc.getActiveCount()).isEqualTo(0);
        
        executorSvc.shutdown();
        
        while(! collectedItems.isEmpty()) {
            testStringPool.yield(collectedItems.remove(0));
        }
        
        assertThat(testStringPool.size()).isEqualTo(5);
        assertThat(testStringPool.leasedCount()).isEqualTo(0);
        assertThat(collectedItems.size()).isEqualTo(0);
        
    }
    
    @Test
    @Disabled
    public void shouldHandleMultithreadedAccessWithTimeouts() throws Exception {
        List<String> collectedItems = new ArrayList<>();
        SimplePool<String> testStringPool = minimumSizedTimeoutPool(1, 1, 1, new Callable<String>() {
            @Override
            public String call() throws Exception {
                return "Value";
            }
        });
        collectedItems.add(testStringPool.get());
        Thread worker = new Thread(new PoolUser(testStringPool, collectedItems));

        assertThat(testStringPool.size()).isEqualTo(0);
        assertThat(testStringPool.leasedCount()).isEqualTo(1);
        assertThat(collectedItems.size()).isEqualTo(1);
        
        worker.run();
        Thread.sleep(50);//give the worker thread a chance to kick-off...
        
        testStringPool.yield(collectedItems.remove(0));
        
        assertThat(testStringPool.size()).isEqualTo(1);//we've returned 1 instance to the pool...
        assertThat(testStringPool.leasedCount()).isEqualTo(0);//0 instances are still leased out...
        assertThat(collectedItems.size()).isEqualTo(0);//collected items should be the same as leased
        
        while(! collectedItems.isEmpty()) {
            testStringPool.yield(collectedItems.remove(0));
        }
        
        assertThat(testStringPool.size()).isEqualTo(1);
        assertThat(testStringPool.leasedCount()).isEqualTo(0);
        assertThat(collectedItems.size()).isEqualTo(0);
        
    }
    
    private static class PoolUser implements Runnable {
        private final SimplePool<String> pool;
        private final List<String> collectedItems; 
        
        PoolUser(SimplePool<String> pool, List<String> collectedItems) {
            this.pool = pool;
            this.collectedItems = collectedItems;
        }
        
        @Override
        public void run() {
            try {
                collectedItems.add(pool.get());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            
        }
    }
}
