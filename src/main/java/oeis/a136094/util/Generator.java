/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094.util;

import java.util.function.Consumer;
import java.util.function.Function;

@FunctionalInterface
public interface Generator<T> {
 
    void generate(Consumer<T> sink);

    default <R> Generator<R> transforming(Function<T, R> transform) {
        return (sink) -> {
            this.generate((item) -> {
                R result = transform.apply(item);
                sink.accept(result);
            });
        };
    }
    
}