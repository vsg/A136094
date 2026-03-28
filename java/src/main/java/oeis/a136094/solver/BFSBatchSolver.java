/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094.solver;

import static oeis.a136094.util.ParallelUtils.processInBatches;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import oeis.a136094.Bundle;
import oeis.a136094.Main;
import oeis.a136094.key.Key;
import oeis.a136094.partials.Partials;
import oeis.a136094.util.MemoryEfficientHashSet;
import oeis.a136094.util.Utils;

public class BFSBatchSolver extends Solver {

    private static class Progress {

        private final Bundle[] bundles0;
        private final int bestAnsLen;
        private final long begin = System.currentTimeMillis();
        private long lastProgressTime = begin;

        public Progress(Bundle[] bundles0, int bestAnsLen) {
            this.bundles0 = bundles0;
            this.bestAnsLen = bestAnsLen;
        }

        public void printProgress(int level, int numStates, State someState) {
            if (Main.PRINT_PROGRESS) {
                long now = System.currentTimeMillis();
                long totalTime = now - begin;
                long plusTime = now - lastProgressTime;
                System.out.println(Utils.formatLog("%s %d %d %d [%d ms, +%d ms]; %s", 
                        Bundle.bundlesToString(bundles0), bestAnsLen, level, numStates, totalTime, plusTime, 
                        Bundle.bundlesToString(someState.sortedBundles)));
                lastProgressTime = now;
            }
        }
        
    }
    
    public BFSBatchSolver(Partials partials) {
        super(partials);
    }

    @Override
    public String solve(Bundle[] bundles0, int bestAnsLen) {
        Progress progress = new Progress(bundles0, bestAnsLen);
        
        BlockingQueue<Batch> processQueue = new LinkedBlockingQueue<>();
        
        BatchStateProcessor batchProcessor = new BatchStateProcessor(partials, processQueue);
        new Thread(batchProcessor).start();
        
        Bundle[] sortedBundles0 = Bundle.sortBundles(bundles0);
        State state0 = new State("", sortedBundles0, null);
        
        String answer = solveBFS(state0, bestAnsLen, processQueue, progress);
        
        batchProcessor.shutdown();
        
        return answer;
    }

    private String solveBFS(State state0, int bestAnsLen, Queue<Batch> processQueue, Progress progress) {
        List<State> states = Arrays.asList(state0);
        for (int level = 0; level <= bestAnsLen; level++) {
            if (states.isEmpty()) break;
            progress.printProgress(level, states.size(), states.get(states.size()/2));

            List<Batch> batches = makeBatches(bestAnsLen, states);
            
            processQueue.addAll(batches);
            
            Set<Key> seen = new MemoryEfficientHashSet<>();
            
            List<State> nextStates = new ArrayList<>();
            for (Batch batch : batches) {
                batch.ensureProcessed();
                
                for (State state : batch.states) {
                    for (State nextState : state.nextStates) {
                        String prefix = nextState.prefix;
                        Bundle[] sortedBundles = nextState.sortedBundles;
                        Key key = nextState.key;
                        
                        if (sortedBundles.length == 0) {
                            return prefix;
                        }
                        
                        if (!seen.add(key)) continue;
                        
                        nextStates.add(nextState);
                    }
                }
            }
            states = nextStates;
        }
        return null;
    }

    private List<Batch> makeBatches(int bestAnsLen, List<State> states) {
        List<Batch> batches = new ArrayList<>();
        processInBatches((consumer) -> {
            for (State state : states) {
                consumer.accept(state);
            }
        }, 10000, (ArrayList<State> batchStates) -> {
            batches.add(new Batch(bestAnsLen, batchStates));
        });
        return batches;
    }

}