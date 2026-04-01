/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094.move;

import java.util.Arrays;

import oeis.a136094.Bundle;

public class MoveBuilder {
    
    private final Bundle[] newBundles = new Bundle[1024];
    
    // cacheMoveResult[index] is the result of moves cacheMove[0..index] applied to cacheBundles0
    private Bundle[] cacheBundles0;
    private final char[] cacheMove = new char[100];
    private final Bundle[][] cacheMoveResult = new Bundle[100][];

    public Bundle[] makeMovesAndSort(Bundle[] bundles, String moves) {
        Bundle[] nextBundles = makeMoves(bundles, moves);
        return Bundle.sortBundles(nextBundles);
    }

    private Bundle[] makeMoves(Bundle[] bundles, String moves) {
        int index = 0;
        Bundle[] bundles0 = bundles;

        if (cacheBundles0 != null && Arrays.equals(cacheBundles0, bundles0)) {
            while (index < moves.length() && cacheMove[index] == moves.charAt(index)) {
                bundles = cacheMoveResult[index];
                index++;
            }
        } else {
            cacheBundles0 = bundles0;
        }

        while (index < moves.length()) {
            int move = moves.charAt(index) - '1';
            bundles = makeMove(bundles, move);

            cacheMove[index] = moves.charAt(index);
            cacheMoveResult[index] = bundles;

            index++;
        }

        cacheMove[index] = 0;
        cacheMoveResult[index] = null;

        return bundles;
    }

    public Bundle[] makeMovesAndSortWithoutCache(Bundle[] bundles, String moves) {
        for (int index = 0; index < moves.length(); index++) {
            int move = moves.charAt(index) - '1';
            bundles = makeMove(bundles, move);
        }
        return Bundle.sortBundles(bundles);
    }

    public Bundle[] makeMoveAndSort(Bundle[] bundles, int move) {
        Bundle[] nextBundles = makeMove(bundles, move);
        return Bundle.sortBundles(nextBundles);
    }
    
    // Example move:
    //   Input: 12/1234 = 1(234) 2(134)
    //   Move: 1
    //   Result: (234) 2(134) = 234/234 2/1234 (which after removing duplicate sequences becomes: 34/234 2/1234)
    //
    //   The result was simplified in the following way:
    //     (234) 2(134) = 2(34) 3(24) 4(23) 2(134) = 3(24) 4(23) 2(134) = 34/234 2/1234
    //     Or, step by step:
    //       1) (234) = 2(34) 3(24) 4(23) - Split all permutations into groups, each starting with a different digit.
    //       2) 2(34) 2(134) = 2(134) - Remove duplicate sequences.
    //       3) 3(24) 4(23) = 34/234 - Group pieces into bundles.
    //       4) 2(134) = 2/1234 - Group pieces into bundles.
    //
    private Bundle[] makeMove(Bundle[] bundles, int move) {
        int r = 0, len = bundles.length, moveMask = 1 << move;
        for (int i = 0; i < len; i++) {
            Bundle bundle = bundles[i];
            int heads = bundle.heads();
            int digits = bundle.digits();
            
            int remainingHeads = heads & ~moveMask;
            if (remainingHeads != 0) {
                newBundles[r++] = Bundle.of(remainingHeads, digits);
            }
        }
        next:
        for (int i = 0; i < len; i++) {
            Bundle bundle = bundles[i];
            int heads = bundle.heads();
            int digits = bundle.digits();
            
            if ((heads & moveMask) != 0) {
                digits = digits & ~moveMask;
                heads = digits;
                
                if (heads == 0) continue;

                // try to add new bundle: 1 x 12/1234 -> 2/1234 [234/234]<- this
                // the new bundle will have to be de-duplicated: 
                // it may either be contained in a tail: 1/12345 234/234 -> 1/12345
                // or it may share both heads and digits: 2/1234 234/234 -> 2/1234 34/234
                for (int j = 0; j < r; j++) {
                    Bundle b = newBundles[j];
                    int h = b.heads();
                    int d = b.digits();
                    
                    if ((digits & ~d) != 0) continue; // de-duplicated bundle must not have other digits
    
                    // case 1: all de-duplicated digits are contained in a tail
                    if ((h & ~digits) != 0) continue next; // 1/12345 234/234 -> 1/12345
    
                    // case 2: shared heads
                    heads = heads & ~h; // 2/1234 234/234 -> 2/1234 34/234
                    
                    if (heads == 0) continue next;
                }

                newBundles[r++] = Bundle.of(heads, digits);
            }
        }
        return Arrays.copyOf(newBundles, r);
    }

}