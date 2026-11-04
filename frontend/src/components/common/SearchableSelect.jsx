import React, { useEffect, useMemo, useRef } from "react";
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
 * and tells assistive technology; `hideLabel` keeps the label for assistive
 * technology only (a table cell); `onBlur` fires when focus leaves the input.
 * A search left without a choice (Escape, or focus moving away) shows the
 * chosen label again, so the box never looks empty while a value is kept.
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
  hideLabel = false,
  onBlur,
}) => {
  const selectedItem = useMemo(
    () =>
      options.find((option) => String(option.id) === String(value ?? "")) ||
      null,
    [options, value],
  );
  const selectedLabel = selectedItem ? selectedItem.value : "";
  const actions = useRef(null);
  const shownLabel = useRef(selectedLabel);
  shownLabel.current = selectedLabel;

  const restoreChosenLabel = () =>
    setTimeout(() => {
      const input = document.getElementById(id);
      if (input && input.value !== shownLabel.current) {
        actions.current?.setInputValue(shownLabel.current);
      }
    }, 0);

  useEffect(() => {
    const keepEscapeForSearch = (event) => {
      const input = document.getElementById(id);
      if (
        event.key === "Escape" &&
        input &&
        document.activeElement === input &&
        actions.current &&
        (input.getAttribute("aria-expanded") === "true" ||
          input.value !== shownLabel.current)
      ) {
        event.preventDefault();
        event.stopPropagation();
        actions.current.closeMenu();
        actions.current.setInputValue(shownLabel.current);
      }
    };
    window.addEventListener("keydown", keepEscapeForSearch, true);
    return () =>
      window.removeEventListener("keydown", keepEscapeForSearch, true);
  }, [id]);

  return (
    <ComboBox
      id={id}
      className={className}
      titleText={
        hideLabel ? (
          <span className="cds--visually-hidden">{labelText}</span>
        ) : required ? (
          <>
            {labelText}
            <RequiredMarker />
          </>
        ) : (
          labelText
        )
      }
      {...requiredProps(required)}
      onBlur={(event) => {
        restoreChosenLabel();
        onBlur?.(event);
      }}
      downshiftActions={actions}
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
      onChange={({ selectedItem: chosen }) => {
        const id = chosen ? String(chosen.id) : "";
        if (id !== String(value ?? "")) {
          onChange(id);
        }
      }}
    />
  );
};

export default SearchableSelect;
