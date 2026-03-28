/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094.key;

import static java.lang.Integer.bitCount;
import static oeis.a136094.util.Utils.MASK_9;

import oeis.a136094.Bundle;

public class KeyUtils {

    // groupSize divides the digits [1, 2, 3, 4, 5, 6, 7, 8, 9] into 9 groups. 
    // Each groupSize[i] value describes how many digits are in the i-th group.
    //
    // The result will be the mask consisting of 9 zero bits separated with one bits according 
    // to the groups described by groupSize array.
    // 
    // The optional swap array will map the digits of the bundles into consecutive digits 
    // in the corresponding groups.
    // 
    // For example:
    //   digits: [1, 2, 3, 4, 5, 6, 7, 8, 9] 
    //   groupSize: [3, 0, 1, 2, 0, 0, 2, 1, 0] 
    //   groups: [{1, 2, 3}, {}, {4}, {5, 6}, {}, {}, {7, 8}, {9}, {}].
    //   groupStartIndex: [0, 3, 3, 4, 6, 6, 6, 8, 9]
    //   result: 00011010011100101
    //
    // The following should be true: groupStartIndex[i] + groupSize[i] = groupStartIndex[i+1]
    // The following should be true: sum(groupSize) = 9
    // 
    // The result will always have 8 ones and 9 zero bits, so 17 bits total.
    //
    public static int K2(Bundle bundle1, Bundle bundle2, int[] swap, int[] groupSize, int[] groupStartIndex) {
        int h1 = bundle1.heads();
        int h2 = bundle2.heads();
        int d1 = bundle1.digits();
        int d2 = bundle2.digits();
        int t1 = d1 & ~h1;
        int t2 = d2 & ~h2;

        groupSize[0] = bitCount(h1 & h2);
        groupSize[1] = bitCount(h1 & t2);
        groupSize[2] = bitCount(h1 & ~d2);

        groupSize[3] = bitCount(t1 & h2);
        groupSize[4] = bitCount(t1 & t2);
        groupSize[5] = bitCount(t1 & ~d2);

        groupSize[6] = bitCount(~d1 & h2);
        groupSize[7] = bitCount(~d1 & t2);
        groupSize[8] = bitCount(~d1 & ~d2 & MASK_9);

        // Effectively, swap bundle1 and bundle2 if needed, to always return minimum value 
        // between K2(bundle1, bundle2) and K2(bundle2, bundle1).
        // 
        //       h2 t2 ~d2
        //    h1  0  1  2
        //    t1  3  4  5
        //   ~d1  6  7  8
        //
        if (((groupSize[1] << 16) | (groupSize[2] << 8) | groupSize[5]) 
                < ((groupSize[3] << 16) | (groupSize[6] << 8) | groupSize[7])) {
            int t;
            t = d1; d1 = d2; d2 = t;
            t = h1; h1 = h2; h2 = t;
            t = groupSize[1]; groupSize[1] = groupSize[3]; groupSize[3] = t; 
            t = groupSize[2]; groupSize[2] = groupSize[6]; groupSize[6] = t; 
            t = groupSize[5]; groupSize[5] = groupSize[7]; groupSize[7] = t; 
        }
        
        int result = 0;
        for (int i = 1; i < 9; i++) {
            int c = groupSize[i];
            // append bit '1' followed by groupSize[i] of '0' bits
            result = (result << (c+1)) | (1 << c); 
        }

        if (swap != null) {
            int lastIndex = 0;
            for (int i = 0; i < 9; i++) {
                groupStartIndex[i] = lastIndex;
                lastIndex += groupSize[i];
            }
            
            for (int d = 0; d < 9; d++) {
                int mask = 1 << d;
                int groupIndex;
                if ((d1 & mask) != 0) {
                    if ((h1 & mask) != 0) {
                        if ((d2 & mask) != 0) {
                            groupIndex = (h2 & mask) != 0 ? 0 : 1; 
                        } else {
                            groupIndex = 2;
                        }
                    } else {
                        if ((d2 & mask) != 0) {
                            groupIndex = (h2 & mask) != 0 ? 3 : 4;
                        } else {
                            groupIndex = 5;
                        }
                    }
                } else {
                    if ((d2 & mask) != 0) {
                        groupIndex = (h2 & mask) != 0 ? 6 : 7;
                    } else {
                        groupIndex = 8;
                    }
                }
                swap[d] = groupStartIndex[groupIndex]++;
            }
        }
        
        return result; // 17 bit
    }
    
}
