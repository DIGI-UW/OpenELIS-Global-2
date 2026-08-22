const specimenId = (intl) =>
  intl.formatMessage({
    id: "analyzerType.recognition.source.specimenId",
  });

export const formatRecognitionCondition = (intl, condition = {}) => {
  const source =
    condition.kind === "SPECIMEN_ID_STARTS_WITH" ||
    condition.kind === "CONFIGURED_SPECIMEN_ID_PATTERN"
      ? specimenId(intl)
      : condition.sourceLabel;

  switch (condition.kind) {
    case "SPECIMEN_ID_STARTS_WITH":
      return intl.formatMessage(
        { id: "analyzerType.recognition.condition.summary.startsWith" },
        { source, value: condition.value },
      );
    case "CONFIGURED_SPECIMEN_ID_PATTERN":
      return intl.formatMessage({
        id: "analyzerType.recognition.condition.summary.configuredPattern",
      });
    case "FIELD_VALUE_EQUALS":
      return intl.formatMessage(
        { id: "analyzerType.recognition.condition.summary.equals" },
        { source, value: condition.value },
      );
    case "FIELD_VALUE_CONTAINS":
      return intl.formatMessage(
        { id: "analyzerType.recognition.condition.summary.contains" },
        { source, value: condition.value },
      );
    default:
      return intl.formatMessage({
        id: "analyzerType.recognition.condition.summary.unknown",
      });
  }
};

export const formatRecognitionMode = (intl, mode) =>
  intl.formatMessage({
    id:
      mode === "RULES"
        ? "analyzerType.recognition.mode.rules.short"
        : mode === "NONE"
          ? "analyzerType.recognition.mode.none.short"
          : "analyzerType.recognition.condition.summary.unknown",
  });
