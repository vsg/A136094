/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094.solver;

import java.util.List;

import oeis.a136094.Bundle;
import oeis.a136094.key.Key;
import oeis.a136094.key.KeyBuilder;
import oeis.a136094.move.MoveBuilder;
import oeis.a136094.partials.Partials;
import oeis.a136094.partials.PartialsLookup;

public class StateProcessor {
    
    private final KeyBuilder keyBuilder = new KeyBuilder();
    private final MoveBuilder moveBuilder = new MoveBuilder();
    private final PartialsLookup partialsLookup;

    public StateProcessor(Partials partials) {
        this.partialsLookup = new PartialsLookup(partials);
    }

    public void process(State state, int bestAnsLen, SeenCache seenCache) {
        String prefix = state.prefix;
        Bundle[] sortedBundles = state.sortedBundles;
        List<State> nextStates = state.nextStates;
        
        int moves = partialsLookup.canHaveAnswer(prefix, sortedBundles, bestAnsLen);
        if (moves == 0) return;
        
        for (int m = 0; m < 9; m++) {
            if (((moves >> m) & 1) == 0) continue;
            
            String nextPrefix = prefix + (1+m);
            Bundle[] nextBundles = moveBuilder.makeMoveAndSort(sortedBundles, m);
            Key nextKey = keyBuilder.makeKey(nextBundles);
            int nextLevel = nextPrefix.length();
            
            if (!seenCache.add(nextLevel, nextKey)) continue;
            
            State nextState = new State(nextPrefix, nextBundles, nextKey);
            nextStates.add(nextState);
        }
    }
    
}