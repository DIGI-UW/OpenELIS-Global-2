import { Permissions } from "../constants/roles";
import {
  storageMutationRoles,
  inventorySaveRoles,
  inventoryItemMutationRoles,
  equipmentMutationRoles,
} from "./rbacActions";

describe("rbacActions", () => {
  it("storageMutationRoles includes update and equipment permissions", () => {
    expect(storageMutationRoles).toEqual(
      expect.arrayContaining([
        ...Permissions.UPDATE_SAMPLES,
        ...Permissions.MANAGE_EQUIPMENT,
      ]),
    );
  });

  it("inventorySaveRoles covers reagent and equipment editors", () => {
    expect(inventorySaveRoles).toEqual(
      expect.arrayContaining([...equipmentMutationRoles]),
    );
  });

  it("inventoryItemMutationRoles matches inventorySaveRoles", () => {
    expect(inventoryItemMutationRoles).toEqual(inventorySaveRoles);
  });
});
