import { formatTransferSourceLab } from "./biorepositoryTransferHelpers";

describe("biorepositoryTransferHelpers", () => {
  test("formatTransferSourceLab maps legacy MEDICAL_LAB CTD transfers to CTD", () => {
    expect(formatTransferSourceLab("MEDICAL_LAB")).toBe("CTD");
  });

  test("formatTransferSourceLab leaves other source labs unchanged", () => {
    expect(formatTransferSourceLab("CTD")).toBe("CTD");
    expect(formatTransferSourceLab("TB_LAB")).toBe("TB_LAB");
    expect(formatTransferSourceLab("Bioanalytical Laboratory")).toBe(
      "Bioanalytical Laboratory",
    );
  });
});
