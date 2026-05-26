import {
  resolveWorkflowTabComponent,
  resolveWorkflowType,
} from "./workflowRouting";

describe("workflowRouting", () => {
  it("distinguishes viral vaccine, virology, and genomics legacy titles", () => {
    expect(
      resolveWorkflowType({
        title: "Viral Vaccine - Lab 1",
        workflowType: "",
      }),
    ).toBe("viral_vaccine");
    expect(
      resolveWorkflowType({
        title: "Virology Laboratory - Lab 1",
        workflowType: "",
      }),
    ).toBe("virology");
    expect(
      resolveWorkflowType({
        title: "Genomics & Bioinformatics Laboratory - Lab 1",
        workflowType: "",
      }),
    ).toBe("genomics");
  });

  it("resolves dedicated tabs for the three workflow families", () => {
    expect(
      resolveWorkflowTabComponent({ workflowType: "viral_vaccine" }),
    ).toBeTruthy();
    expect(resolveWorkflowTabComponent({ workflowType: "virology" })).toBeTruthy();
    expect(resolveWorkflowTabComponent({ workflowType: "genomics" })).toBeTruthy();
  });
});
