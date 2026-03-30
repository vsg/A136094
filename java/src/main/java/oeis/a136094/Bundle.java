/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094;

import static java.lang.Integer.bitCount;
import static oeis.a136094.util.Utils.MASK_18;
import static oeis.a136094.util.Utils.MASK_4;
import static oeis.a136094.util.Utils.MASK_9;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Bundle {
    
    public static final int[] MAX_BUNDLE_INDEX_OF_SHAPE = new int[256];
    public static final Bundle[][] BUNDLES_OF_SHAPE = new Bundle[256][];

    private static final Bundle[] packedToBundle = new Bundle[1<<18];
    private static final String[] indexToString = new String[20000];
    private static final Map<String, Bundle> stringToBundle = new HashMap<>();

    static {
        Bundle[][] bundlesByShape = new Bundle[256][6000];
        int[] numBundlesByShape = new int[256];
        
        int numBundles = 0;
        
        for (int numDigits = 1; numDigits <= 9; numDigits++) {
            for (int digits = 1; digits <= MASK_9; digits++) {
                if (bitCount(digits) != numDigits) continue;
                
                for (int numHeads = 1; numHeads <= numDigits; numHeads++) {
                    for (int heads = 1; heads <= digits; heads++) {
                        if (bitCount(heads) != numHeads) continue;
                        
                        if ((heads & ~digits) != 0) continue;

                        int bundleIndex = numBundles++;
                        String bundleStr = digitsToString(heads) + "/" + digitsToString(digits);
                        
                        Bundle bundle = new Bundle(heads, digits, bundleIndex);
                        int shape = bundle.shape();
                        int packed = bundle.pack();
                        
                        stringToBundle.put(bundleStr, bundle);
                        indexToString[bundleIndex] = bundleStr;
                        
                        packedToBundle[packed] = bundle;
                        
                        int numShapeBundles = numBundlesByShape[shape]++;
                        bundlesByShape[shape][numShapeBundles] = bundle;
                        
                        MAX_BUNDLE_INDEX_OF_SHAPE[shape] = bundleIndex;
                    }
                }
            }
        }

        for (int shape = 0; shape < 256; shape++) {
            BUNDLES_OF_SHAPE[shape] = Arrays.copyOf(bundlesByShape[shape], numBundlesByShape[shape]);
        }
    }

    private final int bundle;
    private final int shape;
    private final int index;

    public Bundle(int heads, int digits, int index) {
        this.bundle = (heads << 9) | digits;
        this.shape = (bitCount(digits) << 4) | bitCount(heads);
        this.index = index;
    }
    
    public int index() {
        return index;
    }
    
    public int heads() {
        return bundle >> 9;
    }
    
    public int digits() {
        return bundle & MASK_9;
    }
    
    public int numHeads() {
        return shape & MASK_4;
    }
    
    public int numDigits() {
        return shape >> 4;
    }
    
    public int shape() {
        return shape;
    }
    
    public int pack() {
        return bundle;
    }
    
    public static Bundle unpack(int packed) {
        return packedToBundle[packed];
    }
    
    public int toSortKey() {
        return (shape << 18) | bundle;
    }

    public Bundle swapBundleDigits(int[] swap) {
        int result = 0;
        for (int src = 0; src < 9; src++) {
            int dest = swap[src];
            result |= ((bundle >> (9 + src)) & 1) << (9 + dest);
            result |= ((bundle >> src) & 1) << dest;
        }
        return unpack(result);
    }

    @Override
    public String toString() {
        return indexToString[index];
    }

    public static Bundle parse(String str) {
        Bundle bundle = stringToBundle.get(str);
        if (bundle == null) throw new RuntimeException("Failed to parse a bundle: " + str);
        return bundle;
    }

    public static int[] packAll(Bundle[] bundles) {
        int[] result = new int[bundles.length];
        for (int i = 0; i < bundles.length; i++) {
            result[i] = bundles[i].pack();
        }
        return result;
    }
    
    public static Bundle[] unpackAll(int[] bundles) {
        Bundle[] result = new Bundle[bundles.length];
        for (int i = 0; i < bundles.length; i++) {
            result[i] = unpack(bundles[i]);
        }
        return result;
    }
    
    public static void makeBundleSwap1234(Bundle bundle, int[] swap) {
        int heads = bundle.heads();
        int digits = bundle.digits();
        int numHeads = bundle.numHeads();
        int numDigits = bundle.numDigits();
        int nextHeadDigit = 0;
        int nextTailDigit = numHeads;
        int nextMissingDigit = numDigits;
        for (int d = 0; d < 9; d++) {
            int mask = 1 << d;
            if ((heads & mask) != 0) {
                swap[d] = nextHeadDigit++;
            } else if ((digits & mask) != 0) {
                swap[d] = nextTailDigit++;
            } else {
                swap[d] = nextMissingDigit++;
            }
        }
    }
    
    public static int swapPackedDigits(int bundle, int d1, int d2) {
        int heads = bundle >> 9;
        int digits = bundle & MASK_9;
        int hh = ((heads >> d1) ^ (heads >> d2)) & 1;
        int dd = ((digits >> d1) ^ (digits >> d2)) & 1;
        heads ^= (hh << d1) | (hh << d2);
        digits ^= (dd << d1) | (dd << d2);
        return (heads << 9) | digits;
    }
    
    public static int heads(Bundle[] bundles) {
        return heads(bundles, bundles.length);
    }

    public static int digits(Bundle[] bundles) {
        return digits(bundles, bundles.length);
    }

    public static int heads(Bundle[] bundles, int len) {
        int result = 0;
        for (int i = 0; i < len; i++) {
            result |= bundles[i].heads();
            if (result == MASK_9) break;
        }
        return result;
    }
    
    public static int digits(Bundle[] bundles, int len) {
        int result = 0;
        for (int i = 0; i < len; i++) {
            result |= bundles[i].digits();
            if (result == MASK_9) break;
        }
        return result;
    }
    
    public static final String bundlesToString(Bundle... bundles) {
        return Arrays.stream(bundles)
               .map(Bundle::toString)
               .collect(Collectors.joining(" "));
    }
    
    public static final Bundle[] parseBundles(String str) {
        return Arrays.stream(str.split(" "))
                .map(Bundle::parse)
                .toArray(Bundle[]::new);
    }

    public static int shape(int numHeads, int numDigits) {
        return (numDigits << 4) | numHeads;
    }

    public static String digitsToString(int mask) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 9; i++) {
            if (((mask >> i) & 1) != 0) {
                builder.append((char)('1' + i));
            }
        }
        return builder.toString();
    }

    public static Bundle[] sortBundles(Bundle[] bundles) {
        int len = bundles.length;
        int[] tmp = new int[len];
    
        for (int i = 0; i < len; i++) {
            tmp[i] = -bundles[i].toSortKey(); // sort in descending order, bigger bundles go first
        }
    
        Arrays.sort(tmp, 0, len);
    
        Bundle[] sortedBundles = new Bundle[len];
        for (int i = 0; i < len; i++) {
            sortedBundles[i] = unpack((-tmp[i]) & MASK_18);
        }
        return sortedBundles;
    }

}