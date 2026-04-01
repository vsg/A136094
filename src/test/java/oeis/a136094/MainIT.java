/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import oeis.a136094.partials.Partials;
import oeis.a136094.partials.PartialsInit;
import oeis.a136094.solver.Solver;

class MainIT {

    private static final String[] EXPECTED_ANSWERS = new String[] {
            null, 
            "1", 
            "121", 
            "1213121", 
            "123412314213", 
            "1234512341523142351",
            "1234516234152361425312643512", 
            "123451672341526371425361274351263471253", 
            "1234156782315426738152643718265341278635124376812453", 
            "123456781923451678234915627348152963471825364912783546123976845123"};

    @Nested
    class FastTests {
        
        @ParameterizedTest
        @ValueSource(ints = { 1, 2, 3, 4, 5, 6 })
        void testBfs(int n) {
            assertThat(solveProblem(n, "bfs")).isEqualTo(EXPECTED_ANSWERS[n]);
        }

        @ParameterizedTest
        @ValueSource(ints = { 1, 2, 3, 4, 5, 6 })
        void testBfsBatch(int n) {
            assertThat(solveProblem(n, "bfs-batch")).isEqualTo(EXPECTED_ANSWERS[n]);
        }
        
        @ParameterizedTest
        @ValueSource(ints = { 1, 2, 3, 4, 5, 6 })
        void testDfs(int n) {
            assertThat(solveProblem(n, "dfs")).isEqualTo(EXPECTED_ANSWERS[n]);
        }
        
        @ParameterizedTest
        @ValueSource(ints = { 1, 2, 3, 4, 5, 6 })
        void testDfsBatch(int n) {
            assertThat(solveProblem(n, "dfs-batch")).isEqualTo(EXPECTED_ANSWERS[n]);
        }
        
        @ParameterizedTest
        @ValueSource(ints = { 1, 2, 3, 4, 5, 6 })
        void testDfsSwarm(int n) {
            assertThat(solveProblem(n, "dfs-swarm")).isEqualTo(EXPECTED_ANSWERS[n]);
        }
        
    }
    
    @Tag("slow")
    @Nested
    class SlowTests {
        
        private static Partials partials;
        
        @BeforeAll
        static void init() {
            partials = precalcPartials(7);
        }
        
        @ParameterizedTest
        @ValueSource(ints = { 7 })
        void testBfs(int n) {
            assertThat(solveProblem(n, "bfs", partials)).isEqualTo(EXPECTED_ANSWERS[n]);
        }

        @ParameterizedTest
        @ValueSource(ints = { 7 })
        void testBfsBatch(int n) {
            assertThat(solveProblem(n, "bfs-batch", partials)).isEqualTo(EXPECTED_ANSWERS[n]);
        }
        
        @ParameterizedTest
        @ValueSource(ints = { 7 })
        void testDfs(int n) {
            assertThat(solveProblem(n, "dfs", partials)).isEqualTo(EXPECTED_ANSWERS[n]);
        }
        
        @ParameterizedTest
        @ValueSource(ints = { 7 })
        void testDfsBatch(int n) {
            assertThat(solveProblem(n, "dfs-batch", partials)).isEqualTo(EXPECTED_ANSWERS[n]);
        }
        
        @ParameterizedTest
        @ValueSource(ints = { 7 })
        void testDfsSwarm(int n) {
            assertThat(solveProblem(n, "dfs-swarm", partials)).isEqualTo(EXPECTED_ANSWERS[n]);
        }
        
    }
    
    private static Partials precalcPartials(int n) {
        Main.MAX_N = n;
        Main.MIN_PIECE_CHECK_SIZE = Math.max(n-2, -1);
        Main.NO_SAVE_FILES = true;

        return PartialsInit.precalc();
    }
    
    private static String solveProblem(int n, String alg) {
        Partials partials = precalcPartials(n);
        
        return solveProblem(n, alg, partials);
    }

    private static String solveProblem(int n, String alg, Partials partials) {
        Solver solver = Solver.createSolver(alg, partials);
        int digits = (1 << n) - 1;
        Problem problem = new Problem(Bundle.unpack((digits << 9) | digits));
        solver.solve(problem);
        return problem.getAnswer();
    }
    
}
