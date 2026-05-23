import React, { useEffect, useMemo, useState } from "react";
import { MultiSelect } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  buildLinkedEquipmentInstrumentsUrl,
  formatLinkedEquipmentLabel,
} from "../notebookLinkedEquipment";
import { loadNotebookEquipmentOptions } from "../utils/notebookInventoryScope";

const mapTemplateInstruments = (templateInstruments = []) =>
  templateInstruments.map((analyzer) => ({
    id: analyzer.id ?? analyzer.value,
    label: formatLinkedEquipmentLabel(analyzer) || analyzer.value || "",
    name: analyzer.value || analyzer.name,
  }));

/**
 * Multi-select equipment picker scoped to notebook owning departments.
 */
function NotebookDepartmentEquipmentMultiSelect({
  notebookId,
  id = "notebook-equipment-multiselect",
  titleText,
  label,
  selectedIds = [],
  templateInstruments = [],
  disabled = false,
  onSelectionChange,
}) {
  const intl = useIntl();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (templateInstruments?.length > 0) {
      setItems(mapTemplateInstruments(templateInstruments));
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
            name: item.value,
          })),
        );
        setLoading(false);
      },
      controller.signal,
    );

    return () => controller.abort();
  }, [notebookId, templateInstruments]);

  const selectedItems = useMemo(
    () => items.filter((item) => selectedIds.includes(item.id)),
    [items, selectedIds],
  );

  return (
    <MultiSelect
      id={id}
      titleText={
        titleText ||
        intl.formatMessage({ id: "notebook.equipment.picker.title" })
      }
      label={
        label ||
        intl.formatMessage({ id: "notebook.equipment.picker.placeholder" })
      }
      items={items}
      itemToString={(item) => (item ? item.label : "")}
      selectedItems={selectedItems}
      onChange={({ selectedItems: next }) => onSelectionChange?.(next)}
      disabled={disabled || loading}
      helperText={
        loading ? (
          <FormattedMessage id="notebook.equipment.picker.loading" />
        ) : items.length === 0 ? (
          <FormattedMessage id="notebook.equipment.picker.empty" />
        ) : null
      }
    />
  );
}

export default NotebookDepartmentEquipmentMultiSelect;
