// LESOTHO: the section is saved and loaded for an order's lab number.
import { beforeEach, describe, expect, it, vi } from "vitest";

const utils = vi.hoisted(() => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
}));
vi.mock("../../components/utils/Utils", () => utils);

import {
  loadClinicalInformation,
  saveClinicalInformation,
} from "./clinicalInformationApi";

beforeEach(() => {
  utils.getFromOpenElisServer.mockReset();
  utils.postToOpenElisServerJsonResponse.mockReset();
});

describe("clinicalInformationApi", () => {
  it("posts the section for the order's lab number", () => {
    utils.postToOpenElisServerJsonResponse.mockImplementation((_u, _b, cb) =>
      cb({ status: "saved" }),
    );
    const done = vi.fn();
    saveClinicalInformation("DEV0126000000001", { forms: ["DNA_PCR"] }, done);
    const [url, body] = utils.postToOpenElisServerJsonResponse.mock.calls[0];
    expect(url).toBe(
      "/rest/lesotho/order-clinical-information/DEV0126000000001",
    );
    expect(JSON.parse(body)).toEqual({ forms: ["DNA_PCR"] });
    expect(done).toHaveBeenCalledWith(true);
  });

  it("reports a failed save", () => {
    utils.postToOpenElisServerJsonResponse.mockImplementation((_u, _b, cb) =>
      cb({ error: "no order has lab number X", statusCode: 400 }),
    );
    const done = vi.fn();
    saveClinicalInformation("X", { forms: [] }, done);
    expect(done).toHaveBeenCalledWith(false);
  });

  it("sends nothing when the section was never filled in, or there is no lab number", () => {
    saveClinicalInformation("DEV01", undefined);
    saveClinicalInformation("", { forms: ["TB"] });
    expect(utils.postToOpenElisServerJsonResponse).not.toHaveBeenCalled();
  });

  it("loads the saved section for a lab number", () => {
    utils.getFromOpenElisServer.mockImplementation((_u, cb) =>
      cb({ data: { forms: ["TB"] } }),
    );
    const onLoaded = vi.fn();
    loadClinicalInformation("DEV01", onLoaded);
    expect(utils.getFromOpenElisServer.mock.calls[0][0]).toBe(
      "/rest/lesotho/order-clinical-information/DEV01",
    );
    expect(onLoaded).toHaveBeenCalledWith({ forms: ["TB"] });
  });
});
