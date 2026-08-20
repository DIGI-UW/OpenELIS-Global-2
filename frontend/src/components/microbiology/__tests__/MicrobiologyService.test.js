import MicrobiologyService, {
  revertAstOverride,
  selectReportableAstRun,
  startRepeatAstRun,
} from "../MicrobiologyService";

describe("MicrobiologyService", () => {
  it("exposes repeat-attempt operations through the shared service contract", () => {
    expect(MicrobiologyService.startRepeatAstRun).toBe(startRepeatAstRun);
    expect(MicrobiologyService.selectReportableAstRun).toBe(
      selectReportableAstRun,
    );
    expect(MicrobiologyService.revertAstOverride).toBe(revertAstOverride);
  });
});
