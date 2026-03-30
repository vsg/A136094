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
        private long numNodesDone;

        public Progress(Bundle[] bundles0, int bestAnsLen) {
            this.bundles0 = bundles0;
            this.bestAnsLen = bestAnsLen;
        }

        public void onNodeDone() {
            numNodesDone++;
        }

        public void printProgress(Node node, long totalCached, int processQueueSize, int orderingQueueSize) {
            if (Main.PRINT_PROGRESS) {
                long now = System.currentTimeMillis();
                long totalTime = now - begin;
                long plusTime = now - lastProgressTime;
                System.out.println(Utils.formatLog("%s %d %d, %d moves, %d cache, %s [%d ms, +%d ms] {%d, %d}", 
                        Bundle.bundlesToString(bundles0), bestAnsLen, node.prefix.length(), numNodesDone, totalCached, 
                        node.prefix, totalTime, plusTime, processQueueSize, orderingQueueSize));
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

        BatchNodeProcessor batchProcessor = new BatchNodeProcessor(partials, processQueue);
        new Thread(batchProcessor).start();

        Bundle[] sortedBundles0 = Bundle.sortBundles(bundles0);
        Node node0 = new Node("", sortedBundles0, null);
        Batch batch0 = new Batch(bestAnsLen, Arrays.asList(node0));
        
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
        long numNodesSinceCleanup = progress.numNodesDone;
        
        Batch batch;
        while ((batch = orderingQueue.poll()) != null) {
            if (batch.bestAnsLen != bestAnsLen) throw new RuntimeException();
            
            batch.ensureProcessed();
            
            List<Node> nextBatchNodes = new ArrayList<Node>();
            
            for (Node node : batch.nodes) {
                String prefix = node.prefix;
                Bundle[] sortedBundles = node.sortedBundles;
                
                if (sortedBundles.length == 0) return prefix;
                
                for (Node nextNode : node.nextNodes) {
                    String nextPrefix = nextNode.prefix;
                    Key nextKey = nextNode.key;
                    
                    int nextLevel = nextPrefix.length();
                    if (!seenCache.add(nextLevel, nextKey)) continue;
                    
                    nextBatchNodes.add(nextNode);
                    
                    if (nextBatchNodes.size() == Main.DFS_BATCH_SIZE) {
                        Batch nextBatch = new Batch(bestAnsLen, nextBatchNodes);
                        processQueue.add(nextBatch);
                        orderingQueue.add(nextBatch);
                        nextBatchNodes = new ArrayList<Node>();
                    }
                }
                
                node.nextNodes.clear();
                
                progress.onNodeDone();
                
                numNodesSinceCleanup++;
                if (numNodesSinceCleanup >= 1000000) {
                    long totalCached = seenCache.cleanup(maxCacheSize.get());
                    progress.printProgress(node, totalCached, processQueue.size(), orderingQueue.size());
                    numNodesSinceCleanup = 0;
                }
            }
            
            if (!nextBatchNodes.isEmpty()) {
                Batch nextBatch = new Batch(bestAnsLen, nextBatchNodes);
                processQueue.add(nextBatch);
                orderingQueue.add(nextBatch);
            }
        }
        
        return null;
    }

}