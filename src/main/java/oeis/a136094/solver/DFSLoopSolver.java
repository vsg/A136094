/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094.solver;

import oeis.a136094.Bundle;
import oeis.a136094.key.Key;
import oeis.a136094.key.KeyBuilder;
import oeis.a136094.move.MoveBuilder;
import oeis.a136094.partials.Partials;
import oeis.a136094.partials.PartialsLookup;

public class DFSLoopSolver extends Solver {

    public DFSLoopSolver(Partials partials) {
        super(partials);
    }

    private static class LevelState {
        String prefix;
        Bundle[] bundles;
        int movesMask;
        int moveIter;
    }
    
    private final KeyBuilder keyBuilder = new KeyBuilder();
    private final MoveBuilder moveBuilder = new MoveBuilder();

    @Override
    public String solve(Bundle[] bundles0, int bestAnsLen) {
        PartialsLookup partialsLookup = new PartialsLookup(partials);
        
        SeenCache seenCache = new SeenCache();
        
        return solveDFSLoop(bundles0, bestAnsLen, partialsLookup, seenCache);
    }
    
    private String solveDFSLoop(Bundle[] bundles0, int bestAnsLen, PartialsLookup partialsLookup, SeenCache seenCache) {
        LevelState[] states = new LevelState[bestAnsLen+1];
        for (int i = 0; i <= bestAnsLen; i++) {
            states[i] = new LevelState();
        }
        
        int level = 0;
        
        states[0].prefix = "";
        states[0].bundles = bundles0;
        states[0].movesMask = Bundle.heads(bundles0);
        states[0].moveIter = 0;
        
        while (level >= 0) {
            LevelState s = states[level];

            // pick next move
            while (s.moveIter < 9 && ((s.movesMask >> s.moveIter) & 1) == 0) {
                s.moveIter++;
            }
            if (s.moveIter == 9) {
                level--;
                continue;
            }

            // make move
            Bundle[] nextBundles = moveBuilder.makeMoveAndSort(s.bundles, s.moveIter);
            String nextPrefix = s.prefix + (s.moveIter + 1);
            
            s.moveIter++;

            // check result
            if (nextBundles.length == 0) {
                return nextPrefix;
            }

            // check duplicates
            Key nextKey = keyBuilder.makeKey(nextBundles); // bundles must be sorted
            if (!seenCache.add(level+1, nextKey)) {
                continue;
            }

            // check canHaveAnswer
            int nextMoves = partialsLookup.canHaveAnswer(nextPrefix, nextBundles, bestAnsLen); // bundles must be sorted
            if (nextMoves == 0) {
                continue;
            }
            
            // try next level
            states[level+1].prefix = nextPrefix;
            states[level+1].bundles = nextBundles;
            states[level+1].movesMask = nextMoves;
            states[level+1].moveIter = 0;
            
            level++;
        }
        
        return null;
    }

}
