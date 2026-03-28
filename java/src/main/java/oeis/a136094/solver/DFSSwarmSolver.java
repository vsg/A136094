/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094.solver;

import static oeis.a136094.util.ParallelUtils.processInParallel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import oeis.a136094.Bundle;
import oeis.a136094.Main;
import oeis.a136094.Problem;
import oeis.a136094.partials.Partials;

public class DFSSwarmSolver extends DFSBatchSolver {

    private volatile boolean batchMode;
    private final Semaphore semaphore = new Semaphore(Main.DFS_SWARM_MAX_GROUPS);
    private final AtomicInteger numSolvers = new AtomicInteger();
    private final AtomicInteger numThreadsPerGroup = new AtomicInteger();
    
    public DFSSwarmSolver(Partials partials) {
        super(partials);
    }

    @Override
    public void solve(Problem problem) {
        solveProblems(Arrays.asList(problem), (_) -> {});
    }

    public void solveProblems(List<Problem> problems, Consumer<Problem> resultConsumer) {
        batchMode = Main.DFS_SWARM_BATCH_MODE;
        numSolvers.set(Main.NUM_WORKER_THREADS);
        numThreadsPerGroup.set(1 + Main.NUM_WORKER_THREADS / Main.DFS_SWARM_MAX_GROUPS);

        Runnable onWorkerDone = () -> {
            // Switch to batch mode
            batchMode = true;
            
            // Reallocate resources
            int numRemainingSolvers = numSolvers.decrementAndGet();
            if (numRemainingSolvers > 0) {
                int numGroups = Math.min(numRemainingSolvers, Main.DFS_SWARM_MAX_GROUPS);
                numThreadsPerGroup.set(1 + Main.NUM_WORKER_THREADS / numGroups);
            }
        };
        
        processInParallel(problems, Main.NUM_WORKER_THREADS, () -> {
            return (problem) -> {
                DFSSwarmSolver.super.solve(problem);
            };
        }, onWorkerDone, resultConsumer);
    }
    
    @Override
    public String solve(Bundle[] bundles0, int bestAnsLen) {
        Progress progress = new Progress(bundles0, bestAnsLen);
        ArrayList<ArrayDeque<State>> levelTodo = new ArrayList<>();
        SeenCache seenCache = new SeenCache();
        
        Bundle[] sortedBundles0 = Bundle.sortBundles(bundles0);
        State state0 = new State("", sortedBundles0, null);
        levelTodo.add(new ArrayDeque<>(Arrays.asList(state0)));

        // start in DFS mode, then switch to DFSBatch mode
        return solveDFS(bestAnsLen, levelTodo, seenCache, progress);
    }

    private String solveDFS(int bestAnsLen, ArrayList<ArrayDeque<State>> levelTodo, SeenCache seenCache,
            Progress progress) {
        long numStatesSinceModeCheck = 0;
        long numStatesSinceCleanup = 0;
        
        StateProcessor processor = new StateProcessor(partials);
        
        int level = 0;
        while (level >= 0) {
            State state = levelTodo.get(level).pollFirst();
            if (state == null) {
                level--;
                continue;
            }
            
            String prefix = state.prefix;
            Bundle[] sortedBundles = state.sortedBundles;
            
            if (sortedBundles.length == 0) return prefix;

            processor.process(state, bestAnsLen, seenCache);
            
            while (levelTodo.size() <= level+1) {
                levelTodo.add(new ArrayDeque<State>());
            }
            levelTodo.get(level+1).addAll(state.nextStates);

            progress.onStateDone();
            
            numStatesSinceModeCheck++;
            if (numStatesSinceModeCheck >= 10000) {
                if (batchMode) {
                    return solveDFSBatch(bestAnsLen, levelTodo, seenCache, progress);
                }
                numStatesSinceModeCheck = 0;
            }

            numStatesSinceCleanup++;
            if (numStatesSinceCleanup >= 10000000) {
                long maxCacheSize = Main.DFS_BATCH_MAX_CACHE / numSolvers.get();
                long totalCached = seenCache.cleanup(maxCacheSize);
                progress.printProgress(state, totalCached, -1, -1);
                numStatesSinceCleanup = 0;
            }
            
            level++;
        }
        return null;
    }

    private String solveDFSBatch(int bestAnsLen, ArrayList<ArrayDeque<State>> levelTodo, SeenCache seenCache,
            Progress progress) {
        try {
            semaphore.acquire();
            
            BlockingQueue<Batch> processQueue = new PriorityBlockingQueue<>(100, BATCH_PREFIX_COMPARATOR);
            PriorityQueue<Batch> orderingQueue = new PriorityQueue<>(BATCH_PREFIX_COMPARATOR);
            
            BatchStateProcessor batchProcessor = new BatchStateProcessor(partials, processQueue, numThreadsPerGroup);
            new Thread(batchProcessor).start();
            
            for (ArrayDeque<State> states : levelTodo) {
                if (states.isEmpty()) continue;
                Batch batch = new Batch(bestAnsLen, new ArrayList<>(states));
                processQueue.add(batch);
                orderingQueue.add(batch);
            }
            
            Supplier<Long> maxCacheSize = () -> Main.DFS_BATCH_MAX_CACHE / Math.min(numSolvers.get(), Main.DFS_SWARM_MAX_GROUPS);
            
            String answer = super.solveDFSBatch(processQueue, orderingQueue, bestAnsLen, seenCache, maxCacheSize, progress);
            
            batchProcessor.shutdown();
            
            return answer;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            semaphore.release();
        }
    }
    
}