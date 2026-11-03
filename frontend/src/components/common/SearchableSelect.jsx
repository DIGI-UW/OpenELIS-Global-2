import React, { useMemo } from "react";
import { ComboBox } from "@carbon/react";
import { foldForSearch, matchesSearch } from "./searchText";
import { RequiredMarker, requiredProps } from "./RequiredMarker";

/**
 * A single choice from a list that comes from site data (tests, labs, units,
 * categories), picked by typing part of its label (OGC-1515). The list narrows
 * to the options containing the typed text, ignoring case and accents, and
 * stays keyboard usable. While the box still shows the chosen label, the whole
 * list is offered again.
 *
 * Options are `{ id, value }` like {@link CustomSelect}; `onChange` receives
 * the chosen id, or "" when the choice is cleared. `required` marks the label
 * and tells assistive technology; `onBlur` fires when focus leaves the input.
 */
const SearchableSelect = ({
  id,
  labelText,
  options = [],
  value,
  onChange,
  placeholder,
  helperText,
  invalid,
  invalidText,
  disabled,
  size,
  className,
  required = false,
  onBlur,
}) => {
  const selectedItem = useMemo(
    () =>
      options.find((option) => String(option.id) === String(value ?? "")) ||
      null,
    [options, value],
  );
  const selectedLabel = selectedItem ? selectedItem.value : "";

  return (
    <ComboBox
      id={id}
      className={className}
      titleText={
        required ? (
          <>
            {labelText}
            <RequiredMarker />
          </>
        ) : (
          labelText
        )
      }
      {...requiredProps(required)}
      onBlur={onBlur}
      placeholder={placeholder}
      helperText={helperText}
      invalid={invalid}
      invalidText={invalidText}
      disabled={disabled}
      size={size}
      items={options}
      selectedItem={selectedItem}
      itemToString={(item) => (item ? String(item.value ?? "") : "")}
      shouldFilterItem={({ item, inputValue }) =>
        foldForSearch(inputValue) === foldForSearch(selectedLabel) ||
        matchesSearch(item?.value, inputValue)
      }
      onChange={({ selectedItem: chosen }) =>
        onChange(chosen ? String(chosen.id) : "")
      }
    />
  );
};

export default SearchableSelect;
