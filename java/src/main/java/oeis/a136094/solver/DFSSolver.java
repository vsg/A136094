/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094.solver;

import oeis.a136094.Bundle;
import oeis.a136094.partials.Partials;

public class DFSSolver extends Solver {

    public DFSSolver(Partials partials) {
        super(partials);
    }

    @Override
    public String solve(Bundle[] bundles0, int bestAnsLen) {
        NodeProcessor processor = new NodeProcessor(partials);
        
        SeenCache seenCache = new SeenCache();
        
        Bundle[] sortedBundles0 = Bundle.sortBundles(bundles0);
        Node node0 = new Node("", sortedBundles0, null);
        
        return solveDFS(node0, bestAnsLen, processor, seenCache);
    }
    
    public String solveDFS(Node node, int bestAnsLen, NodeProcessor processor, SeenCache seenCache) {
        String prefix = node.prefix;
        Bundle[] sortedBundles = node.sortedBundles;
        
        if (sortedBundles.length == 0) return prefix;

        processor.process(node, bestAnsLen, seenCache);
        
        for (Node nextNode : node.nextNodes) {
            String ans = solveDFS(nextNode, bestAnsLen, processor, seenCache);
            if (ans != null) {
                return ans;
            }
        }
        return null;
    }
    
}