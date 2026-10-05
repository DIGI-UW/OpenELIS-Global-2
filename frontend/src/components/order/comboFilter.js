/**
 * Carbon's ComboBox lists every item whatever is typed unless it is given a
 * filter. This keeps the items whose label contains the typed text, ignoring
 * case. While the input still shows the selected item's label, the whole list
 * stays available, so reopening the picker offers every choice.
 */
export const filterByTypedLabel =
  (selectedLabel = "") =>
  ({ item, itemToString, inputValue }) => {
    const typed = (inputValue || "").trim().toLowerCase();
    if (!typed || typed === (selectedLabel || "").trim().toLowerCase()) {
      return true;
    }
    return (itemToString(item) || "").toLowerCase().includes(typed);
  };
