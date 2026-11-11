import React, { useEffect, useState } from "react";
import { useIntl } from "react-intl";
import { ComboBox, TextInput } from "@carbon/react";
import { getFromOpenElisServer } from "../../../utils/Utils";

/**
 * The reported value and the performing laboratory of a test marked tested
 * elsewhere (FRS clinical order entry v4, FR-B20). The laboratory is a
 * search-first picker over the Organizations list, never free text. Each
 * change is handed to `onSave` once it is complete (the value on blur, the
 * laboratory on selection).
 */
const TestedElsewhereFields = ({ testId, mark, onSave, disabled }) => {
  const intl = useIntl();
  const [value, setValue] = useState(mark.reportedValue || "");
  const [search, setSearch] = useState("");
  const [labs, setLabs] = useState([]);
  const selectedLab = mark.performingLabId
    ? { id: mark.performingLabId, organizationName: mark.performingLabName }
    : null;

  useEffect(() => {
    const term = search.trim();
    if (term.length < 2) {
      return undefined;
    }
    let active = true;
    const timer = setTimeout(() => {
      getFromOpenElisServer(
        `/rest/organization/search?search=${encodeURIComponent(term)}`,
        (response) => {
          if (active) {
            setLabs(
              (response?.organizations || []).map((org) => ({
                id: String(org.id),
                organizationName: org.organizationName,
              })),
            );
          }
        },
      );
    }, 300);
    return () => {
      active = false;
      clearTimeout(timer);
    };
  }, [search]);

  const items =
    selectedLab && !labs.some((lab) => lab.id === String(selectedLab.id))
      ? [{ ...selectedLab, id: String(selectedLab.id) }, ...labs]
      : labs;

  return (
    <div
      className="tested-elsewhere-fields"
      data-testid={`tested-elsewhere-fields-${testId}`}
    >
      <TextInput
        id={`tested-elsewhere-value-${testId}`}
        size="sm"
        labelText={intl.formatMessage({
          id: "order.tests.testedElsewhere.value",
        })}
        helperText={intl.formatMessage({
          id: "order.tests.testedElsewhere.help",
        })}
        value={value}
        maxLength={255}
        onChange={(event) => setValue(event.target.value)}
        onBlur={() => {
          if (value !== (mark.reportedValue || "")) {
            onSave({ reportedValue: value });
          }
        }}
        disabled={disabled}
      />
      <ComboBox
        id={`tested-elsewhere-lab-${testId}`}
        size="sm"
        titleText={intl.formatMessage({
          id: "order.tests.testedElsewhere.lab",
        })}
        items={items}
        selectedItem={
          selectedLab ? { ...selectedLab, id: String(selectedLab.id) } : null
        }
        itemToString={(lab) => (lab ? lab.organizationName || "" : "")}
        onInputChange={(text) => setSearch(text || "")}
        onChange={({ selectedItem }) => {
          if (
            selectedItem &&
            String(selectedItem.id) !== String(mark.performingLabId || "")
          ) {
            onSave({
              performingLabId: selectedItem.id,
              performingLabName: selectedItem.organizationName,
            });
          }
        }}
        disabled={disabled}
      />
    </div>
  );
};

export default TestedElsewhereFields;
