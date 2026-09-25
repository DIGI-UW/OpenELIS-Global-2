const sharedKeys = {
  baudRate: "serial.config.baudRate.label",
  dataBits: "serial.config.dataBits.label",
  stopBits: "serial.config.stopBits.label",
  parity: "serial.config.parity.label",
  flowControl: "serial.config.flowControl.label",
  "valueType.number": "analyzerType.editor.inputKind.NUMBER",
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
