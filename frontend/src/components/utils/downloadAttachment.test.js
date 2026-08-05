import { downloadAttachment } from "./downloadAttachment";

describe("downloadAttachment", () => {
  it("uses one temporary object URL and removes the temporary link", () => {
    const click = vi.fn();
    const link = { click, remove: vi.fn(), href: "", download: "" };
    vi.spyOn(document, "createElement").mockReturnValue(link);
    vi.spyOn(document.body, "appendChild").mockImplementation(() => link);
    vi.spyOn(URL, "createObjectURL").mockReturnValue("blob:macro-export");
    vi.spyOn(URL, "revokeObjectURL").mockImplementation(() => {});
    const blob = new Blob(["csv"], { type: "text/csv" });

    downloadAttachment(blob, "openelis-text-macros.csv");

    expect(link.href).toBe("blob:macro-export");
    expect(link.download).toBe("openelis-text-macros.csv");
    expect(click).toHaveBeenCalledTimes(1);
    expect(link.remove).toHaveBeenCalledTimes(1);
    expect(URL.revokeObjectURL).toHaveBeenCalledWith("blob:macro-export");
  });
});
