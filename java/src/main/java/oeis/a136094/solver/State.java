/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094.solver;

import java.util.ArrayList;
import java.util.List;

import oeis.a136094.Bundle;
import oeis.a136094.key.Key;

class State {
    
    String prefix;
    Bundle[] sortedBundles;
    Key key;
    List<State> nextStates = new ArrayList<>();

    State(String prefix, Bundle[] sortedBundles, Key key) {
        this.prefix = prefix;
        this.sortedBundles = sortedBundles;
        this.key = key;
    }
    
}