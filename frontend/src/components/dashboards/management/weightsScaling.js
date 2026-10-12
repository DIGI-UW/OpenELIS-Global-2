const round2 = (value) => Math.round(value * 100) / 100;

/**
 * Weights scaled proportionally to sum to 1.00 at two decimals, the rounding
 * remainder going to the largest weight. Null when nothing can be scaled.
 */
export const scaleToOne = (weights) => {
  const keys = Object.keys(weights);
  const total = keys.reduce((sum, key) => sum + weights[key], 0);
  if (!(total > 0)) {
    return null;
  }
  const scaled = Object.fromEntries(
    keys.map((key) => [key, round2(weights[key] / total)]),
  );
  const largest = keys.reduce((a, b) => (scaled[b] > scaled[a] ? b : a));
  const remainder = 1 - keys.reduce((sum, key) => sum + scaled[key], 0);
  scaled[largest] = round2(scaled[largest] + remainder);
  return scaled;
};
