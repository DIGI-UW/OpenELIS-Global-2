import React, { useEffect, useMemo, useState } from "react";
import { ComboBox } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  buildLinkedEquipmentInstrumentsUrl,
  formatLinkedEquipmentLabel,
} from "../notebookLinkedEquipment";
import { loadNotebookEquipmentOptions } from "../utils/notebookInventoryScope";

/**
 * Searchable equipment picker scoped to notebook owning departments (not All Lab Units).
 */
function NotebookDepartmentEquipmentPicker({
  notebookId,
  id = "notebook-equipment-picker",
  titleText,
  label,
  placeholder,
  selectedItem,
  templateInstruments = [],
  disabled = false,
  onChange,
}) {
  const intl = useIntl();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (templateInstruments?.length > 0) {
      setItems(
        templateInstruments.map((analyzer) => ({
          id: analyzer.id ?? analyzer.value,
          label: formatLinkedEquipmentLabel(analyzer),
        })),
      );
      return undefined;
    }

    if (!notebookId) {
      setItems([]);
      return undefined;
    }

    setLoading(true);
    const controller = new AbortController();
    loadNotebookEquipmentOptions(
      notebookId,
      (departmentIds) => buildLinkedEquipmentInstrumentsUrl(departmentIds),
      (options) => {
        setItems(
          (options || []).map((item) => ({
            id: item.id,
            label: item.value || formatLinkedEquipmentLabel(item),
          })),
        );
        setLoading(false);
      },
      controller.signal,
    );

    return () => controller.abort();
  }, [notebookId, templateInstruments]);

  const selected = useMemo(() => {
    if (!selectedItem) {
      return null;
    }
    return (
      items.find((item) => String(item.id) === String(selectedItem.id)) || {
        id: selectedItem.id,
        label: selectedItem.label || selectedItem.value || "",
      }
    );
  }, [items, selectedItem]);

  return (
    <ComboBox
      id={id}
      titleText={
        titleText ||
        intl.formatMessage({
          id: "notebook.equipment.picker.title",
          defaultMessage: "Linked equipment / instruments",
        })
      }
      placeholder={
        placeholder ||
        label ||
        intl.formatMessage({
          id: "notebook.equipment.picker.placeholder",
          defaultMessage: "Select equipment...",
        })
      }
      items={items}
      itemToString={(item) => (item ? item.label : "")}
      selectedItem={selected}
      onChange={({ selectedItem: next }) => onChange?.(next)}
      disabled={disabled || loading}
      shouldFilterItem={({ item, inputValue }) => {
        if (!inputValue) {
          return true;
        }
        return item?.label?.toLowerCase().includes(inputValue.toLowerCase());
      }}
      helperText={
        loading ? (
          <FormattedMessage
            id="notebook.equipment.picker.loading"
            defaultMessage="Loading equipment..."
          />
        ) : items.length === 0 ? (
          <FormattedMessage
            id="notebook.equipment.picker.empty"
            defaultMessage="No equipment available for this notebook."
          />
        ) : null
      }
    />
  );
}

export default NotebookDepartmentEquipmentPicker;
