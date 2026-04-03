package dev.nichar.facepalm.engine;

public enum ScoringStrategy {

    AVERAGE {
        public double calculate(int r, int c) {
            return (r + c) / 2.0;
        }
    },
    GEOMETRIC {
        public double calculate(int r, int c) {
            return Math.sqrt(r * (double) c);
        }
    },
    RMS {
        public double calculate(int r, int c) {
            return Math.sqrt((r * r + c * c) / 2.0);
        }
    },
    WEIGHTED_QUADRATIC {
        public double calculate(int r, int c) {
            double rScaled = (r * r) / 100.0;
            return (rScaled * 0.8) + (c * 0.2);
        }
    },
    GATEKEEPER {
        public double calculate(int r, int c) {
            if (r >= 90) {
                return Math.max(r, c);
            }
            return (r * 0.5) + (c * 0.5);
        }
    };

    public abstract double calculate(int risk, int confidence);
}
