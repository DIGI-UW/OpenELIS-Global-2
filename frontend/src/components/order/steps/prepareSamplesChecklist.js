import { isPlausibleTemperature } from "./sections/handlingRules";

/**
 * The complete level of Prepare Samples (FR-A7, FR-D7): what the "To
 * continue" checklist lists before Save and next. A sample type on the order;
 * for every sample still live (typed, not rejected) a collection date and
 * time not before the admission date; and consent where the laboratory
 * requires it. The collector is optional (OGC-1419): many tubes arrive with no
 * name on them, so it never holds the step.
 */
export function prepareSamplesToContinue({
  samples = [],
  labNumber,
  consentSatisfied,
  intl,
}) {
  const sampleName = ({ sample, index }) =>
    `${labNumber || ""}-${index + 1} ${sample.sampleTypeName || ""}`.trim();
  const items = [];
  if (!samples.some((s) => s.sampleTypeId)) {
    items.push({
      id: "order.continue.item.sampleType",
      label: intl.formatMessage({ id: "order.continue.item.sampleType" }),
      targetId: "sampleType-0",
    });
  }
  samples
    .map((sample, index) => ({ sample, index }))
    .filter(({ sample }) => sample.sampleTypeId && !sample.sampleRejected)
    .forEach((entry) => {
      const { sample, index } = entry;
      if (!isPlausibleTemperature(sample.arrivalTemperature)) {
        items.push({
          id: `arrivalTemperature-${index}`,
          label: intl.formatMessage(
            { id: "order.continue.item.arrivalTemperature" },
            { sample: sampleName(entry) },
          ),
          targetId: `arrivalTemperature-${index}`,
        });
      }
      if (!sample.collectionDate || !sample.collectionTime) {
        items.push({
          id: `collectionTime-${index}`,
          label: intl.formatMessage(
            { id: "order.continue.item.collectionTime" },
            { sample: sampleName(entry) },
          ),
          targetId: `collectionDate-${index}`,
        });
      }
    });
  if (!consentSatisfied) {
    items.push({
      id: "order.continue.item.consent",
      label: intl.formatMessage({ id: "order.continue.item.consent" }),
      targetId: "consent-section",
    });
  }
  return items;
}
