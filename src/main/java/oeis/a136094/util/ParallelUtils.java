/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ParallelUtils {

    private static final ExecutorService THREAD_POOL = Executors.newCachedThreadPool((runnable) -> {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        return thread;
    });
    
    public static <T> void processInParallel(Collection<T> input, int numThreads, Supplier<Consumer<T>> workerFactory) {
        processInParallel(input, numThreads, workerFactory, null, null);
    }
    
    public static <T> void processInParallel(Collection<T> input, int numThreads, Supplier<Consumer<T>> workerFactory,
            Runnable workerDoneCallback, Consumer<T> resultCallback) {
        int numItems = input.size();
        if (numItems == 0) return;
        if (numThreads == 1) {
            Consumer<T> worker = workerFactory.get();
            for (T item : input) {
                worker.accept(item);
                if (resultCallback != null) {
                    resultCallback.accept(item);
                }
            }
            if (workerDoneCallback != null) {
                workerDoneCallback.run();
            }
        } else {
            // Input from array+AtomicInteger is about 25% faster than from ArrayBlockingQueue
            @SuppressWarnings("unchecked")
            T[] inputArray = (T[]) input.toArray();
            AtomicInteger nextIndex = new AtomicInteger();
            BlockingQueue<T> resultQueue = (resultCallback != null) ? new ArrayBlockingQueue<>(numItems) : null;
            CountDownLatch latch = new CountDownLatch(numThreads);
            for (int i = 0; i < numThreads; i++) {
                THREAD_POOL.submit(() -> {
                    try {
                        Consumer<T> worker = workerFactory.get();
                        int index;
                        while ((index = nextIndex.getAndIncrement()) < numItems) {
                            T item = inputArray[index];
                            worker.accept(item);
                            if (resultQueue != null) {
                                resultQueue.put(item);
                            }
                        }
                        if (workerDoneCallback != null) {
                            workerDoneCallback.run();
                        }
                        latch.countDown();
                    } catch (Throwable t) {
                        t.printStackTrace();
                        System.exit(-1);
                    }
                });
            }
            try {
                if (resultCallback != null) {
                    for (int index = 0; index < numItems; index++) {
                        T item = resultQueue.take();
                        resultCallback.accept(item);
                    }
                }
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException();
            }
        }
    }

    public static <T> void processInBatches(Collection<T> input, int batchSize, Consumer<ArrayList<T>> batchCallback) {
        processInBatches((consumer) -> {
            for (T item : input) {
                consumer.accept(item);
            }
        }, batchSize, batchCallback);
    }
    
    public static <T> void processInBatches(Generator<T> input, int batchSize, Consumer<ArrayList<T>> batchCallback) {
        ArrayList<T> batch = new ArrayList<>();
        input.generate((item) -> {
            batch.add(item);
            if (batch.size() == batchSize) {
                batchCallback.accept(batch);
                batch.clear();
            }
        });
        if (!batch.isEmpty()) {
            batchCallback.accept(batch);
        }
    }

}
