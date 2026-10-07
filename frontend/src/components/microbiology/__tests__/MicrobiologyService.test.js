import MicrobiologyService, {
  getAstPanels,
  previewMicrobiologyOrder,
  createIsolate,
  updateIsolateIdentification,
  recordCaseActivity,
  logCriticalCommunication,
  releaseFinalReport,
  revertAstOverride,
  selectReportableAstRun,
  startRepeatAstRun,
} from "../MicrobiologyService";
import {
  getFromOpenElisServer,
  putToOpenElisServerFullResponse,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";

vi.mock("../../utils/Utils", () => ({
  postToOpenElisServerJsonResponse: vi.fn(),
  getFromOpenElisServer: vi.fn(),
  putToOpenElisServerFullResponse: vi.fn(),
}));

describe("MicrobiologyService", () => {
  it.each([
    undefined,
    { status: 403 },
    { error: "offline" },
    {},
    { cases: [] },
  ])("rejects failed or malformed order previews %o", async (response) => {
    postToOpenElisServerJsonResponse.mockImplementationOnce(
      (url, body, callback) => callback(response),
    );
    await expect(previewMicrobiologyOrder({ specimens: [] })).rejects.toThrow();
  });

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

  it("rejects a denied isolate creation instead of completing the form", async () => {
    postToOpenElisServerJsonResponse.mockImplementationOnce(
      (url, body, callback) => callback({ status: 403 }),
    );
    await expect(
      createIsolate({ caseId: "case-1", sourceSampleItemId: "sample-1" }),
    ).rejects.toThrow();
  });

  it.each([
    undefined,
    { ok: false, status: 403, json: async () => ({ message: "Denied" }) },
  ])("rejects unsuccessful identification responses %o", async (response) => {
    putToOpenElisServerFullResponse.mockImplementationOnce(
      (url, body, callback) => callback(response),
    );
    await expect(
      updateIsolateIdentification("isolate-1", {}),
    ).rejects.toThrow();
  });

  it("settles empty error and malformed success responses, and returns saved identification", async () => {
    putToOpenElisServerFullResponse.mockImplementationOnce(
      (url, body, callback) => callback({ ok: false, status: 409 }),
    );
    await expect(
      updateIsolateIdentification("isolate-1", {}),
    ).rejects.toMatchObject({ status: 409 });
    putToOpenElisServerFullResponse.mockImplementationOnce(
      (url, body, callback) =>
        callback({
          ok: true,
          json: async () => {
            throw new SyntaxError("Invalid JSON");
          },
        }),
    );
    await expect(updateIsolateIdentification("isolate-1", {})).rejects.toThrow(
      "Invalid JSON",
    );
    const saved = { id: "isolate-1", identificationStatus: "CONFIRMED" };
    putToOpenElisServerFullResponse.mockImplementationOnce(
      (url, body, callback) => callback({ ok: true, json: async () => saved }),
    );
    await expect(updateIsolateIdentification("isolate-1", {})).resolves.toEqual(
      saved,
    );
  });

  it("sends the lab-unit worklist filter without retired workflow authority", async () => {
    getFromOpenElisServer.mockImplementationOnce((url, callback) =>
      callback({ rows: [] }),
    );
    await MicrobiologyService.getWorklistRows({
      testSectionId: "unit-9",
      workflow: "BACTERIOLOGY",
    });
    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/microbiology/worklist?testSectionId=unit-9",
      expect.any(Function),
    );
  });

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
