/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094.key;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class PermutationPairs {

    private static final byte MASK_4 = (1 << 4) - 1;
    
    private static final byte[][][] pairs = generatePairs();
    
    public static byte[] permutationPairs(int numDigits, int[] groupSize) {
        int groupsMask = groupsMask(numDigits, groupSize);
        return pairs[numDigits][groupsMask];
    }
    
    private static byte[][][] generatePairs() {
        byte[][][]  result = new byte[10][256][];
        for (int numDigits = 1; numDigits <= 9; numDigits++) {
            for (int groupsMask = 0; groupsMask < (1 << (numDigits-1)); groupsMask++) {
                result[numDigits][groupsMask] = generatePairs(numDigits, groupsMask);
            }
        }
        return result;
    }

    private static byte[] generatePairs(int numDigits, int groupsMask) {
        if (numDigits < 1 || numDigits > 9) throw new IllegalArgumentException();
        if (groupsMask < 0 || groupsMask >= (1<<(numDigits-1))) throw new IllegalArgumentException();
        
        int[] digits = new int[numDigits];
        for (int d = 0; d < numDigits; d++) {
            digits[d] = d;
        }
        int[] groupSize = groupSizes(numDigits, groupsMask);
        
        List<Integer> pairs = new ArrayList<>();
        generatePairs(numDigits, groupSize, 0, 0, 0, (pair) -> {
            pairs.add(pair);
        });
        int numPermutations = pairs.size();
        
        byte[] result = new byte[numPermutations];
        for (int i = 0; i < numPermutations; i++) {
            result[i] = pairs.get(i).byteValue();
        }
        return result;
    }
    
    private static int[] groupSizes(int numDigits, int groupsMask) {
        int[] groupSize = new int[9];
        int numGroups = 0;
        for (int d = 0; d < numDigits; d++) {
            groupSize[numGroups]++;
            if (((groupsMask >> d) & 1) == 0) {
                numGroups++;
            }
        }
        return Arrays.copyOf(groupSize, numGroups);
    }

    private static int groupsMask(int numDigits, int[] groupSize) {
        int result = 0;
        int digit = 0;
        for (int group = 0; group < groupSize.length && numDigits > 0; group++) {
            int size = groupSize[group];
            numDigits -= size;
            for (int i = 0; i < size-1; i++) {
                result |= (1 << digit++);
            }
            result |= (0 << digit++);
        }
        return result;
    }
    
    private static void generatePairs(int numDigits, int[] groupSize, int nextDigit, int nextGroup, int result, 
            Consumer<Integer> callback) {
        if (nextDigit == numDigits) {
            callback.accept(result);
            return;
        }

        int n = groupSize[nextGroup];
        generatePairs(numDigits, groupSize, nextDigit+n, nextGroup+1, result, callback);
        if (n == 1) {
            return;
        }
        if (n == 2) {
            int pair = pair(nextDigit, nextDigit+1);
            generatePairs(numDigits, groupSize, nextDigit+n, nextGroup+1, pair, callback);
            return;
        }

        int[] c = new int[n];
        int i = 0;
        while (i < n) {
            if (c[i] < i) {
                int j = (i%2 == 0) ? 0 : c[i];
                int pair = pair(nextDigit+i, nextDigit+j);
                generatePairs(numDigits, groupSize, nextDigit+n, nextGroup+1, pair, callback);
                c[i]++;
                i = 0;
            } else {
                c[i] = 0;
                i++;
            }
        }
    }

    private static int pair(int d1, int d2) {
        return (d1 << 4) | d2;
    }

    public static void main(String[] args) {
        for (int numDigits = 1; numDigits <= 4; numDigits++) {
            for (int groupsMask = 0; groupsMask < (1 << (numDigits-1)); groupsMask++) {
                System.out.println();
                
                int[] digits = new int[numDigits];
                for (int d = 0; d < numDigits; d++) {
                    digits[d] = d;
                }
                
                int[] groupSizes = groupSizes(numDigits, groupsMask);
                System.out.println("numDigits = " + numDigits + ", groupSizes = " + Arrays.toString(groupSizes));
                
                for (byte pair : pairs[numDigits][groupsMask]) {
                    int d1 = (pair >> 4) & MASK_4;
                    int d2 = pair & MASK_4;
                    int t = digits[d1]; digits[d1] = digits[d2]; digits[d2] = t;
                    System.out.println("{"+d1+", "+d2+"} " + Arrays.toString(digits));
                }
            }
        }
    
    }
    
}
