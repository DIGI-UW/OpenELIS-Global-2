export const includesComboBoxText = ({ item, itemToString, inputValue }) => {
  const query = (inputValue || "").trim().toLowerCase();
  if (!query) {
    return true;
  }

  return (itemToString?.(item) || "").toLowerCase().includes(query);
};
