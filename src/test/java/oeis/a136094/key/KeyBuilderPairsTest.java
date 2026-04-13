/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094.key;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import oeis.a136094.Bundle;

@Tag("slow")
class KeyBuilderPairsTest {

    static final int NUM_UNIQUE_BUNDLE_PAIRS = 10465;

    static List<Bundle[]> pairs;
    
    KeyBuilder keyBuilder = new KeyBuilder();
    
    @BeforeAll
    static void init() {
        pairs = pairs();
    }
    
    @Test
    void swapDoesNotChangeKey() {
        pairs.forEach(bundles -> {
            Bundle b1 = bundles[0];
            Bundle b2 = bundles[1];
            
            assertThat(key(b1, b2)).isEqualTo(key(b2, b1));
        });
    }
    
    @Test
    void swapDoesNotChangeK2() {
        pairs.forEach(bundles -> {
            Bundle b1 = bundles[0];
            Bundle b2 = bundles[1];
            
            assertThat(k2(b1, b2)).isEqualTo(k2(b2, b1));
        });
    }
    
    @Test
    void uniquePairsCount() {
        long uniquePairK2 = pairs.stream()
                .map(bundles -> k2(bundles[0], bundles[1]))
                .distinct()
                .count();
        
        long uniquePairKeys = pairs.stream()
                .map(bundles -> key(bundles))
                .distinct()
                .count();
        
        assertThat(uniquePairK2).isEqualTo(uniquePairKeys).isEqualTo(NUM_UNIQUE_BUNDLE_PAIRS);
    }
    
    private static List<Bundle[]> pairs() {
        List<Bundle[]> result = new ArrayList<>();
        
        for (int numDigits1 = 1; numDigits1 <= 9; numDigits1++) {
            int digits1 = (1 << numDigits1) - 1;
            for (int numHeads1 = 1; numHeads1 <= numDigits1; numHeads1++) {
                int heads1 = (1 << numHeads1) - 1;
                Bundle bundle1 = Bundle.of(heads1, digits1);
                
                for (Bundle bundle2 : Bundle.ALL_BUNDLES) {
                    result.add(new Bundle[] {bundle1, bundle2});
                }
            }
        }
        
        return result;
    }

    private int k2(Bundle bundle1, Bundle bundle2) {
        int[] groupSize = new int[9];
        int[] groupStartIndex = new int[9];
        return KeyUtils.K2(bundle1, bundle2, null, groupSize, groupStartIndex);
    }
    
    private Key key(Bundle... bundles) {
        return keyBuilder.makeKey(Bundle.sortBundles(bundles));
    }

}
