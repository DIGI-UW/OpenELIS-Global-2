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
