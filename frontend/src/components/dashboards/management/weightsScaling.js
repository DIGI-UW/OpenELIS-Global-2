const round2 = (value) => Math.round(value * 100) / 100;

// The rounding remainder goes to the largest weight.
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
