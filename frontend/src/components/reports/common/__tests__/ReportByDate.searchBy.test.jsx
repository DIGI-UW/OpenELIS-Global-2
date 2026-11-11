import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";
import ReportByDate from "../ReportByDate";
import { getFromOpenElisServer } from "../../../utils/Utils";

vi.mock("../../../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
  };
});

vi.mock("../../../common/CustomDatePicker", () => ({
  default: ({ id, onChange }) => (
    <button type="button" onClick={() => onChange("2026-11-03")}>
      {"set " + id}
    </button>
  ),
}));

vi.mock("../../../common/CustomNotification", () => ({
  AlertDialog: () => null,
}));

const optionNames = () =>
  screen.queryAllByRole("option").map((option) => option.textContent);

/**
 * The activity reports' Search By list is searched by typing (OGC-1515), and
 * the report opens for the entry chosen.
 */
describe("Activity report Search By", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === "/rest/test-list") {
        callback([
          { id: "10", value: "Haemoglobin" },
          { id: "12", value: "Hématocrite" },
          { id: "13", value: "Glucose" },
        ]);
      } else {
        callback([]);
      }
    });
    vi.spyOn(window, "open").mockImplementation(() => null);
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.clearAllMocks();
  });

  const renderReport = () =>
    render(
      <IntlProvider locale="en" messages={messages}>
        <ReportByDate report="activityReportByTest" />
      </IntlProvider>,
    );

  it("narrows the tests to what is typed and opens the report for the chosen one", async () => {
    renderReport();
    await userEvent.click(screen.getByRole("button", { name: "set startDate" }));
    await userEvent.click(screen.getByRole("button", { name: "set endDate" }));
    const box = await screen.findByRole("combobox", { name: "Search By" });

    await userEvent.type(box, "HEMA");
    expect(optionNames()).toEqual(["Hématocrite"]);
    await userEvent.keyboard("{ArrowDown}{Enter}");
    await userEvent.click(
      screen.getByRole("button", { name: "Generate Printable Version" }),
    );

    expect(window.open).toHaveBeenCalledTimes(1);
    const opened = new URL(window.open.mock.calls[0][0], "https://lab.test");
    expect(opened.searchParams.get("report")).toBe("activityReportByTest");
    expect(opened.searchParams.get("selectList.selection")).toBe("12");
  });

  it("still asks for a value when the choice is cleared", async () => {
    renderReport();
    await userEvent.click(screen.getByRole("button", { name: "set startDate" }));
    await userEvent.click(screen.getByRole("button", { name: "set endDate" }));
    const box = await screen.findByRole("combobox", { name: "Search By" });
    await userEvent.type(box, "gluc");
    await userEvent.keyboard("{ArrowDown}{Enter}");
    await userEvent.click(screen.getByRole("button", { name: /clear/i }));

    await userEvent.click(
      screen.getByRole("button", { name: "Generate Printable Version" }),
    );

    expect(window.open).not.toHaveBeenCalled();
    expect(screen.getByText("Please select a value.")).toBeInTheDocument();
  });
});
