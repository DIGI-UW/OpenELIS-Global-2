import { foldForSearch, matchesSearch } from "../common/searchText";

/**
 * Carbon's ComboBox lists every item whatever is typed unless it is given a
 * filter. This keeps the items whose label contains the typed text, ignoring
 * case and accents. While the input still shows the selected item's label, the
 * whole list stays available, so reopening the picker offers every choice.
 */
export const filterByTypedLabel =
  (selectedLabel = "") =>
  ({ item, itemToString, inputValue }) => {
    const typed = foldForSearch(inputValue);
    if (!typed || typed === foldForSearch(selectedLabel)) {
      return true;
    }
    return matchesSearch(itemToString(item), typed);
  };
