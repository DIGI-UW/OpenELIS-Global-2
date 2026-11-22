import { LEVEL_ORDER } from "./useLocationPicker";

const ANCESTOR_FIELDS = {
  room: { idKey: "parentRoomId", nameKeys: ["parentRoomName", "roomName"] },
  device: {
    idKey: "parentDeviceId",
    nameKeys: ["parentDeviceName", "deviceName"],
  },
  shelf: {
    idKey: "parentShelfId",
    nameKeys: ["parentShelfLabel", "shelfLabel"],
  },
  rack: { idKey: "parentRackId", nameKeys: ["parentRackLabel", "rackLabel"] },
};

const firstDefined = (result, keys) => {
  const key = keys.find((k) => result[k] !== undefined && result[k] !== null);
  return key ? result[key] : undefined;
};

export function searchResultToReplaceAction(result) {
  if (!result || typeof result !== "object") return null;
  const { type, id } = result;
  if (!type || !LEVEL_ORDER.includes(type)) return null;

  const selection = { [type]: { id, name: result.name || result.label } };

  const leafIndex = LEVEL_ORDER.indexOf(type);
  for (let i = leafIndex - 1; i >= 0; i--) {
    const level = LEVEL_ORDER[i];
    const { idKey, nameKeys } = ANCESTOR_FIELDS[level];
    const ancestorId = result[idKey];
    if (ancestorId === undefined || ancestorId === null) break;
    selection[level] = { id: ancestorId, name: firstDefined(result, nameKeys) };
  }

  return { type: "REPLACE_SELECTION", selection };
}
