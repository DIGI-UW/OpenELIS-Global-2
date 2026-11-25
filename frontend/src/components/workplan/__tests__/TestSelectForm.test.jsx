import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import TestSelectForm from "../TestSelectForm";
import PanelSelectForm from "../PanelSelectForm";
import TestSectionSelectForm from "../TestSectionSelectForm";
import { getFromOpenElisServer } from "../../utils/Utils";

vi.mock("../../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
  };
});

const renderInIntl = (ui) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      {ui}
    </IntlProvider>,
  );

const optionNames = () =>
  screen.queryAllByRole("option").map((option) => option.textContent);

/**
 * The workplan's test dropdown offers the tests the signed-in user may work
 * on. The server narrows that list to their Results lab units; asking for the
 * whole catalogue let a user choose a test they hold no lab unit for and land
 * on an empty workplan. The test, panel and lab unit lists are searched by
 * typing (OGC-1515).
 */
describe("Workplan test selection", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.startsWith("/rest/test-list")) {
        callback([
          { id: "10", value: "Haemoglobin" },
          { id: "11", value: "Platelet Count" },
          { id: "12", value: "Hématocrite" },
        ]);
      } else if (url.startsWith("/rest/displayList/PANELS")) {
        callback([
          { id: "1", value: "Bilan Biochimique" },
          { id: "2", value: "Sérologie VIH" },
        ]);
      } else if (url.startsWith("/rest/user-test-sections/")) {
        callback([
          { id: "56", value: "Hematology" },
          { id: "36", value: "Biochemistry" },
        ]);
      } else {
        callback([]);
      }
    });
  });

  afterEach(() => {
    vi.clearAllMocks();
    window.history.replaceState(null, "", "/");
  });

  it("offers the user's own tests, read from the lab-unit scoped list", async () => {
    renderInIntl(<TestSelectForm value={vi.fn()} title="Test Type" />);

    await userEvent.click(
      await screen.findByRole("combobox", { name: "Test Type" }),
    );

    expect(optionNames()).toEqual([
      "Haemoglobin",
      "Platelet Count",
      "Hématocrite",
    ]);
    const requested = getFromOpenElisServer.mock.calls.map(([url]) => url);
    expect(requested).toContain("/rest/test-list");
    expect(requested).not.toContain("/rest/displayList/ALL_TESTS");
  });

  it("narrows the tests to what is typed, ignoring case and accents, and reports the chosen test", async () => {
    const value = vi.fn();
    renderInIntl(<TestSelectForm value={value} title="Test Type" />);
    const box = await screen.findByRole("combobox", { name: "Test Type" });

    await userEvent.type(box, "hema");
    expect(optionNames()).toEqual(["Hématocrite"]);
    await userEvent.keyboard("{ArrowDown}{Enter}");

    expect(value).toHaveBeenLastCalledWith("12", "Hématocrite");
    expect(box).toHaveValue("Hématocrite");
  });

  it("preselects the test named in the address and reports it once loaded", async () => {
    window.history.replaceState(null, "", "/WorkPlanByTest?testId=11");
    const value = vi.fn();
    renderInIntl(<TestSelectForm value={value} title="Test Type" />);

    expect(
      await screen.findByRole("combobox", { name: "Test Type" }),
    ).toHaveValue("Platelet Count");
    expect(value).toHaveBeenCalledWith("11", "Platelet Count");
  });

  it("reports no test with the placeholder label until one is chosen, and again when cleared", async () => {
    const value = vi.fn();
    renderInIntl(<TestSelectForm value={value} title="Test Type" />);
    await screen.findByRole("combobox", { name: "Test Type" });

    expect(value).toHaveBeenLastCalledWith("", "Select Test Type");
    await userEvent.type(
      screen.getByRole("combobox", { name: "Test Type" }),
      "plat",
    );
    await userEvent.keyboard("{ArrowDown}{Enter}");
    expect(value).toHaveBeenLastCalledWith("11", "Platelet Count");
    await userEvent.click(screen.getByRole("button", { name: /clear/i }));

    expect(value).toHaveBeenLastCalledWith("", "Select Test Type");
  });

  it("searches the panels by typing", async () => {
    const value = vi.fn();
    renderInIntl(<PanelSelectForm value={value} title="Panel Type" />);
    const box = await screen.findByRole("combobox", { name: "Panel Type" });

    await userEvent.type(box, "serologie");
    expect(optionNames()).toEqual(["Sérologie VIH"]);
    await userEvent.keyboard("{ArrowDown}{Enter}");

    expect(value).toHaveBeenLastCalledWith("2", "Sérologie VIH");
  });

  it("searches the user's Results lab units by typing", async () => {
    const value = vi.fn();
    renderInIntl(<TestSectionSelectForm value={value} title="Unit Type" />);
    const box = await screen.findByRole("combobox", { name: "Unit Type" });

    await userEvent.type(box, "chem");
    expect(optionNames()).toEqual(["Biochemistry"]);
    await userEvent.keyboard("{ArrowDown}{Enter}");

    expect(value).toHaveBeenLastCalledWith("36", "Biochemistry");
    expect(getFromOpenElisServer.mock.calls[0][0]).toBe(
      "/rest/user-test-sections/Results",
    );
  });
});
