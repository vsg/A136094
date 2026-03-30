/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094.key;

import static java.lang.Integer.bitCount;
import static oeis.a136094.Bundle.digits;
import static oeis.a136094.util.ParallelUtils.processInBatches;
import static oeis.a136094.util.ParallelUtils.processInParallel;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import oeis.a136094.Bundle;
import oeis.a136094.Main;

public class KeyBuilder {
        
    private final long[] hash = new long[9];
    private final int[] groupSizes = new int[9];
    private final int[] key1 = new int[256];
    private final int[] key2 = new int[256];
    private final int[] swap = new int[9];
            
    public Key makeKey(Bundle[] sortedBundles) {
        int len = sortedBundles.length;
        if (len == 0) {
            return new Key(new int[0]);
        }
        
        int[] digits = new int[9];
        for (int d = 0; d < 9; d++) {
            digits[d] = d;
        }
        
        int digitsMask = digits(sortedBundles);
        int numDigits = bitCount(digitsMask);

        calcStructHash(sortedBundles, len, hash);
        for (int d = 0; d < 9; d++) {
            hash[d] = (hash[d] >>> 1) | (((digitsMask >> d) & 1L) << 63);
        }
        
        calculateDigitGroups(digits, hash, groupSizes);
        
        return findMinKeyUsingPermutations(sortedBundles, len, digits, groupSizes, numDigits, null);
    }
    
    // sort digits by their hash. calculate consecutive equivalence groups of sorted digits.
    // digits within each group have the same hash and produce equivalent bundles (having same shortest 
    // solution length), when applied as permutation of bundle digits.
    // missing digits are sorted to go at the end, not participating in groups and their permutations.
    private void calculateDigitGroups(int[] digits, long[] hash, int[] groupSizes) {
        //Arrays.sort(digits, Comparator.comparing(d -> hash[d]));
        for (int i = 1; i < 9; i++) {
            int t = digits[i];
            int j = i-1;
            while (j >= 0 && hash[digits[j]] > hash[t]) {
                digits[j+1] = digits[j];
                j--;
            }
            digits[j+1] = t;
        }

        int numGroups = 0;
        Arrays.fill(groupSizes, 0);
        for (int d = 0; d < 9; d++) {
            groupSizes[numGroups]++;
            if (d == 8 || (hash[digits[d]] >> 4) != (hash[digits[d+1]] >> 4)) {
                numGroups++;
            }
        }
    }

    private Key findMinKeyUsingPermutations(Bundle[] sortedBundles, int len, int[] digits, int[] groupSizes, 
            int numDigits, int[] swapResult) {
        int[] minKey = key1;
        minKey[0] = Integer.MAX_VALUE;
        
        for (int[] permutation : Permutations.permutations(numDigits, groupSizes)) {
            for (int d = 0; d < 9; d++) {
                int index = (d < numDigits) ? permutation[d] : d;
                swap[digits[index]] = d;
            }
            
            int[] newKey = (minKey == key2) ? key1 : key2;
            
            for (int i = 0; i < len; i++) {
                newKey[i] = sortedBundles[i].swapBundleDigits(swap).pack();
            }
            Arrays.sort(newKey, 0, len);

            if (compareKeys(newKey, minKey, len) < 0) {
                minKey = newKey;
                if (swapResult != null) {
                    for (int i = 0; i < 9; i++) {
                        swapResult[i] = swap[i];
                    }
                }
            }
        }

        return new Key(Arrays.copyOf(minKey, len));
    }

    private int compareKeys(int[] key1, int[] key2, int len) {
        for (int i = 0; i < len; i++) {
            int a = key1[i], b = key2[i];
            if (a != b) {
                return a < b ? -1 : 1;
            }
        }
        return 0;
    }

    private final int[] numHeads = new int[9];
    private final int[] numDigits = new int[9];
    private final int[] headsMask = new int[9];
    private final int[] digitsMask = new int[9];

    public void calcStructHash(Bundle[] sortedBundles, int len, long[] result) {
        Arrays.fill(result, 0L);
        for (int i = 0; i < len; i++) {
            Bundle bundle = sortedBundles[i];
            int heads = bundle.heads();
            int digits = bundle.digits();

            for (int d = 0; d < 9; d++) {
                if (((digits >> d) & 1) != 0) {
                    numDigits[d]++;
                    digitsMask[d] |= digits;

                    if (((heads >> d) & 1) != 0) {
                        numHeads[d]++;
                        headsMask[d] |= heads;
                    }
                }
            }

            if (i+1 == len || sortedBundles[i+1].shape() != sortedBundles[i].shape()) {
                for (int d = 0; d < 9; d++) {
                    long value = result[d];
                    value = 31 * value + numHeads[d];
                    value = 31 * value + numDigits[d];
                    value = 31 * value + bitCount(headsMask[d]);
                    value = 31 * value + bitCount(digitsMask[d]);
                    result[d] = value;
                }
                Arrays.fill(numHeads, 0);
                Arrays.fill(numDigits, 0);
                Arrays.fill(headsMask, 0);
                Arrays.fill(digitsMask, 0);
            }
        }
    }

    public static void generateKeysInParallel(Consumer<Consumer<Bundle[]>> inputCallback, 
            BiConsumer<Bundle[], Key> resultCallback) {
        class Item {
            Bundle[] bundles;
            Key key;
            
            Item(Bundle[] bundles, Key key) {
                this.bundles = bundles; 
                this.key = key;
            }
        }
        Consumer<Consumer<Item>> inputAdapter = (consumer) -> {
            inputCallback.accept((bundles) -> {
                consumer.accept(new Item(bundles, null));
            });
        };
        processInBatches(inputAdapter, 100000, (batch) -> {
            processInParallel(batch, Main.NUM_WORKER_THREADS, () -> {
                KeyBuilder keyBuilder = new KeyBuilder();
                return (item) -> {
                    Bundle[] bundles = item.bundles;
                    Bundle[] sortedBundles = Bundle.sortBundles(bundles);
                    Key key = keyBuilder.makeKey(sortedBundles);
                    item.key = key;
                };
            });
            for (Item item : batch) {
                resultCallback.accept(item.bundles, item.key);
            }
        });
    }

    public static void generateKeysInParallel(Collection<Bundle[]> input, BiConsumer<Bundle[], Key> resultCallback) {
        KeyBuilder.generateKeysInParallel((consumer) -> {
            for (Bundle[] bundles : input) {
                consumer.accept(bundles);
            }
        }, resultCallback);
    }

}