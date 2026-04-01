/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094.key;

import static oeis.a136094.key.Permutations.permutations;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PermutationsTest {

    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 3, 4, 5, 6, 7, 8, 9 })
    void shouldGenerateValidPermutations(int n) {
        int[] sortedDigits = IntStream.range(0, n).toArray();
        int[][] permutations = permutations(n);
        
        long uniqueCount = Arrays.stream(permutations)
                .map(Arrays::toString)
                .distinct()
                .count();
        
        assertThat(permutations).hasDimensions(factorial(n), n);
        assertThat(uniqueCount).isEqualTo(factorial(n));
        assertThat(Arrays.stream(permutations))
                .allSatisfy(values -> assertThat(sorted(values)).isEqualTo(sortedDigits));
    }
    
    @Test
    void shouldGeneratePermutationsWithinDigitGroups() {
        assertThat(permutations(4, new int[] { 1, 1, 1, 1 })).isEqualTo(
                new int[][] { 
                    { 0, 1, 2, 3 } 
                }
        );
        assertThat(permutations(4, new int[] { 2, 1, 1 })).isEqualTo(
                new int[][] { 
                    { 0, 1, 2, 3 },
                    { 1, 0, 2, 3 } 
                }
        );
        assertThat(permutations(4, new int[] { 1, 2, 1 })).isEqualTo(
                new int[][] { 
                    { 0, 1, 2, 3 },
                    { 0, 2, 1, 3 } 
                }
        );
        assertThat(permutations(4, new int[] { 1, 1, 2 })).isEqualTo(
                new int[][] { 
                    { 0, 1, 2, 3 },
                    { 0, 1, 3, 2 } 
                }
        );
        assertThat(permutations(4, new int[] { 2, 2 })).isEqualTo(
                new int[][] { 
                    { 0, 1, 2, 3 },
                    { 0, 1, 3, 2 }, 
                    { 1, 0, 3, 2 },
                    { 1, 0, 2, 3 }
                }
        );
        assertThat(permutations(4, new int[] { 3, 1 })).isEqualTo(
                new int[][] { 
                    { 0, 1, 2, 3 }, 
                    { 1, 0, 2, 3 }, 
                    { 2, 0, 1, 3 }, 
                    { 0, 2, 1, 3 }, 
                    { 1, 2, 0, 3 }, 
                    { 2, 1, 0, 3 } 
                }
        );
        assertThat(permutations(4, new int[] { 1, 3})).isEqualTo(
                new int[][] { 
                    { 0, 1, 2, 3 },
                    { 0, 2, 1, 3 },
                    { 0, 3, 1, 2 },
                    { 0, 1, 3, 2 },
                    { 0, 2, 3, 1 },
                    { 0, 3, 2, 1 }
                }
        );
    }
    
    private static int factorial(int n) {
        return IntStream.rangeClosed(1, n).reduce(1, (a, b) -> a * b);
    }

    private static int[] sorted(int[] array) {
        return Arrays.stream(array).sorted().toArray();
    }
    
}
