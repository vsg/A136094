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
        StateProcessor processor = new StateProcessor(partials);
        SeenCache seenCache = new SeenCache();
        Bundle[] sortedBundles0 = Bundle.sortBundles(bundles0);
        State state0 = new State("", sortedBundles0, null);
        return solveDFS(state0, bestAnsLen, processor, seenCache);
    }
    
    public String solveDFS(State state, int bestAnsLen, StateProcessor processor, SeenCache seenCache) {
        String prefix = state.prefix;
        Bundle[] sortedBundles = state.sortedBundles;
        
        if (sortedBundles.length == 0) return prefix;

        processor.process(state, bestAnsLen, seenCache);
        
        for (State nextState : state.nextStates) {
            String ans = solveDFS(nextState, bestAnsLen, processor, seenCache);
            if (ans != null) {
                return ans;
            }
        }
        return null;
    }
    
}