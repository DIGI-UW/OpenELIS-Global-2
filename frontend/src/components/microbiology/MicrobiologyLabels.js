export const formatMicrobiologyEnum = (value, intl) => {
  if (!value) {
    return "";
  }

  // i18n-keys: microbiology.enum.*
  const messageId = `microbiology.enum.${String(value)}`;
  return intl.formatMessage({ id: messageId, defaultMessage: String(value) });
};
export const formatMicrobiologyActivityNote = (activity, intl) => {
  if (
    activity?.activityType === "CULTURE_PROTOCOL_CHANGED" &&
    activity.structuredData
  ) {
    try {
      const details = JSON.parse(activity.structuredData);
      if (details.toMethodName && details.reason) {
        return intl.formatMessage(
          { id: "microbiology.case.timeline.protocolChanged" },
          {
            from:
              details.fromMethodName ||
              intl.formatMessage({ id: "microbiology.protocol.noneTitle" }),
            to: details.toMethodName,
            reason: details.reason,
          },
        );
      }
    } catch (error) {
      // Historical malformed JSON falls back to the stored legacy note.
    }
  }
  return activity?.note || "";
};

export const formatCulturePurpose = (intl, value) =>
  intl.formatMessage({
    id: ["CLINICAL_DIAGNOSTIC", "DIAGNOSTIC"].includes(value)
      ? "microbiology.culturePurpose.clinical"
      : ["ACTIVE_SCREENING", "SCREENING"].includes(value)
        ? "microbiology.culturePurpose.screening"
        : "microbiology.culturePurpose.unspecified",
  });
