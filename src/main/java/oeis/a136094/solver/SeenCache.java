/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094.solver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import oeis.a136094.key.Key;
import oeis.a136094.util.MemoryEfficientHashSet;

public class SeenCache {
    
    private final ArrayList<Set<Key>> levelSeenKeys = new ArrayList<>();
    private final long[] levelSeenIntsCount = new long[100];
    
    public boolean add(int level, Key key) {
        while (levelSeenKeys.size() <= level) {
            levelSeenKeys.add(new MemoryEfficientHashSet<>());
        }
        Set<Key> seen = levelSeenKeys.get(level);
        if (!seen.add(key)) return false;
        levelSeenIntsCount[level] += key.size();
        return true;
    }

    public void clearLevel(int level) {
        if (levelSeenKeys.size() > level) {
            levelSeenKeys.get(level).clear();
            levelSeenIntsCount[level] = 0;
        }
    }
    
    public long cleanUp(long maxCacheSize) {
        long totalCached = Arrays.stream(levelSeenIntsCount).sum();
        for (int level = levelSeenKeys.size()-1; level >= 0; level--) {
            if (totalCached < maxCacheSize) break;
            totalCached -= levelSeenIntsCount[level];
            clearLevel(level);
        }
        return totalCached;
    }
    
}