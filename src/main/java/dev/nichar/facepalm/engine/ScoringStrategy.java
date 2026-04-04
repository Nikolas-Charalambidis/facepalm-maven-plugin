package dev.nichar.facepalm.engine;

/**
 * Mathematical strategies for merging Risk and Confidence scores into a final Threat Level.
 */
public enum ScoringStrategy {

    /**
     * Standard arithmetic mean for a balanced and predictable score.
     */
    AVERAGE {
        public double calculate(int r, int c) {
            return (r + c) / 2.0;
        }
    },

    /**
     * Geometric mean that discounts findings with one very low value.
     */
    GEOMETRIC {
        public double calculate(int r, int c) {
            return Math.sqrt(r * (double) c);
        }
    },

    /**
     * Root Mean Square (Quadratic Mean) that favors higher values.
     */
    ROOT_MEAN_SQUARE {
        public double calculate(int r, int c) {
            return Math.sqrt((r * r + c * c) / 2.0);
        }
    },

    /**
     * Weights the square of the Risk score to surface extreme risks regardless of confidence.
     */
    WEIGHTED_QUADRATIC {
        public double calculate(int r, int c) {
            double rScaled = (r * r) / 100.0;
            return (rScaled * 0.8) + (c * 0.2);
        }
    },

    /**
     * Uses the maximum value if Risk is critical (>90); otherwise defaults to a 50/50 split.
     */
    GATEKEEPER {
        public double calculate(int r, int c) {
            if (r >= 90) {
                return Math.max(r, c);
            }
            return (r * 0.5) + (c * 0.5);
        }
    };

    /**
     * Calculates the final composite score based on the strategy.
     */
    public abstract double calculate(int risk, int confidence);
}
