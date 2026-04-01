/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094.solver;

import oeis.a136094.Bundle;
import oeis.a136094.Problem;
import oeis.a136094.partials.Partials;

public abstract class Solver {

    protected Partials partials;

    public Solver(Partials partials) {
        this.partials = partials;
    }

    public abstract String solve(Bundle[] bundles, int bestAnsLen);

    public void solve(Problem problem) {
        String answer = null;
        long begin = System.currentTimeMillis();
        for (int len = 1; len <= 100; len++) {
            answer = solve(problem.getBundles(), len);
            if (answer != null) {
                break;
            }
        }
        long end = System.currentTimeMillis();
        if (answer == null) {
            throw new IllegalStateException();
        }
        problem.setAnswer(answer, end - begin);
    }

    public static boolean isMultiThreadedAlg(String algName) {
        return switch (algName) {
        case "none" -> true;
        case "bfs" -> false;
        case "bfs-batch" -> true;
        case "dfs" -> false;
        case "dfs-batch" -> true;
        case "dfs-disk" -> true;
        case "dfs-swarm" -> true;
        default -> throw new RuntimeException("Unknown algorithm: " + algName);
        };
    }

    public static Solver createSolver(String algName, Partials partials) {
        return switch (algName) {
        case "none" -> null;
        case "bfs" -> new BFSSolver(partials);
        case "bfs-batch" -> new BFSBatchSolver(partials);
        case "dfs" -> new DFSSolver(partials);
        case "dfs-batch" -> new DFSBatchSolver(partials);
        case "dfs-disk" -> new DFSDiskSolver(partials);
        case "dfs-swarm" -> new DFSSwarmSolver(partials);
        default -> throw new RuntimeException("Unknown algorithm: " + algName);
        };
    }
    
}