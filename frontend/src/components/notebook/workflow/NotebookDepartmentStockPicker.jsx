import React, { useEffect, useMemo, useState } from "react";
import { ComboBox } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { buildLinkedStockInventoryUrl } from "../notebookLinkedEquipment";
import {
  loadNotebookScopedInventory,
  NOTEBOOK_INVENTORY_SCOPE_STATUS,
} from "../utils/notebookInventoryScope";

const mapStockOptions = (response) => {
  if (!response || !Array.isArray(response)) {
    return [];
  }
  return response.map((item) => ({
    id: item.id,
    itemId: item.id,
    label: item.name || item.description || String(item.id),
    name: item.name,
    lotNumber: item.lotNumber,
    units: item.units,
  }));
};

/**
 * Searchable stock/reagent picker for material usage — department-scoped, separate from equipment.
 */
function NotebookDepartmentStockPicker({
  notebookId,
  id = "notebook-stock-picker",
  titleText,
  label,
  placeholder,
  selectedItem,
  disabled = false,
  onChange,
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
    loadNotebookScopedInventory(
      notebookId,
      buildLinkedStockInventoryUrl([]),
      (response, error, meta = {}) => {
        if (!error) {
          setItems(mapStockOptions(response));
        } else {
          setItems([]);
        }
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
  }, [notebookId]);

  const selected = useMemo(() => {
    if (!selectedItem) {
      return null;
    }
    return (
      items.find((item) => String(item.id) === String(selectedItem.id)) || selectedItem
    );
  }, [items, selectedItem]);

  return (
    <ComboBox
      id={id}
      titleText={
        titleText ||
        intl.formatMessage({
          id: "notebook.stock.picker.title",
          defaultMessage: "Reagents / Consumables",
        })
      }
      placeholder={
        placeholder ||
        label ||
        intl.formatMessage({
          id: "notebook.stock.picker.placeholder",
          defaultMessage: "Select stock item...",
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
            id="notebook.stock.picker.loading"
            defaultMessage="Loading stock items..."
          />
        ) : scopeStatus ===
          NOTEBOOK_INVENTORY_SCOPE_STATUS.DEPARTMENT_SCOPE_UNAVAILABLE ? (
          <FormattedMessage
            id="notebook.stock.picker.noDepartmentScope"
            defaultMessage="Notebook departments could not be resolved for inventory-scoped reagents."
          />
        ) : items.length === 0 ||
          scopeStatus === NOTEBOOK_INVENTORY_SCOPE_STATUS.NO_INVENTORY_LOTS ? (
          <FormattedMessage
            id="notebook.stock.picker.empty"
            defaultMessage="No active reagent or consumable lots were found in this notebook's departments."
          />
        ) : null
      }
    />
  );
}

export default NotebookDepartmentStockPicker;
