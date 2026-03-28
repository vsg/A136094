/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094.solver;

import static oeis.a136094.util.ParallelUtils.processInParallel;

import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import oeis.a136094.Main;
import oeis.a136094.partials.Partials;

public class BatchStateProcessor implements Runnable {

    private static final Batch TERMINATOR = new Batch(-1, Collections.emptyList());
    
    private final Partials partials;
    private final BlockingQueue<Batch> queue;
    private final AtomicInteger numThreads;

    public BatchStateProcessor(Partials partials, BlockingQueue<Batch> queue) {
        this(partials, queue, new AtomicInteger(Main.NUM_WORKER_THREADS));
    }
    
    public BatchStateProcessor(Partials partials, BlockingQueue<Batch> queue, AtomicInteger numThreads) {
        this.partials = partials;
        this.queue = queue;
        this.numThreads = numThreads;
    }
    
    @Override
    public void run() {
        try {
            Batch batch;
            while ((batch = queue.take()) != TERMINATOR) {
                processBatch(batch, numThreads.get());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private void processBatch(Batch batch, int numThreads) {
        processInParallel(batch.states, numThreads, () -> {
            StateProcessor processor = new StateProcessor(partials);
            SeenCache seenCache = new SeenCache();
            return (state) -> {
                processor.process(state, batch.bestAnsLen, seenCache);
            };
        });
        batch.processed = true;
    }
    
    public void shutdown() {
        queue.add(TERMINATOR);
    }
    
}