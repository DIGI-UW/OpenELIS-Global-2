/**
 * The new-tab helper the Reports section and the sign-out use: the tab is
 * opened inside the click, cut off from this page, and pointed at the file
 * once it exists.
 */
import { afterEach, describe, expect, it, vi } from "vitest";
import config from "../../config.json";
import { openPendingTab, reportFileUrl } from "./reportTab";

const fakeTab = () => {
  const page = document.implementation.createHTMLDocument("");
  return {
    opener: window,
    document: page,
    location: { replace: vi.fn() },
    close: vi.fn(),
  };
};

describe("reportTab", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("theTab_isOpenedAtOnce_andLosesItsOpener", () => {
    const tab = fakeTab();
    const open = vi.spyOn(window, "open").mockReturnValue(tab);
    const text = "Preparing the report <b>now</b>";

    const pending = openPendingTab(text);

    // Opened before the helper returns: nothing was awaited first.
    expect(open).toHaveBeenCalledTimes(1);
    expect(open).toHaveBeenCalledWith("", "_blank");
    expect(pending.blocked).toBe(false);
    expect(tab.opener).toBeNull();
    expect(tab.document.title).toBe(text);
    // The text is written as text: markup in it stays words, never an element.
    expect(tab.document.body.textContent).toBe(text);
    expect(tab.document.body.children).toHaveLength(0);
    expect(tab.document.body.childNodes).toHaveLength(1);
    expect(tab.document.body.firstChild.nodeType).toBe(Node.TEXT_NODE);
  });

  it("show_replacesTheLocation_andClose_closes", () => {
    const tab = fakeTab();
    vi.spyOn(window, "open").mockReturnValue(tab);

    const pending = openPendingTab("Preparing the report");
    pending.show(
      "/api/OpenELIS-Global/rest/pathology/caseView/7/reports/3/file",
    );

    expect(tab.location.replace).toHaveBeenCalledWith(
      "/api/OpenELIS-Global/rest/pathology/caseView/7/reports/3/file",
    );
    expect(tab.close).not.toHaveBeenCalled();

    pending.close();
    expect(tab.close).toHaveBeenCalledTimes(1);
  });

  it("aBlockedPopup_isReported_andNothingThrows", () => {
    vi.spyOn(window, "open").mockReturnValue(null);

    const pending = openPendingTab("Preparing the report");

    expect(pending.blocked).toBe(true);
    expect(() => pending.show("/somewhere")).not.toThrow();
    expect(() => pending.close()).not.toThrow();

    // A browser that refuses by throwing is reported the same way.
    window.open.mockImplementation(() => {
      throw new Error("blocked");
    });
    let thrown;
    expect(() => {
      thrown = openPendingTab("Preparing the report");
    }).not.toThrow();
    expect(thrown.blocked).toBe(true);
  });

  it("theFileUrl_isTheCaseAndTheRow", () => {
    expect(reportFileUrl(7, 31)).toBe(
      `${config.serverBaseUrl}/rest/pathology/caseView/7/reports/31/file`,
    );
    expect(reportFileUrl("12", "4")).toBe(
      "/api/OpenELIS-Global/rest/pathology/caseView/12/reports/4/file",
    );
  });
});
