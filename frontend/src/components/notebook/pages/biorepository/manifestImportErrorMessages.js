const interpolate = (template, values = {}) =>
  Object.entries(values).reduce((result, [key, value]) => {
    return result.replace(new RegExp(`\\{${key}\\}`, "g"), String(value));
  }, template);

const renderMessage = (formatMessage, id, defaultMessage, values) => {
  if (typeof formatMessage === "function") {
    return formatMessage({ id, defaultMessage }, values);
  }
  return interpolate(defaultMessage, values);
};

const extractSampleContext = (message) => {
  const match = String(message || "").match(/^Sample\s+'([^']+)'\s*:\s*(.+)$/i);
  if (!match) {
    return { sampleRef: null, detail: String(message || "").trim() };
  }

  return {
    sampleRef: match[1],
    detail: match[2].trim(),
  };
};

const withSampleContext = (sampleRef, detail, formatMessage) => {
  if (!sampleRef) {
    return detail;
  }

  return renderMessage(
    formatMessage,
    "biorepository.manifest.error.sampleContext",
    'Sample "{sampleRef}": {detail}',
    { sampleRef, detail },
  );
};

export const translateManifestImportMessage = (message, formatMessage) => {
  if (!message) {
    return "";
  }

  const { sampleRef, detail } = extractSampleContext(message);
  const normalized = detail.toLowerCase();

  if (normalized.startsWith("duplicate sample id in manifest:")) {
    const sampleId = detail.split(":").slice(1).join(":").trim();
    return withSampleContext(
      sampleRef,
      renderMessage(
        formatMessage,
        "biorepository.manifest.error.duplicateInManifest",
        'This file contains the Sample ID "{sampleId}" more than once. Keep only one row for each sample.',
        { sampleId },
      ),
      formatMessage,
    );
  }

  if (normalized.startsWith("sample id already exists:")) {
    const sampleId = detail.split(":").slice(1).join(":").trim();
    return withSampleContext(
      sampleRef,
      renderMessage(
        formatMessage,
        "biorepository.manifest.error.sampleIdExists",
        'Sample ID "{sampleId}" is already registered in the system.',
        { sampleId },
      ),
      formatMessage,
    );
  }

  if (normalized.startsWith("invalid sample type:")) {
    const sampleType = detail.split(":").slice(1).join(":").trim();
    return withSampleContext(
      sampleRef,
      renderMessage(
        formatMessage,
        "biorepository.manifest.error.invalidSampleTypeFriendly",
        'Sample type "{sampleType}" is not recognized. Please correct it or use an existing sample type.',
        { sampleType },
      ),
      formatMessage,
    );
  }

  if (normalized.includes("barcode is required")) {
    return withSampleContext(
      sampleRef,
      renderMessage(
        formatMessage,
        "biorepository.manifest.error.sampleIdRequiredFriendly",
        "Sample ID is required.",
      ),
      formatMessage,
    );
  }

  if (normalized.includes("sample type is required")) {
    return withSampleContext(
      sampleRef,
      renderMessage(
        formatMessage,
        "biorepository.manifest.error.sampleTypeRequiredFriendly",
        "Sample type is required.",
      ),
      formatMessage,
    );
  }

  if (normalized.includes("origin lab is required")) {
    return withSampleContext(
      sampleRef,
      renderMessage(
        formatMessage,
        "biorepository.manifest.error.originLabRequiredFriendly",
        "Origin lab is required.",
      ),
      formatMessage,
    );
  }

  if (normalized.includes("receipt date is required")) {
    return withSampleContext(
      sampleRef,
      renderMessage(
        formatMessage,
        "biorepository.manifest.error.receiptDateRequiredFriendly",
        "Receipt date is required.",
      ),
      formatMessage,
    );
  }

  if (normalized.includes("minimum storage temperature is required")) {
    return withSampleContext(
      sampleRef,
      renderMessage(
        formatMessage,
        "biorepository.manifest.error.minTempRequiredFriendly",
        "Minimum storage temperature is required.",
      ),
      formatMessage,
    );
  }

  if (normalized.includes("maximum storage temperature is required")) {
    return withSampleContext(
      sampleRef,
      renderMessage(
        formatMessage,
        "biorepository.manifest.error.maxTempRequiredFriendly",
        "Maximum storage temperature is required.",
      ),
      formatMessage,
    );
  }

  if (normalized.includes("maximum storage temperature must be greater than or equal to minimum storage temperature")) {
    return withSampleContext(
      sampleRef,
      renderMessage(
        formatMessage,
        "biorepository.manifest.error.tempRangeFriendly",
        "Maximum storage temperature must be greater than or equal to the minimum storage temperature.",
      ),
      formatMessage,
    );
  }

  if (normalized.includes("invalid receipt date format")) {
    return withSampleContext(
      sampleRef,
      renderMessage(
        formatMessage,
        "biorepository.manifest.error.invalidReceiptDateFriendly",
        "Receipt date is not in a supported format. Use yyyy-MM-dd or yyyy-MM-dd HH:mm:ss.",
      ),
      formatMessage,
    );
  }

  if (normalized.includes("invalid collection date format")) {
    return withSampleContext(
      sampleRef,
      renderMessage(
        formatMessage,
        "biorepository.manifest.error.invalidCollectionDateFriendly",
        "Collection date is not in a supported format. Use yyyy-MM-dd, dd/MM/yyyy, or dd-MM-yyyy.",
      ),
      formatMessage,
    );
  }

  if (normalized.includes("invalid biosafety level")) {
    return withSampleContext(
      sampleRef,
      renderMessage(
        formatMessage,
        "biorepository.manifest.error.invalidBiosafetyLevelFriendly",
        "Biosafety level is invalid. Use BSL_1, BSL_2, BSL_3, or BSL_4.",
      ),
      formatMessage,
    );
  }

  if (normalized.includes("user session not found") || normalized.includes("please log in again")) {
    return renderMessage(
      formatMessage,
      "biorepository.manifest.error.sessionExpiredFriendly",
      "Your session has expired. Please sign in again and retry the import.",
    );
  }

  if (normalized.includes("no samples provided")) {
    return renderMessage(
      formatMessage,
      "biorepository.manifest.error.noSamplesProvidedFriendly",
      "The selected file does not contain any samples to import.",
    );
  }

  if (normalized.includes("duplicate key value violates unique constraint")) {
    let friendlyDetail = renderMessage(
      formatMessage,
      "biorepository.manifest.error.duplicateRecordFriendly",
      "This row matches a record that already exists in the system.",
    );

    if (
      normalized.includes("sample_item")
      || normalized.includes("biosample")
      || normalized.includes("barcode")
      || normalized.includes("external_id")
    ) {
      friendlyDetail = renderMessage(
        formatMessage,
        "biorepository.manifest.error.duplicateSampleFriendly",
        "A sample with the same Sample ID already exists in the system.",
      );
    } else if (
      normalized.includes("type_of_sample")
      || normalized.includes("typeofsample")
      || normalized.includes("local_abbreviation")
    ) {
      friendlyDetail = renderMessage(
        formatMessage,
        "biorepository.manifest.error.duplicateSampleTypeFriendly",
        "This sample type already exists in the system. Please validate the file again and retry the import.",
      );
    }

    return withSampleContext(sampleRef, friendlyDetail, formatMessage);
  }

  if (
    normalized.includes("could not execute statement")
    || normalized.includes("constraint")
    || normalized.includes("batch entry")
    || normalized.includes("null value in column")
    || normalized.includes("invalid input syntax")
  ) {
    return withSampleContext(
      sampleRef,
      renderMessage(
        formatMessage,
        "biorepository.manifest.error.genericDataFriendly",
        "This row contains a value that could not be saved. Please review the row and try again.",
      ),
      formatMessage,
    );
  }

  if (normalized.startsWith("failed to register samples:")) {
    return renderMessage(
      formatMessage,
      "biorepository.manifest.error.failedToRegisterFriendly",
      "Some samples could not be imported. Please review the row errors and correct the file before trying again.",
    );
  }

  return withSampleContext(sampleRef, detail, formatMessage);
};

export const translateManifestImportMessages = (messages, formatMessage) =>
  (Array.isArray(messages) ? messages : []).map((message) =>
    translateManifestImportMessage(message, formatMessage),
  );
