/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094.key;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class Permutations {

    private static final int[][][][] permutations = generatePermutations();
    
    public static int[][] permutations(int numDigits, int[] groupSize) {
        int groupsMask = groupsMask(numDigits, groupSize);
        return permutations[numDigits][groupsMask];
    }
    
    private static int[][][][] generatePermutations() {
        int[][][][]  result = new int[10][256][][];
        for (int numDigits = 1; numDigits <= 9; numDigits++) {
            for (int groupsMask = 0; groupsMask < (1 << (numDigits-1)); groupsMask++) {
                result[numDigits][groupsMask] = generatePermutations(numDigits, groupsMask);
            }
        }
        return result;
    }

    private static int[][] generatePermutations(int numDigits, int groupsMask) {
        if (numDigits < 1 || numDigits > 9) throw new IllegalArgumentException();
        if (groupsMask < 0 || groupsMask >= (1<<(numDigits-1))) throw new IllegalArgumentException();
        
        int[] digits = new int[numDigits];
        for (int d = 0; d < numDigits; d++) {
            digits[d] = d;
        }
        int[] groupSize = groupSizes(numDigits, groupsMask);
        
        List<int[]> permutations = new ArrayList<>();
        generatePermutations(digits, groupSize, 0, 0, (permutation) -> {
            permutations.add(Arrays.copyOf(permutation, permutation.length));
        });
        int numPermutations = permutations.size();
        
        int[][] result = new int[numPermutations][];
        for (int i = 0; i < numPermutations; i++) {
            result[i] = permutations.get(i);
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
    
    private static void generatePermutations(int[] digits, int[] groupSize, int nextDigit, int nextGroup, 
            Consumer<int[]> callback) {
        if (nextDigit == digits.length) {
            callback.accept(digits);
            return;
        }

        int n = groupSize[nextGroup];
        generatePermutations(digits, groupSize, nextDigit+n, nextGroup+1, callback);
        if (n == 1) {
            return;
        }
        if (n == 2) {
            int t = digits[nextDigit]; digits[nextDigit] = digits[nextDigit+1]; digits[nextDigit+1] = t;
            generatePermutations(digits, groupSize, nextDigit+n, nextGroup+1, callback);
            return;
        }

        int[] c = new int[n];
        int i = 0;
        while (i < n) {
            if (c[i] < i) {
                int j = (i%2 == 0) ? 0 : c[i];
                int t = digits[nextDigit+i]; digits[nextDigit+i] = digits[nextDigit+j]; digits[nextDigit+j] = t;
                generatePermutations(digits, groupSize, nextDigit+n, nextGroup+1, callback);
                c[i]++;
                i = 0;
            } else {
                c[i] = 0;
                i++;
            }
        }
    }

    public static void main(String[] args) {
        for (int numDigits = 1; numDigits <= 4; numDigits++) {
            for (int groupsMask = 0; groupsMask < (1 << (numDigits-1)); groupsMask++) {
                System.out.println();
                
                int[] groupSizes = groupSizes(numDigits, groupsMask);
                System.out.println("numDigits = " + numDigits + ", groupSizes = " + Arrays.toString(groupSizes));
                
                int[][] perms = permutations[numDigits][groupsMask];
                for (int[] perm : perms) {
                    System.out.println(Arrays.toString(perm));
                }
            }
        }
    
    }
    
}
