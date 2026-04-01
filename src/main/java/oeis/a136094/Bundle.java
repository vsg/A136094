/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094;

import static java.lang.Integer.bitCount;
import static oeis.a136094.util.BitUtils.BITWISE_OR;
import static oeis.a136094.util.BitUtils.MASK_18;
import static oeis.a136094.util.BitUtils.MASK_9;
import static oeis.a136094.util.BitUtils.digitsMaskToString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Bundle {
    
    public static final int[] MAX_BUNDLE_INDEX_OF_SHAPE = new int[256];
    public static final Bundle[][] BUNDLES_OF_SHAPE = new Bundle[256][];
    public static final List<Bundle> ALL_BUNDLES = new ArrayList<>();

    private static final Bundle[] packedToBundle = new Bundle[1<<18];
    private static final Bundle[][] indexToPieces = new Bundle[20000][];
    private static final String[] indexToString = new String[20000];
    private static final Map<String, Bundle> stringToBundle = new HashMap<>();

    static {
        initBundles();
        initByPackedLookup();
        initByShapeLookup();
        initPieces();
        initToString();
    }

    private static void initBundles() {
        int index = 0;
        
        for (int numDigits = 1; numDigits <= 9; numDigits++) {
            for (int digits = 1; digits <= MASK_9; digits++) {
                if (bitCount(digits) != numDigits) continue;
                
                for (int numHeads = 1; numHeads <= numDigits; numHeads++) {
                    for (int heads = 1; heads <= digits; heads++) {
                        if (bitCount(heads) != numHeads) continue;
                        
                        if ((heads & ~digits) != 0) continue;
                        
                        Bundle bundle = new Bundle(heads, digits, index++);
                        ALL_BUNDLES.add(bundle);
                    }
                }
            }
        }
    }

    private static void initByPackedLookup() {
        for (Bundle bundle : ALL_BUNDLES) {
            int packed = bundle.pack();
            
            packedToBundle[packed] = bundle;
        }        
    }

    private static void initByShapeLookup() {
        Bundle[][] bundlesByShape = new Bundle[256][6000];
        int[] numBundlesByShape = new int[256];
        
        for (Bundle bundle : ALL_BUNDLES) {
            int index = bundle.index();
            int shape = bundle.shape();
            
            int numShapeBundles = numBundlesByShape[shape]++;
            bundlesByShape[shape][numShapeBundles] = bundle;
            
            MAX_BUNDLE_INDEX_OF_SHAPE[shape] = index;
        }

        for (int shape = 0; shape < 256; shape++) {
            BUNDLES_OF_SHAPE[shape] = Arrays.copyOf(bundlesByShape[shape], numBundlesByShape[shape]);
        }
    }
    
    private static void initPieces() {
        for (Bundle bundle : ALL_BUNDLES) {
            int heads = bundle.heads();
            int digits = bundle.digits();
            int index = bundle.index();
            
            indexToPieces[index] = IntStream.range(0, 9)
                    .filter(d -> ((heads >> d) & 1) != 0)
                    .mapToObj(d -> packedToBundle[pack(1 << d, digits)])
                    .toArray(Bundle[]::new);
        }
    }

    private static void initToString() {
        for (Bundle bundle : ALL_BUNDLES) {
            int heads = bundle.heads();
            int digits = bundle.digits();
            int index = bundle.index();
            
            String bundleStr = digitsMaskToString(heads) + "/" + digitsMaskToString(digits);
            
            stringToBundle.put(bundleStr, bundle);
            indexToString[index] = bundleStr;
        }
    }
    
    private final int heads;
    private final int digits;
    private final int numHeads;
    private final int numDigits;
    private final int index;
    private final int shape;
    private final int packed;

    public Bundle(int heads, int digits, int index) {
        this.heads = heads;
        this.digits = digits;
        this.numHeads = bitCount(heads);
        this.numDigits = bitCount(digits);
        this.index = index;
        this.shape = shape(numHeads, numDigits);
        this.packed = pack(heads, digits);
    }
    
    public int heads() {
        return heads;
    }
    
    public int digits() {
        return digits;
    }
    
    public int numHeads() {
        return numHeads;
    }
    
    public int numDigits() {
        return numDigits;
    }
    
    public int index() {
        return index;
    }
    
    public int shape() {
        return shape;
    }
    
    public int pack() {
        return packed;
    }
    
    public static Bundle of(int heads, int digits) {
        return packedToBundle[pack(heads, digits)];
    }
    
    public static Bundle unpack(int packed) {
        return packedToBundle[packed];
    }
    
    public Bundle[] pieces() {
        return indexToPieces[index];
    }
    
    public int toSortKey() {
        return (shape << 18) | packed;
    }

    public Bundle swapBundleDigits(int[] swap) {
        int hh = 0, dd = 0;
        for (int src = 0; src < 9; src++) {
            int dest = swap[src];
            hh |= ((heads >> src) & 1) << dest;
            dd |= ((digits >> src) & 1) << dest;
        }
        return packedToBundle[pack(hh, dd)];
    }

    public int[] makeBundleSwap1234() {
        int nextHeadDigit = 0;
        int nextTailDigit = numHeads;
        int nextMissingDigit = numDigits;
        int[] swap = new int[9];
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
        return swap;
    }
    
    @Override
    public String toString() {
        return indexToString[index];
    }

    public static Bundle parse(String bundleStr) {
        Bundle bundle = stringToBundle.get(bundleStr);
        if (bundle == null) throw new RuntimeException("Failed to parse a bundle: " + bundleStr);
        return bundle;
    }

    public static int[] packAll(Bundle[] bundles) {
        return Arrays.stream(bundles)
                .mapToInt(Bundle::pack)
                .toArray();
    }
    
    public static Bundle[] unpackAll(int[] bundles) {
        return Arrays.stream(bundles)
                .mapToObj(Bundle::unpack)
                .toArray(Bundle[]::new);
    }
    
    public static int heads(Bundle[] bundles) {
        return Arrays.stream(bundles)
                .mapToInt(Bundle::heads)
                .reduce(0, BITWISE_OR);
    }
    
    public static int digits(Bundle[] bundles) {
        return Arrays.stream(bundles)
                .mapToInt(Bundle::digits)
                .reduce(0, BITWISE_OR);
    }
    
    public static String bundlesToString(Bundle... bundles) {
        return Arrays.stream(bundles)
               .map(Bundle::toString)
               .collect(Collectors.joining(" "));
    }
    
    public static Bundle[] parseBundles(String str) {
        return Arrays.stream(str.split(" "))
                .map(Bundle::parse)
                .toArray(Bundle[]::new);
    }

    public static int shape(int numHeads, int numDigits) {
        return (numDigits << 4) | numHeads;
    }

    public static int pack(int heads, int digits) {
        return (heads << 9) | digits;
    }
    
    public static Bundle[] sortBundles(Bundle[] bundles) {
        return Arrays.stream(bundles)
                .mapToInt(b -> -b.toSortKey())  // sort in descending order, bigger bundles go first
                .sorted()
                .mapToObj(b -> unpack((-b) & MASK_18))
                .toArray(Bundle[]::new);
    }

}
