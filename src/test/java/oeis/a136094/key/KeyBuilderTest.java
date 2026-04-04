/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094.key;

import static oeis.a136094.key.Permutations.permutations;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import oeis.a136094.Bundle;

class KeyBuilderTest {

    KeyBuilder keyBuilder = new KeyBuilder();
    
    @Test
    void basic() {
        assertThat(key("1/1")).isEqualTo(key("2/2")).isEqualTo(key("3/3"));
        assertThat(key("12/12")).isEqualTo(key("13/13")).isEqualTo(key("34/34"));
        assertThat(key("1/123")).isEqualTo(key("2/123")).isEqualTo(key("3/123")).isEqualTo(key("5/345"));
        
        assertThat(key("1/1")).isNotEqualTo(key("1/1 2/2")).isNotEqualTo(key("12/12")); // shape mismatch
        
        assertThat(key("1/1 2/23")).isEqualTo(key("2/23 1/1")).isEqualTo(key("4/45 6/6")); // bundle swap, digit swap
        
        assertThat(key("12/12 13/13")).isEqualTo(key("56/56 67/67")); // 1->6, 2->5, 3->7
        
        assertThat(key("1/123 2/234")).isEqualTo(key("9/789 6/679")); // 1->6 2->9, 3->7, 4->8, bundle swap

        assertThat(key("1/1234 125/125")).isEqualTo(key("2/1234 125/125")); // 1->2, 2->1
        assertThat(key("1/1234 125/125")).isEqualTo(key("1/1234 135/135")); // 2->3, 3->2
        assertThat(key("1/1234 125/125")).isEqualTo(key("5/1345 125/125")); // 1->5, 5->2, 2->1
        assertThat(key("1/1234 125/125")).isEqualTo(key("1/1267 128/128")); // 3->6, 4->7, 5->8
        
        assertThat(key("1/1234 125/125")).isNotEqualTo(key("3/1234 125/125")); // 3 not shared
        assertThat(key("1/1234 125/125")).isNotEqualTo(key("4/1234 125/125")); // 4 not shared
        assertThat(key("1/1234 125/125")).isNotEqualTo(key("1/123 125/125")); // shape mismatch
        assertThat(key("1/1234 125/125")).isNotEqualTo(key("1/1234 25/125")); // shape mismatch
        assertThat(key("1/1234 125/125")).isNotEqualTo(key("1/1234 25/25")); // shape mismatch
        assertThat(key("1/1234 125/125")).isNotEqualTo(key("1/1234")); // shape mismatch
        
        assertThat(key("16/16 24/24 26/26 4/34 4/45 6/36 7/37 7/127 25/125 2/123 5/135 5/145"))
                .isEqualTo(key("17/147 7/347 6/467 1/148 1/134 78/78 57/57 45/45 8/38 8/18 6/36 5/35"));

        assertThat(key("15/15 24/24 26/26 4/34 4/45 6/36 7/37 7/127 25/125 2/123 5/135 5/145"))
                .isNotEqualTo(key("17/147 7/347 6/467 1/148 1/134 78/78 57/57 45/45 8/38 8/18 6/36 5/35"));
    }
    
    @Test
    void singleBundle() {
        List<Bundle[]> bundlesByShape = Arrays.stream(Bundle.BUNDLES_OF_SHAPE).filter(bb -> bb.length > 0).toList();
        
        assertThat(bundlesByShape).allSatisfy(bundles -> {
            assertThat(Arrays.stream(bundles).map(b -> key(b)).distinct().count()).isEqualTo(1);
        });
        
        assertThat(bundlesByShape.stream().map(bb -> key(bb[0])).distinct().count()).isEqualTo(bundlesByShape.size());
    }
    
    @Tag("slow")
    @Test
    void bundlePairSwap() {
        bundlePairs().forEach(bundles -> {
            Bundle b1 = bundles[0];
            Bundle b2 = bundles[1];
            
            assertThat(key(b1, b2)).isEqualTo(key(b2, b1));
        });
    }
    
    @ParameterizedTest
    @ValueSource(strings = {
            "1/1234 125/125 256/256 2/278",
            "1/12345 1/12346 2/12357 4/23478",
            "123/12345 167/12467 24/12348 39/12369",
            "16/16 24/24 26/26 4/34 4/45 6/36 7/37 7/127 25/125 2/123 5/135 5/145",
    })
    void permutationsOfDigits(String bundlesStr) {
        Bundle[] bundles = Bundle.parseBundles(bundlesStr);
        
        long uniqueKeys = Arrays.stream(permutations(9))
                .map(perm -> swapDigits(bundles, perm))
                .map(bb -> key(bb))
                .distinct()
                .count();
        
        assertThat(uniqueKeys).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "1/1234 125/125 256/256",
            "1/12345 1/12346 2/12357 4/23478",
            "123/12345 167/12467 24/12348 39/12369 1/12379",
    })
    void permutationsOfBundles(String bundlesStr) {
        Bundle[] bundles = Bundle.parseBundles(bundlesStr);

        long uniqueKeys = Arrays.stream(permutations(bundles.length))
                .map(perm -> swapBundles(bundles, perm))
                .map(bb -> key(bb))
                .distinct()
                .count();
        
        assertThat(uniqueKeys).isEqualTo(1);
    }

    @Test
    void pinSpecificImplementation() {
        assertThat(key("1/1").asBundles()).isEqualTo(Bundle.parseBundles("1/1"));
        assertThat(key("1/12").asBundles()).isEqualTo(Bundle.parseBundles("2/12"));
        assertThat(key("12/12").asBundles()).isEqualTo(Bundle.parseBundles("12/12"));
        assertThat(key("123/123456").asBundles()).isEqualTo(Bundle.parseBundles("456/123456"));
        assertThat(key("12/12 23/23").asBundles()).isEqualTo(Bundle.parseBundles("13/13 23/23"));
        assertThat(key("12/12 34/34").asBundles()).isEqualTo(Bundle.parseBundles("12/12 34/34"));
        assertThat(key("12345/12345 6789/6789").asBundles()).isEqualTo(Bundle.parseBundles("1234/1234 56789/56789"));
        assertThat(key("1/1234 125/125").asBundles()).isEqualTo(Bundle.parseBundles("5/2345 145/145"));
        assertThat(key("1/1234 125/125 256/256").asBundles()).isEqualTo(Bundle.parseBundles("125/125 6/3456 256/256"));
        
        assertThat(key("1/12345 1/12346 2/12357 4/23478").asBundles()).isEqualTo(
                Bundle.parseBundles("6/13567 7/34578 8/25678 8/45678"));
        
        assertThat(key("123/12345 167/12467 24/12348 39/12369 1/12379").asBundles()).isEqualTo(
                Bundle.parseBundles("27/26789 48/14789 9/25789 569/45689 789/34789"));

        assertThat(key("16/16 24/24 26/26 4/34 4/45 6/36 7/37 7/127 25/125 2/123 5/135 5/145").asBundles()).isEqualTo(
                Bundle.parseBundles("1/13 2/23 2/27 4/34 4/456 15/15 6/356 16/16 26/26 7/257 7/357 67/567"));
    }
    
    private static List<Bundle[]> bundlePairs() {
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

    private Key key(Bundle... bundles) {
        return keyBuilder.makeKey(Bundle.sortBundles(bundles));
    }
    
    private Key key(String bundlesStr) {
        return keyBuilder.makeKey(Bundle.sortBundles(Bundle.parseBundles(bundlesStr)));
    }

    private static Bundle[] swapDigits(Bundle[] bundles, int[] permutation) {
        return Arrays.stream(bundles)
                .map(b -> b.swapBundleDigits(permutation))
                .toArray(Bundle[]::new);
    }

    private static Bundle[] swapBundles(Bundle[] bundles, int[] permutation) {
        return Arrays.stream(permutation)
                .mapToObj(i -> bundles[i])
                .toArray(Bundle[]::new);
    }

}
