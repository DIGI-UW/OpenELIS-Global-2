import React from "react";
import { useIntl } from "react-intl";
import SearchableSelect from "../common/SearchableSelect";
import { heldOrServedItems, isStainedRow, optionId } from "./pathologyRows";
import "./pathologyCaseView.scss";

/**
 * A slide's stain, picked from the laboratory's stain dictionary, wherever a
 * slide row offers it (Microtomy, Staining). An empty dictionary is a
 * configuration gap the bench is told about; until the list has arrived
 * there is nothing to pick and nothing yet to say. A row held for its own
 * reasons says nothing about it, since that is not what holds it.
 *
 * The stain on a stained slide is the one that was applied, so it is frozen
 * until the slide is marked unsatisfactory. It is then shown as text with the
 * way to change it under it, not as a disabled picker: a disabled field greys
 * its helper text below a readable contrast, and there is nothing to pick.
 * The stain's name keeps the picker's id, so the row reads the same way.
 */
const StainPicker = ({
  id,
  slide,
  stains,
  stainNames,
  labelText,
  hideLabel = false,
  size = "md",
  disabled,
  onChange,
}) => {
  const intl = useIntl();
  const items = heldOrServedItems(stains, slide.stainId, stainNames);
  const stainsUnknown = !Array.isArray(stains);
  const noStains = !stainsUnknown && stains.length === 0;

  // The stain the row holds, by name; the served list carries a held name
  // the dictionary no longer serves.
  const chosenName = items.find(
    (item) => String(item.id) === String(slide.stainId),
  )?.value;

  if (isStainedRow(slide)) {
    return (
      <div className="pathology-case-view__frozen-stain">
        <span
          className={
            hideLabel
              ? "cds--visually-hidden"
              : "pathology-case-view__row-meta-label"
          }
        >
          {labelText}
        </span>
        <span id={id} className="pathology-case-view__frozen-stain-name">
          {chosenName ?? ""}
        </span>
        <span id={id + "Hint"} className="pathology-case-view__hint">
          {intl.formatMessage({ id: "pathology.locked.stainFrozen" })}
        </span>
      </div>
    );
  }

  // A narrow picker cuts a long stain name short, so the whole name is
  // shown on hover.
  return (
    <div className="pathology-case-view__stain-picker" title={chosenName}>
      <SearchableSelect
        id={id}
        size={size}
        hideLabel={hideLabel}
        labelText={labelText}
        placeholder={intl.formatMessage({ id: "common.select" })}
        options={items}
        value={slide.stainId ?? ""}
        onChange={(chosen) => onChange(optionId(items, chosen))}
        disabled={disabled || stainsUnknown || noStains}
        helperText={
          !disabled && noStains
            ? intl.formatMessage({ id: "pathology.empty.noStains" })
            : undefined
        }
      />
    </div>
  );
};

export default StainPicker;
