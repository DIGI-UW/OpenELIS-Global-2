import { matchesSearch } from "../common/searchText";

export const includesComboBoxText = ({ item, itemToString, inputValue }) =>
  matchesSearch(itemToString?.(item), inputValue);
