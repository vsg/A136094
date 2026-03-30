/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094.solver;

import java.util.ArrayList;
import java.util.List;

class Batch {
    
    int bestAnsLen;
    List<Node> nodes;
    volatile boolean processed;
    
    Batch(int bestAnsLen, List<Node> nodes) {
        this.bestAnsLen = bestAnsLen;
        this.nodes = new ArrayList<>(nodes);
    }
    
    public String minPrefix() {
        return !nodes.isEmpty() ? nodes.get(0).prefix : null;
    }
    
    public void ensureProcessed() {
        while (!processed) {
            Thread.yield();//XXX
        }
    }
    
}