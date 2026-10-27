import React from "react";
import { ComboBox } from "@carbon/react";
import { useIntl } from "react-intl";

/** The systems most catalogs cite; any other identifier may be typed. */
export const STANDARD_SOURCES = ["LOINC", "SNOMED", "CIEL", "OCL"];

/** The length of every terminology table's source column. */
export const MAX_SOURCE_LENGTH = 20;

export const sourceTooLong = (source) =>
  (source || "").trim().length > MAX_SOURCE_LENGTH;

/**
 * A terminology mapping's source: one of the standard systems, or the
 * identifier the catalog's own codes are kept under.
 */
const TerminologySourceInput = ({ id, titleText = "", value, onChange }) => {
  const intl = useIntl();
  return (
    <ComboBox
      id={id}
      titleText={titleText}
      aria-label={
        titleText
          ? undefined
          : intl.formatMessage({ id: "label.testCatalog.terminology.source" })
      }
      allowCustomValue
      items={STANDARD_SOURCES}
      shouldFilterItem={({ item, inputValue }) =>
        !inputValue || item.toLowerCase().includes(inputValue.toLowerCase())
      }
      selectedItem={value || null}
      invalid={sourceTooLong(value)}
      invalidText={intl.formatMessage(
        { id: "label.testCatalog.terminology.sourceTooLong" },
        { max: MAX_SOURCE_LENGTH },
      )}
      onChange={({ selectedItem }) => onChange(selectedItem || "")}
      onInputChange={(text) => onChange(text || "")}
    />
  );
};

export default TerminologySourceInput;
