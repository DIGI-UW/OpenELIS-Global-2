import {
  InventoryItemAPI,
  InventoryLotStorageAPI,
  InventoryManagementAPI,
} from "./InventoryService";
import { postToOpenElisServerJsonResponse } from "../utils/Utils";

vi.mock("../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
  postToOpenElisServerForBlob: vi.fn(),
}));

// The shape Utils.postToOpenElisServerJsonResponse hands the callback when
// the request never reached the server (its catch branch).
const transportFailure = {
  error: "Network error",
  message: "Network error",
  status: 0,
};

beforeEach(() => {
  vi.clearAllMocks();
  postToOpenElisServerJsonResponse.mockImplementation(
    (endpoint, payload, callback) => callback(transportFailure),
  );
});

describe("InventoryService POST wrappers on a dropped request", () => {
  it("rejects assignLocation instead of resolving", async () => {
    await expect(
      InventoryLotStorageAPI.assignLocation({ inventoryLotId: "1" }),
    ).rejects.toThrow("Network error");
  });

  it("rejects moveLocation instead of resolving", async () => {
    await expect(
      InventoryLotStorageAPI.moveLocation({ inventoryLotId: "1" }),
    ).rejects.toThrow("Network error");
  });

  it("rejects a lot receive instead of resolving", async () => {
    await expect(InventoryManagementAPI.receive({})).rejects.toThrow(
      "Network error",
    );
  });
});

describe("InventoryService POST wrappers on a 400 with a translated error", () => {
  // The body InventoryItemRestController.create builds for a
  // LocalizedValidationException, as Utils hands it to the callback.
  const duplicateCodeBody = {
    message: "Inventory item code already exists: MY_REAGENT",
    errorCode: "inventory.item.error.duplicateCode",
    params: { code: "MY_REAGENT" },
    status: 400,
    statusCode: 400,
    statusText: "Bad Request",
  };

  it("keeps errorCode and params on the rejection from create", async () => {
    postToOpenElisServerJsonResponse.mockImplementation(
      (endpoint, payload, callback) => callback(duplicateCodeBody),
    );

    const err = await InventoryItemAPI.create({ name: "My Reagent" }).catch(
      (e) => e,
    );

    expect(err).toBeInstanceOf(Error);
    expect(err.message).toBe(duplicateCodeBody.message);
    expect(err.errorCode).toBe("inventory.item.error.duplicateCode");
    expect(err.params).toEqual({ code: "MY_REAGENT" });
  });
});
