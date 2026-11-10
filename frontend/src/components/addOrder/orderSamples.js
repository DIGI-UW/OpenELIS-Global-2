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

/**
 * The warning for a sample that has a sample type but no test, naming the sample
 * as the Result Reporting headings do ("Sample 1: Serum") when its type name is
 * known, and by its number alone otherwise.
 */
export function missingTestsTitle(intl, samples, sampleNumber, typeNamesById) {
  const typeId = (samples || [])[sampleNumber - 1]?.sampleTypeId;
  const sampleType = typeId ? (typeNamesById || {})[String(typeId)] : undefined;
  return sampleType
    ? intl.formatMessage(
        { id: "order.sample.missingTests.named" },
        { sampleNumber, sampleType },
      )
    : intl.formatMessage({ id: "order.sample.missingTests" }, { sampleNumber });
}
