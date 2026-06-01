import React, { useEffect, useMemo, useState } from "react";
import { MultiSelect } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  buildLinkedEquipmentInstrumentsUrl,
  formatLinkedEquipmentLabel,
} from "../notebookLinkedEquipment";
import {
  loadNotebookEquipmentOptions,
  mergeInventoryOptionsWithLinkedSelections,
  NOTEBOOK_INVENTORY_SCOPE_STATUS,
} from "../utils/notebookInventoryScope";

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
  const [scopeStatus, setScopeStatus] = useState(
    NOTEBOOK_INVENTORY_SCOPE_STATUS.READY,
  );

  useEffect(() => {
    if (!notebookId) {
      setItems([]);
      setScopeStatus(NOTEBOOK_INVENTORY_SCOPE_STATUS.DEPARTMENT_SCOPE_UNAVAILABLE);
      return undefined;
    }

    setLoading(true);
    const controller = new AbortController();
    loadNotebookEquipmentOptions(
      notebookId,
      (departmentIds) => buildLinkedEquipmentInstrumentsUrl(departmentIds),
      (options, error, meta = {}) => {
        const normalizedOptions = (options || []).map((item) => ({
          id: item.id,
          label: item.value || formatLinkedEquipmentLabel(item),
          name: item.value,
        }));
        setItems(
          mergeInventoryOptionsWithLinkedSelections(
            normalizedOptions,
            templateInstruments,
          ),
        );
        setScopeStatus(
          error
            ? NOTEBOOK_INVENTORY_SCOPE_STATUS.DEPARTMENT_SCOPE_UNAVAILABLE
            : meta.scopeStatus || NOTEBOOK_INVENTORY_SCOPE_STATUS.READY,
        );
        setLoading(false);
      },
      controller.signal,
    );

    return () => controller.abort();
  }, [notebookId, templateInstruments]);

  const selectedItems = useMemo(
    () =>
      items.filter((item) =>
        (selectedIds || []).some(
          (selectedId) => String(selectedId) === String(item.id),
        ),
      ),
    [items, selectedIds],
  );

  return (
    <MultiSelect
      id={id}
      titleText={
        titleText ||
        intl.formatMessage({
          id: "notebook.equipment.picker.title",
          defaultMessage: "Linked equipment / instruments",
        })
      }
      label={
        label ||
        intl.formatMessage({
          id: "notebook.equipment.picker.placeholder",
          defaultMessage: "Select equipment...",
        })
      }
      items={items}
      itemToString={(item) => (item ? item.label : "")}
      selectedItems={selectedItems}
      onChange={({ selectedItems: next }) => onSelectionChange?.(next)}
      disabled={disabled || loading}
      helperText={
        loading ? (
          <FormattedMessage
            id="notebook.equipment.picker.loading"
            defaultMessage="Loading equipment..."
          />
        ) : scopeStatus ===
          NOTEBOOK_INVENTORY_SCOPE_STATUS.DEPARTMENT_SCOPE_UNAVAILABLE ? (
          <FormattedMessage
            id="notebook.equipment.picker.noDepartmentScope"
            defaultMessage="Notebook departments could not be resolved for inventory-scoped equipment."
          />
        ) : items.length === 0 ||
          scopeStatus === NOTEBOOK_INVENTORY_SCOPE_STATUS.NO_INVENTORY_EQUIPMENT ? (
          <FormattedMessage
            id="notebook.equipment.picker.empty"
            defaultMessage="No active equipment found in this notebook's departments."
          />
        ) : null
      }
    />
  );
}

export default NotebookDepartmentEquipmentMultiSelect;
