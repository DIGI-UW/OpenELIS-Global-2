const sharedKeys = {
  yes: "label.yes",
  no: "label.no",
  name: "analyzerType.field.profileName",
  manufacturer: "analyzerType.field.manufacturer",
  model: "analyzerType.field.model",
  protocol: "analyzerType.field.protocol",
  "semantic.result": "common.result",
  "semantic.units": "label.validation.review.units",
  "semantic.interpretation": "analyzer.testMapping.parsedFields.interpretation",
  "semantic.testDate": "label.results.testDate",
  "semantic.position": "common.position",
};

export const profileAuthoringMessage = (intl, key, values) =>
  // i18n-keys: analyzerType.editor.*
  intl.formatMessage(
    { id: sharedKeys[key] || `analyzerType.editor.${key}` },
    values,
  );
