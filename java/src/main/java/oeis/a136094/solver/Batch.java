/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094.solver;

import java.util.ArrayList;
import java.util.List;

class Batch {
    
    int bestAnsLen;
    List<State> states;
    volatile boolean processed;
    
    Batch(int bestAnsLen, List<State> states) {
        this.bestAnsLen = bestAnsLen;
        this.states = new ArrayList<>(states);
    }
    
    public String minPrefix() {
        return !states.isEmpty() ? states.get(0).prefix : null;
    }
    
    public void ensureProcessed() {
        while (!processed) {
            Thread.yield();//XXX
        }
    }
    
}