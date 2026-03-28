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
        
        public void printProgress(int level, int numStates, State someState) {
            if (Main.PRINT_PROGRESS) {
                long now = System.currentTimeMillis();
                long totalTime = now - begin;
                long plusTime = now - lastProgressTime;
                System.out.println(Utils.formatLog("%s %d %d %d [%d ms, +%d ms]; %s", 
                        Bundle.bundlesToString(bundles0), bestAnsLen, level, numStates, totalTime, plusTime, 
                        Bundle.bundlesToString(someState.sortedBundles)));
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
        
        StateProcessor processor = new StateProcessor(partials);
        
        Bundle[] sortedBundles0 = Bundle.sortBundles(bundles0);
        State state0 = new State("", sortedBundles0, null);
        
        List<State> states = Arrays.asList(state0);
        for (int level = 0; level <= bestAnsLen; level++) {
            if (states.isEmpty()) break;
            progress.printProgress(level, states.size(), states.get(states.size()/2));
            
            SeenCache seenCache = new SeenCache();
            
            List<State> nextStates = new ArrayList<>();
            for (State state : states) {
                String prefix = state.prefix;
                Bundle[] sortedBundles = state.sortedBundles;

                if (sortedBundles.length == 0) return prefix;
                
                processor.process(state, bestAnsLen, seenCache);
                
                nextStates.addAll(state.nextStates);
            }
            states = nextStates;
        }
        return null;
    }

}