import React, { useEffect, useMemo, useState } from "react";
import { ComboBox } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { buildLinkedStockInventoryUrl } from "../notebookLinkedEquipment";
import { loadNotebookScopedInventory } from "../utils/notebookInventoryScope";

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

  useEffect(() => {
    if (!notebookId) {
      setItems([]);
      return undefined;
    }

    setLoading(true);
    const controller = new AbortController();
    loadNotebookScopedInventory(
      notebookId,
      buildLinkedStockInventoryUrl([]),
      (response, error) => {
        if (!error) {
          setItems(mapStockOptions(response));
        } else {
          setItems([]);
        }
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
        ) : items.length === 0 ? (
          <FormattedMessage
            id="notebook.stock.picker.empty"
            defaultMessage="No stock items available for this notebook."
          />
        ) : null
      }
    />
  );
}

export default NotebookDepartmentStockPicker;
