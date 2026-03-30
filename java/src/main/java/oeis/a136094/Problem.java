/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094;

import oeis.a136094.key.Key;
import oeis.a136094.util.LogUtils;

public class Problem {

    private final Bundle[] bundles;
    private final Key key;
    private volatile String answer;
    private volatile long solveDuration;

    public Problem(Bundle... bundles) {
        this(bundles, null);
    }
    
    public Problem(Bundle[] bundles, Key key) {
        this.bundles = bundles;
        this.key = key;
    }

    public Bundle[] getBundles() {
        return bundles;
    }

    public Key getKey() {
        return key;
    }

    public void setAnswer(String answer, long solveDuration) {
        this.answer = answer;
        this.solveDuration = solveDuration;
    }

    public boolean hasAnswer() {
        return answer != null;
    }
    
    public String getAnswer() {
        return answer;
    }
    
    public void printResult(String suffix) {
        System.out.println(LogUtils.formatLog("%s => %s [%d], %d ms%s", 
                Bundle.bundlesToString(bundles), answer, answer != null ? answer.length() : 0, solveDuration, suffix));
    }
    
}