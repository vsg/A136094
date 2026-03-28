/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094.solver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Supplier;

import oeis.a136094.Bundle;
import oeis.a136094.Main;
import oeis.a136094.key.Key;
import oeis.a136094.partials.Partials;
import oeis.a136094.util.Utils;

public class DFSBatchSolver extends Solver {

    protected static final Comparator<Batch> BATCH_PREFIX_COMPARATOR = Comparator
            .comparing(batch -> Optional.ofNullable(batch.minPrefix()).orElse(""));
    
    protected static class Progress {

        private final Bundle[] bundles0;
        private final int bestAnsLen;
        private final long begin = System.currentTimeMillis();
        private long lastProgressTime = begin;
        private long numStatesDone;

        public Progress(Bundle[] bundles0, int bestAnsLen) {
            this.bundles0 = bundles0;
            this.bestAnsLen = bestAnsLen;
        }

        public void onStateDone() {
            numStatesDone++;
        }

        public void printProgress(State state, long totalCached, int processQueueSize, int orderingQueueSize) {
            if (Main.PRINT_PROGRESS) {
                long now = System.currentTimeMillis();
                long totalTime = now - begin;
                long plusTime = now - lastProgressTime;
                System.out.println(Utils.formatLog("%s %d %d, %d moves, %d cache, %s [%d ms, +%d ms] {%d, %d}", 
                        Bundle.bundlesToString(bundles0), bestAnsLen, state.prefix.length(), numStatesDone, totalCached, 
                        state.prefix, totalTime, plusTime, processQueueSize, orderingQueueSize));
                lastProgressTime = now;
            }
        }

    }
    
    public DFSBatchSolver(Partials partials) {
        super(partials);
    }

    @Override
    public String solve(Bundle[] bundles0, int bestAnsLen) {
        Progress progress = new Progress(bundles0, bestAnsLen);
        
        PriorityBlockingQueue<Batch> processQueue = new PriorityBlockingQueue<>(100, BATCH_PREFIX_COMPARATOR);
        PriorityQueue<Batch> orderingQueue = new PriorityQueue<>(BATCH_PREFIX_COMPARATOR);

        BatchStateProcessor batchProcessor = new BatchStateProcessor(partials, processQueue);
        new Thread(batchProcessor).start();

        Bundle[] sortedBundles0 = Bundle.sortBundles(bundles0);
        State state0 = new State("", sortedBundles0, null);
        Batch batch0 = new Batch(bestAnsLen, Arrays.asList(state0));
        
        processQueue.add(batch0);
        orderingQueue.add(batch0);
        
        SeenCache seenCache = new SeenCache();
        Supplier<Long> maxCacheSize = () -> Main.DFS_BATCH_MAX_CACHE;
        
        String answer = solveDFSBatch(processQueue, orderingQueue, bestAnsLen, seenCache, maxCacheSize, progress);
        
        batchProcessor.shutdown();
        
        return answer;
    }

    protected String solveDFSBatch(Queue<Batch> processQueue, Queue<Batch> orderingQueue, int bestAnsLen, 
            SeenCache seenCache, Supplier<Long> maxCacheSize, Progress progress) {
        long numStatesSinceCleanup = progress.numStatesDone;
        
        Batch batch;
        while ((batch = orderingQueue.poll()) != null) {
            if (batch.bestAnsLen != bestAnsLen) throw new RuntimeException();
            
            batch.ensureProcessed();
            
            List<State> nextBatchStates = new ArrayList<State>();
            
            for (State state : batch.states) {
                String prefix = state.prefix;
                Bundle[] sortedBundles = state.sortedBundles;
                
                if (sortedBundles.length == 0) return prefix;
                
                for (State nextState : state.nextStates) {
                    String nextPrefix = nextState.prefix;
                    Key nextKey = nextState.key;
                    
                    int nextLevel = nextPrefix.length();
                    if (!seenCache.add(nextLevel, nextKey)) continue;
                    
                    nextBatchStates.add(nextState);
                    
                    if (nextBatchStates.size() == Main.DFS_BATCH_SIZE) {
                        Batch nextBatch = new Batch(bestAnsLen, nextBatchStates);
                        processQueue.add(nextBatch);
                        orderingQueue.add(nextBatch);
                        nextBatchStates = new ArrayList<State>();
                    }
                }
                
                state.nextStates.clear();
                
                progress.onStateDone();
                
                numStatesSinceCleanup++;
                if (numStatesSinceCleanup >= 1000000) {
                    long totalCached = seenCache.cleanup(maxCacheSize.get());
                    progress.printProgress(state, totalCached, processQueue.size(), orderingQueue.size());
                    numStatesSinceCleanup = 0;
                }
            }
            
            if (!nextBatchStates.isEmpty()) {
                Batch nextBatch = new Batch(bestAnsLen, nextBatchStates);
                processQueue.add(nextBatch);
                orderingQueue.add(nextBatch);
            }
        }
        
        return null;
    }

}