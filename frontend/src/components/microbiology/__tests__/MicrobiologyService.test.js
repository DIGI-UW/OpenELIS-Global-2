import MicrobiologyService, {
  getAstPanels,
  recordCaseActivity,
  logCriticalCommunication,
  releaseFinalReport,
  revertAstOverride,
  selectReportableAstRun,
  startRepeatAstRun,
} from "../MicrobiologyService";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";

vi.mock("../../utils/Utils", () => ({
  postToOpenElisServerJsonResponse: vi.fn(),
  getFromOpenElisServer: vi.fn(),
  putToOpenElisServerFullResponse: vi.fn(),
}));

describe("MicrobiologyService", () => {
  it.each([undefined, { status: 0 }, { status: 403 }, { status: 409 }])(
    "rejects a failed culture outcome response %o",
    async (response) => {
      postToOpenElisServerJsonResponse.mockImplementationOnce(
        (url, body, callback) => callback(response),
      );
      await expect(
        recordCaseActivity("case-1", {
          sourceSampleItemId: "sample-1",
          nextStage: "POSITIVE_SIGNAL",
        }),
      ).rejects.toThrow();
    },
  );

  it("loads panel options without the retired workflow query parameter", async () => {
    const panels = [{ id: "panel-1", label: "Gram negative panel" }];
    getFromOpenElisServer.mockImplementationOnce((url, callback) =>
      callback(panels),
    );
    await expect(getAstPanels()).resolves.toEqual(panels);
    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/microbiology/reference/ast-panels",
      expect.any(Function),
    );
  });

  it("rejects a critical communication whose write returns an error status", async () => {
    postToOpenElisServerJsonResponse.mockImplementationOnce((url, body, cb) =>
      cb({ status: 409, error: "conflict" }),
    );
    await expect(logCriticalCommunication("case-1", {})).rejects.toThrow();
  });

  it("rejects a final release whose write fails and resolves when it succeeds", async () => {
    postToOpenElisServerJsonResponse.mockImplementationOnce((url, body, cb) =>
      cb({ status: 0 }),
    );
    await expect(releaseFinalReport("case-1")).rejects.toThrow();
    postToOpenElisServerJsonResponse.mockImplementationOnce((url, body, cb) =>
      cb({ id: "case-1", stage: "FINAL_RELEASED" }),
    );
    await expect(releaseFinalReport("case-1")).resolves.toEqual({
      id: "case-1",
      stage: "FINAL_RELEASED",
    });
  });

  it("exposes repeat-attempt operations through the shared service contract", () => {
    expect(MicrobiologyService.startRepeatAstRun).toBe(startRepeatAstRun);
    expect(MicrobiologyService.selectReportableAstRun).toBe(
      selectReportableAstRun,
    );
    expect(MicrobiologyService.revertAstOverride).toBe(revertAstOverride);
  });
});
