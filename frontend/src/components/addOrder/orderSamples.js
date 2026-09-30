/**
 * The samples an order submits: every sample with at least one test, wherever
 * it sits in the list. A blank sample (no type, no tests) is left out.
 */
export function samplesWithTests(samples) {
  return (samples || []).filter((sample) => sample?.tests?.length > 0);
}

/**
 * The 1-based numbers, as the sample step shows them, of the samples that have
 * a sample type but no test. Such a sample would be dropped on save, so the
 * order must not be submitted until it gets a test or is removed.
 */
export function samplesMissingTests(samples) {
  return (samples || []).reduce((numbers, sample, index) => {
    if (sample?.sampleTypeId && !(sample?.tests?.length > 0)) {
      numbers.push(index + 1);
    }
    return numbers;
  }, []);
}
