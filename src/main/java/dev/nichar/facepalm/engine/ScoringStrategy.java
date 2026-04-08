/*
 * Licensed under Apache-2.0.
 * Copyright (c) 2026 Nikolas Charalambidis.
 * All rights reserved.
 */

package dev.nichar.facepalm.engine;

/**
 * Mathematical strategies for calculating a composite threat score from risk and confidence.
 */
public enum ScoringStrategy {

    /**
     * Arithmetic mean for a balanced, linear score.
     */
    AVERAGE {

        @Override
        public double calculate(final int r, final int c) {
            return (r + c) / 2.0;
        }
    },

    /**
     * Geometric mean that heavily discounts findings with low individual scores.
     */
    GEOMETRIC {

        @Override
        public double calculate(final int r, final int c) {
            return Math.sqrt(r * (double) c);
        }
    },

    /**
     * Quadratic mean that emphasizes higher values to surface critical findings.
     */
    ROOT_MEAN_SQUARE {

        @Override
        public double calculate(final int r, final int c) {
            return Math.sqrt((r * r + c * c) / 2.0);
        }
    },

    /**
     * Weights the square of the risk score to prioritize extreme threats.
     */
    WEIGHTED_QUADRATIC {

        @Override
        public double calculate(final int r, final int c) {
            final double rScaled = (r * r) / 100.0;
            return (rScaled * 0.8) + (c * 0.2);
        }
    },

    /**
     * Returns the maximum score for critical risks; otherwise applies a balanced split.
     */
    GATEKEEPER {

        @Override
        public double calculate(final int r, final int c) {
            if (r >= 90) {
                return Math.max(r, c);
            }
            return (r * 0.5) + (c * 0.5);
        }
    };

    /**
     * Computes the final composite score based on the implementation strategy.
     */
    public abstract double calculate(int risk, int confidence);
}
