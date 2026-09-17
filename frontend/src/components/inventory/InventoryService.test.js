import {
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
