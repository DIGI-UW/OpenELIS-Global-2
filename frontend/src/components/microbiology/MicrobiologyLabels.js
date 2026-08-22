export const formatMicrobiologyEnum = (value) => {
  if (!value) {
    return "";
  }

  const acronymLabels = {
    ast: "AST",
    id: "ID",
    mic: "MIC",
    qc: "QC",
    tb: "TB",
    whonet: "WHONET",
  };

  return String(value)
    .toLowerCase()
    .split("_")
    .map(
      (word) =>
        acronymLabels[word] || word.charAt(0).toUpperCase() + word.slice(1),
    )
    .join(" ");
};

export const formatCulturePurpose = (intl, value) =>
  intl.formatMessage({
    id:
      value === "CLINICAL_DIAGNOSTIC"
        ? "microbiology.culturePurpose.clinical"
        : value === "ACTIVE_SCREENING"
          ? "microbiology.culturePurpose.screening"
          : "microbiology.culturePurpose.unspecified",
  });
