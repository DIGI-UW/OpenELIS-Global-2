const existingMessageIds = {
  BACTERIOLOGY: "label.testCatalog.basicInfo.cultureWorkflowType.BACTERIOLOGY",
  MYCOBACTERIOLOGY_TB:
    "label.testCatalog.basicInfo.cultureWorkflowType.MYCOBACTERIOLOGY_TB",
  MYCOLOGY: "label.testCatalog.basicInfo.cultureWorkflowType.MYCOLOGY",
};

export const formatMicrobiologyEnum = (value, intl) => {
  if (!value) {
    return "";
  }

  const messageId =
    existingMessageIds[value] || `microbiology.enum.${String(value)}`;
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
