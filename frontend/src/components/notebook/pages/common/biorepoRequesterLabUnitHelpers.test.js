import {
  WORKFLOW_DEPARTMENT_NAMES,
  getNonBiorepositoryDepartments,
  isBiorepositoryLabUnit,
  resolveRequesterLabUnit,
} from "./biorepoRequesterLabUnitHelpers";

describe("biorepoRequesterLabUnitHelpers", () => {
  test("isBiorepositoryLabUnit detects biorepository names", () => {
    expect(isBiorepositoryLabUnit("Biorepository")).toBe(true);
    expect(isBiorepositoryLabUnit("Biorepository Laboratory")).toBe(true);
    expect(isBiorepositoryLabUnit("CTD")).toBe(false);
    expect(isBiorepositoryLabUnit("Immunology")).toBe(false);
  });

  test("resolveRequesterLabUnit uses medlab workflow map to CTD", () => {
    expect(
      resolveRequesterLabUnit({
        notebookData: { workflowType: "medlab" },
        session: { loginLabUnit: "Biorepository Laboratory" },
      }),
    ).toBe("CTD");
  });

  test("resolveRequesterLabUnit prefers non-biorepository notebook department", () => {
    expect(
      resolveRequesterLabUnit({
        notebookData: {
          workflowType: "immunology",
          departments: [
            { testSectionName: "Biorepository Laboratory" },
            { testSectionName: "Immunology" },
          ],
        },
        session: { loginLabUnit: "Biorepository Laboratory" },
      }),
    ).toBe("Immunology");
  });

  test("resolveRequesterLabUnit ignores biorepository session fallback", () => {
    expect(
      resolveRequesterLabUnit({
        session: { loginLabUnit: "Biorepository Laboratory" },
      }),
    ).toBe("");
  });

  test("resolveRequesterLabUnit falls back to session when not biorepository", () => {
    expect(
      resolveRequesterLabUnit({
        session: { loginLabUnit: "Immunology" },
      }),
    ).toBe("Immunology");
  });

  test("getNonBiorepositoryDepartments filters biorepository department", () => {
    expect(
      getNonBiorepositoryDepartments({
        departments: [
          { localizedName: "Biorepository Laboratory" },
          { localizedName: "CTD" },
        ],
      }),
    ).toEqual(["CTD"]);
  });

  test("WORKFLOW_DEPARTMENT_NAMES includes medlab CTD mapping", () => {
    expect(WORKFLOW_DEPARTMENT_NAMES.medlab).toBe("CTD");
  });
});
