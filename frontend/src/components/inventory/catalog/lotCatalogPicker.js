import { getItemTypeLabel, isLotReceivableType } from "./inventoryItemTypeLabels";

/** Build searchable ComboBox options for Receive Lot (excludes EQUIPMENT). */
export function buildLotCatalogOptions(catalogItems) {
  return (catalogItems || [])
    .filter((item) => isLotReceivableType(item.itemType))
    .sort((a, b) => a.name.localeCompare(b.name))
    .map((catalogItem) => {
      const typeLabel = getItemTypeLabel(catalogItem.itemType);
      const category = catalogItem.category || "";
      return {
        id: catalogItem.id,
        text: `${catalogItem.name} (${typeLabel})`,
        searchText: `${catalogItem.name} ${typeLabel} ${category}`.toLowerCase(),
        item: catalogItem,
      };
    });
}

export function filterCatalogOptions(options, inputValue) {
  if (!inputValue) {
    return options;
  }
  const needle = inputValue.trim().toLowerCase();
  return options.filter((item) => item.searchText.includes(needle));
}
