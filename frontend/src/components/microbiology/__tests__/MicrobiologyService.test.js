import MicrobiologyService, {
  logCriticalCommunication,
  releaseFinalReport,
  revertAstOverride,
  selectReportableAstRun,
  startRepeatAstRun,
} from "../MicrobiologyService";
import { postToOpenElisServerJsonResponse } from "../../utils/Utils";

vi.mock("../../utils/Utils", () => ({
  postToOpenElisServerJsonResponse: vi.fn(),
  getFromOpenElisServer: vi.fn(),
  putToOpenElisServerFullResponse: vi.fn(),
}));

describe("MicrobiologyService", () => {
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
