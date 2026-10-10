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
