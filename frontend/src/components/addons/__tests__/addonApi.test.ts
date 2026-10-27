import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
  putToOpenElisServerJsonResponse: vi.fn(),
  postToOpenElisServerFormDataJsonResponse: vi.fn(),
  deleteFromOpenElisServer: vi.fn(),
}));

import {
  getFromOpenElisServer,
  postToOpenElisServerFormDataJsonResponse,
  postToOpenElisServerJsonResponse,
  putToOpenElisServerJsonResponse,
} from "../../utils/Utils";
import {
  AddonRequestError,
  installAddon,
  invalidateExtensions,
  loadExtensions,
  requestToken,
  setAddonEnabled,
} from "../addonApi";

describe("add-on API calls", () => {
  beforeEach(() => vi.clearAllMocks());

  it("asks for a frame token with the slot and the page state", async () => {
    vi.mocked(postToOpenElisServerJsonResponse).mockImplementation(
      (_path, _body, callback) =>
        callback({
          token: "oeat.x.y",
          expiresIn: 600,
          scopes: [],
          context: {},
        }),
    );
    const issued = await requestToken(
      "hello",
      "result-lookup-side-panel-slot",
      {
        page: "patient-results",
        patientId: "42",
      },
    );
    expect(issued.token).toBe("oeat.x.y");
    const [path, body] = vi.mocked(postToOpenElisServerJsonResponse).mock
      .calls[0];
    expect(path).toBe("/rest/addons/hello/token");
    expect(JSON.parse(body as string)).toEqual({
      slot: "result-lookup-side-panel-slot",
      context: { page: "patient-results", patientId: "42" },
    });
  });

  it("uploads the package as the multipart field 'package'", async () => {
    vi.mocked(postToOpenElisServerFormDataJsonResponse).mockImplementation(
      (_path, _form, callback) =>
        callback({ upgraded: false, addon: { id: "hello" } }),
    );
    const file = new File(["zip"], "hello.zip", { type: "application/zip" });
    await installAddon(file);
    const [path, form] = vi.mocked(postToOpenElisServerFormDataJsonResponse)
      .mock.calls[0];
    expect(path).toBe("/rest/addons");
    expect((form as FormData).get("package")).toBe(file);
  });

  it("turns a refusal into an error carrying the server's code", async () => {
    vi.mocked(postToOpenElisServerFormDataJsonResponse).mockImplementation(
      (_path, _form, callback) =>
        callback({
          status: 400,
          code: "manifest.invalid",
          message: "addon.json is not valid",
          problems: ["id is required"],
        }),
    );
    const error = await installAddon(new File(["x"], "x.zip")).catch((e) => e);
    expect(error).toBeInstanceOf(AddonRequestError);
    expect(error.code).toBe("manifest.invalid");
    expect(error.problems).toEqual(["id is required"]);
  });

  it("switches an add-on with a JSON body", async () => {
    vi.mocked(putToOpenElisServerJsonResponse).mockImplementation(
      (_path, _body, callback) => callback({ id: "hello", enabled: true }),
    );
    await setAddonEnabled("hello", true);
    const [path, body] = vi.mocked(putToOpenElisServerJsonResponse).mock
      .calls[0];
    expect(path).toBe("/rest/addons/hello/enabled");
    expect(JSON.parse(body as string)).toEqual({ enabled: true });
  });

  it("loads the extensions once until they are invalidated", async () => {
    vi.mocked(getFromOpenElisServer).mockImplementation((_path, callback) =>
      callback({ extensions: [] }),
    );
    invalidateExtensions();
    await loadExtensions();
    await loadExtensions();
    expect(getFromOpenElisServer).toHaveBeenCalledTimes(1);
    expect(vi.mocked(getFromOpenElisServer).mock.calls[0][0]).toBe(
      "/rest/addons/extensions",
    );
    invalidateExtensions();
    await loadExtensions();
    expect(getFromOpenElisServer).toHaveBeenCalledTimes(2);
  });

  it("leaves the slots empty when the extensions cannot be read", async () => {
    vi.mocked(getFromOpenElisServer).mockImplementation((_path, callback) =>
      callback(undefined),
    );
    invalidateExtensions();
    expect(await loadExtensions()).toEqual([]);
  });
});
