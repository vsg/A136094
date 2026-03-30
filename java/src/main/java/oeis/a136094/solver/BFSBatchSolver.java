/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094.solver;

import static oeis.a136094.util.ParallelUtils.processInBatches;

import java.util.ArrayList;
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
import oeis.a136094.util.LogUtils;

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

        public void printProgress(int level, int numNodes, Node someNode) {
            if (Main.PRINT_PROGRESS) {
                long now = System.currentTimeMillis();
                long totalTime = now - begin;
                long plusTime = now - lastProgressTime;
                System.out.println(LogUtils.formatLog("%s %d %d %d [%d ms, +%d ms]; %s", 
                        Bundle.bundlesToString(bundles0), bestAnsLen, level, numNodes, totalTime, plusTime, 
                        Bundle.bundlesToString(someNode.sortedBundles)));
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
        
        BlockingQueue<Batch> processorQueue = new LinkedBlockingQueue<>();
        
        BatchNodeProcessor batchProcessor = new BatchNodeProcessor(partials, processorQueue);
        new Thread(batchProcessor).start();
        
        Bundle[] sortedBundles0 = Bundle.sortBundles(bundles0);
        Node node0 = new Node("", sortedBundles0, null);
        
        String answer = solveBFS(node0, bestAnsLen, processorQueue, progress);
        
        batchProcessor.shutdown();
        
        return answer;
    }

    private String solveBFS(Node node0, int bestAnsLen, Queue<Batch> processorQueue, Progress progress) {
        List<Node> nodes = List.of(node0);
        for (int level = 0; level <= bestAnsLen; level++) {
            if (nodes.isEmpty()) break;
            progress.printProgress(level, nodes.size(), nodes.get(nodes.size()/2));

            List<Batch> batches = makeBatches(bestAnsLen, nodes);
            
            processorQueue.addAll(batches);
            
            Set<Key> seen = new MemoryEfficientHashSet<>();
            
            List<Node> nextNodes = new ArrayList<>();
            for (Batch batch : batches) {
                batch.ensureProcessed();
                
                for (Node node : batch.nodes) {
                    for (Node nextNode : node.nextNodes) {
                        String prefix = nextNode.prefix;
                        Bundle[] sortedBundles = nextNode.sortedBundles;
                        Key key = nextNode.key;
                        
                        if (sortedBundles.length == 0) {
                            return prefix;
                        }
                        
                        if (!seen.add(key)) continue;
                        
                        nextNodes.add(nextNode);
                    }
                }
            }
            nodes = nextNodes;
        }
        return null;
    }

    private List<Batch> makeBatches(int bestAnsLen, List<Node> nodes) {
        List<Batch> batches = new ArrayList<>();
        processInBatches(nodes, 10000, (ArrayList<Node> batchNodes) -> {
            batches.add(new Batch(bestAnsLen, batchNodes));
        });
        return batches;
    }

}