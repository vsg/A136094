/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094.util;

import java.util.function.IntBinaryOperator;

public class BitUtils {

    public static final int MASK_18 = (1 << 18) - 1;
    public static final int MASK_9 = (1 << 9) - 1;
    public static final int MASK_4 = (1 << 4) - 1;
    
    public static final IntBinaryOperator BITWISE_OR = (a, b) -> a | b;

    public static String digitsMaskToString(int mask) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 9; i++) {
            if (((mask >> i) & 1) != 0) {
                builder.append((char)('1' + i));
            }
        }
        return builder.toString();
    }

}
