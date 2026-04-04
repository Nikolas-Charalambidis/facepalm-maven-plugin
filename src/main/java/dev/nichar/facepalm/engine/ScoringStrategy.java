package dev.nichar.facepalm.engine;

/**
 * Defines the mathematical strategies for merging raw Risk and Confidence scores.
 * Since findings are discovered with a base Risk (how bad it is) and Confidence (how likely it's real),
 * these strategies determine the final "Threat Level" that triggers build failures or alerts.
 */
public enum ScoringStrategy {

    /**
     * Standard arithmetic mean. Balanced and predictable.
     */
    AVERAGE {
        public double calculate(int r, int c) {
            return (r + c) / 2.0;
        }
    },

    /**
     * Geometric mean. Penalizes findings where one value is very low.
     * Often used to ensure a high score requires *both* high risk and high confidence.
     */
    GEOMETRIC {
        public double calculate(int r, int c) {
            return Math.sqrt(r * (double) c);
        }
    },

    /**
     * Root Mean Square (Quadratic Mean).
     * Favors higher values; a very high Risk will pull the average up more than a low Confidence pulls it down.
     */
    ROOT_MEAN_SQUARE {
        public double calculate(int r, int c) {
            return Math.sqrt((r * r + c * c) / 2.0);
        }
    },

    /**
     * Heavily weights the square of the Risk score.
     * Designed for environments where extreme risks must be surfaced regardless of confidence.
     */
    WEIGHTED_QUADRATIC {
        public double calculate(int r, int c) {
            double rScaled = (r * r) / 100.0;
            return (rScaled * 0.8) + (c * 0.2);
        }
    },

    /**
     * Strict logic: If Risk is critical (>90), it ignores the average and takes the maximum value.
     * Otherwise, it defaults to a standard 50/50 split.
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
     * Calculates the final composite score based on the chosen strategy.
     *
     * @param risk The raw risk score (0-100).
     * @param confidence The raw confidence score (0-100).
     * @return The calculated threat level.
     */
    public abstract double calculate(int risk, int confidence);
}
