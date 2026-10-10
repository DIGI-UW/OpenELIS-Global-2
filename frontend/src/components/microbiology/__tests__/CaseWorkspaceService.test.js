import { getCase, transferCase } from "../CaseWorkspaceService";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
}));

beforeEach(() => vi.clearAllMocks());

test("accepts a successful case status and encodes identifiers", async () => {
  const shell = { id: "case/1", status: "ACTIVE" };
  getFromOpenElisServer.mockImplementation((url, callback) => callback(shell));
  await expect(getCase(shell.id)).resolves.toEqual(shell);
  expect(getFromOpenElisServer).toHaveBeenCalledWith(
    "/rest/microbiology/cases/case%2F1/shell",
    expect.any(Function),
  );
});

test("rejects an HTTP access denial rather than presenting it as a case", async () => {
  getFromOpenElisServer.mockImplementation((url, callback) =>
    callback({ status: 403, error: "Forbidden" }),
  );
  await expect(getCase("hidden")).rejects.toThrow("request failed");
});

test("keeps successful transfer status separate from numeric HTTP errors", async () => {
  postToOpenElisServerJsonResponse.mockImplementation((url, body, callback) =>
    callback({ status: "ACTIVE", labUnitId: "2" }),
  );
  await expect(transferCase("1", "2")).resolves.toMatchObject({
    labUnitId: "2",
  });
  postToOpenElisServerJsonResponse.mockImplementation((url, body, callback) =>
    callback({ status: 409, error: "locked" }),
  );
  await expect(transferCase("1", "2")).rejects.toThrow("request failed");
});

test("submits component identifiers and preserves acknowledgement refusal details", async () => {
  const { saveResults } = await import("../CaseWorkspaceService");
  const body = {
    version: "123",
    components: [{ componentId: "primary", value: "12" }],
  };
  postToOpenElisServerJsonResponse.mockImplementation((url, json, callback) =>
    callback([{ analysisId: "a/1", components: body.components }]),
  );
  await expect(saveResults("case/1", "a/1", body)).resolves.toMatchObject([
    { analysisId: "a/1" },
  ]);
  expect(postToOpenElisServerJsonResponse).toHaveBeenCalledWith(
    "/rest/microbiology/cases/case%2F1/analyses/a%2F1/results",
    JSON.stringify(body),
    expect.any(Function),
  );
  const refusal = {
    status: 422,
    code: "ACKNOWLEDGEMENT_REQUIRED",
    acknowledgementRequired: [
      { analysisId: "a/1", componentId: "primary", kind: "CRITICAL" },
    ],
  };
  postToOpenElisServerJsonResponse.mockImplementation((url, json, callback) =>
    callback(refusal),
  );
  await expect(saveResults("case/1", "a/1", body)).rejects.toMatchObject({
    response: refusal,
  });
});
