/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094.solver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

class Batch {
    
    int bestAnsLen;
    List<Node> nodes;
    CompletableFuture<Void> processed;
    
    Batch(int bestAnsLen, List<Node> nodes) {
        this.bestAnsLen = bestAnsLen;
        this.nodes = new ArrayList<>(nodes);
        this.processed = new CompletableFuture<>();
    }
    
    public String minPrefix() {
        return !nodes.isEmpty() ? nodes.get(0).prefix : null;
    }
    
    public void ensureProcessed() {
        processed.join();
    }
    
}