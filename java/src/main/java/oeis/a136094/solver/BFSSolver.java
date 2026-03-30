/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094.solver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import oeis.a136094.Bundle;
import oeis.a136094.Main;
import oeis.a136094.partials.Partials;
import oeis.a136094.util.Utils;

public class BFSSolver extends Solver {

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
                System.out.println(Utils.formatLog("%s %d %d %d [%d ms, +%d ms]; %s", 
                        Bundle.bundlesToString(bundles0), bestAnsLen, level, numNodes, totalTime, plusTime, 
                        Bundle.bundlesToString(someNode.sortedBundles)));
                lastProgressTime = now;
            }
        }
        
    }
    
    public BFSSolver(Partials partials) {
        super(partials);
    }

    @Override
    public String solve(Bundle[] bundles0, int bestAnsLen) {
        Progress progress = new Progress(bundles0, bestAnsLen);
        
        NodeProcessor processor = new NodeProcessor(partials);
        
        Bundle[] sortedBundles0 = Bundle.sortBundles(bundles0);
        Node node0 = new Node("", sortedBundles0, null);
        
        List<Node> nodes = Arrays.asList(node0);
        for (int level = 0; level <= bestAnsLen; level++) {
            if (nodes.isEmpty()) break;
            progress.printProgress(level, nodes.size(), nodes.get(nodes.size()/2));
            
            SeenCache seenCache = new SeenCache();
            
            List<Node> nextNodes = new ArrayList<>();
            for (Node node : nodes) {
                String prefix = node.prefix;
                Bundle[] sortedBundles = node.sortedBundles;

                if (sortedBundles.length == 0) return prefix;
                
                processor.process(node, bestAnsLen, seenCache);
                
                nextNodes.addAll(node.nextNodes);
            }
            nodes = nextNodes;
        }
        return null;
    }

}