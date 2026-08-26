export const includesComboBoxText = ({ item, itemToString, inputValue }) => {
  const query = (inputValue || "").trim().toLocaleLowerCase();
  if (!query) {
    return true;
  }

  return (itemToString?.(item) || "").toLocaleLowerCase().includes(query);
};
