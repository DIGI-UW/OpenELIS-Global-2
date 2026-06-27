export const formatMicrobiologyEnum = (value) => {
  if (!value) {
    return "";
  }

  const acronymLabels = {
    ast: "AST",
    id: "ID",
    mic: "MIC",
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
