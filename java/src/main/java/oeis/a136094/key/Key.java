/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094.key;

import java.io.Serializable;
import java.util.Arrays;

import oeis.a136094.Bundle;

@SuppressWarnings("serial")
public class Key implements Serializable {
    
    private final int[] values;
    private final int hashCode;

    public Key(int[] values) {
        this.values = values;
        this.hashCode = Arrays.hashCode(values);
    }

    public Bundle[] asBundles() {
        return Bundle.unpackAll(values);
    }
    
    public int size() {
        return values.length;
    }
    
    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Key other = (Key) obj;
        return hashCode == other.hashCode && Arrays.equals(values, other.values);
    }

    @Override
    public String toString() {
        return Bundle.bundlesToString(asBundles());
    }
    
}