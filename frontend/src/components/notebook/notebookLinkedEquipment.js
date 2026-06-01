/**
 * Linked equipment on notebook templates is workflow metadata only.
 * Options come from inventory catalog items of type EQUIPMENT in allowed departments.
 */

export const LINKED_EQUIPMENT_ITEM_TYPE = "EQUIPMENT";
export const LINKED_STOCK_ITEM_TYPES = ["REAGENT", "CARTRIDGE", "CONSUMABLE", "RDT", "HIV_KIT", "SYPHILIS_KIT"];

export function buildLinkedEquipmentInstrumentsUrl(departmentIds = []) {
  const params = new URLSearchParams({
    status: "active",
    requireLots: "false",
  });
  params.append("itemTypes", LINKED_EQUIPMENT_ITEM_TYPE);
  (departmentIds || [])
    .map((id) => (typeof id === "object" ? id?.id : id))
    .filter((id) => id != null && id !== "")
    .forEach((id) => params.append("departmentIds", String(id)));
  return `/rest/inventory/instruments?${params.toString()}`;
}

export function formatLinkedEquipmentLabel(item) {
  if (!item?.name) {
    return item?.value || "";
  }
  return item.name;
}

export function mapLinkedEquipmentOptions(response) {
  if (!response || !Array.isArray(response)) {
    return [];
  }
  return response.map((item) => ({
    id: item.id,
    value: formatLinkedEquipmentLabel(item),
    itemType: item.itemType,
  }));
}

export function buildLinkedStockInventoryUrl(departmentIds = []) {
  const params = new URLSearchParams({
    status: "active",
    requireLots: "false",
  });
  LINKED_STOCK_ITEM_TYPES.forEach((type) => params.append("itemTypes", type));
  (departmentIds || [])
    .map((id) => (typeof id === "object" ? id?.id : id))
    .filter((id) => id != null && id !== "")
    .forEach((id) => params.append("departmentIds", String(id)));
  return `/rest/inventory/instruments?${params.toString()}`;
}
