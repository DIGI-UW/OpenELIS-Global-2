import {
  InventoryItemAPI,
  InventoryLotAPI,
  InventoryLotStorageAPI,
  InventoryManagementAPI,
} from "./InventoryService";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../utils/Utils";

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
    message: "Inventory item code already exists: MY-REAGENT",
    errorCode: "inventory.item.error.duplicateCode",
    params: { code: "MY-REAGENT" },
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
    expect(err.params).toEqual({ code: "MY-REAGENT" });
  });
});

// The 400 body InventoryLotRestController.update builds for a duplicate barcode.
const duplicateBarcodeBody = {
  message: "Barcode VR4BC001 is already assigned to lot VR4-BCOWNER",
  errorCode: "inventory.lot.error.duplicateBarcode",
  params: { barcode: "VR4BC001", lotNumber: "VR4-BCOWNER" },
};

describe("InventoryLotAPI.update", () => {
  it("keeps errorCode and params on the rejection from a lot edit", async () => {
    // put() reads the CSRF token; jsdom here has no localStorage.
    vi.stubGlobal("localStorage", { getItem: () => "csrf-token" });
    global.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 400,
      headers: { get: () => "application/json" },
      json: () => Promise.resolve(duplicateBarcodeBody),
    });

    const err = await InventoryLotAPI.update(8, { barcode: "VR4BC001" }).catch(
      (e) => e,
    );

    expect(err).toBeInstanceOf(Error);
    expect(err.message).toBe(duplicateBarcodeBody.message);
    expect(err.errorCode).toBe("inventory.lot.error.duplicateBarcode");
    expect(err.params).toEqual(duplicateBarcodeBody.params);
  });
});

describe("InventoryLotStorageAPI.getMovements", () => {
  it("reads the lot's movement rows from the storage movements endpoint", async () => {
    const rows = [{ id: 1, reason: "Consolidating stock" }];
    getFromOpenElisServer.mockImplementation((endpoint, callback) =>
      callback(rows),
    );

    await expect(InventoryLotStorageAPI.getMovements(7)).resolves.toEqual(rows);
    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/storage/inventory-lots/7/movements",
      expect.any(Function),
    );
  });
});
