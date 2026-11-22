import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { describe, expect, it, vi } from "vitest";
import messages from "../../../languages/en.json";
import { NotificationContext } from "../../layout/Layout";
import ReportByDateCSV from "./ReportByDateCSV";

vi.mock("../../utils/Utils", async (importOriginal) => ({
  ...(await importOriginal()),
  getFromOpenElisServer: vi.fn((url, callback) =>
    callback(
      url === "/rest/studyprojects"
        ? [{ id: "26", value: "Routine HIV Testing" }]
        : [],
    ),
  ),
}));

const firstOnChange = {};

vi.mock("../../common/CustomDatePicker", () => ({
  default: ({ id, onChange }) => {
    if (!firstOnChange[id]) firstOnChange[id] = onChange;
    return <input id={id} readOnly />;
  },
}));

/**
 * Carbon's DatePicker (flatpickr) keeps the change handler it was first given,
 * so a date picked after the study and the date type reached the form through
 * a handler from the first render. It used to rebuild the form from that
 * render's values and drop both choices ("Please select date type." on a form
 * where one was chosen).
 */
describe("ReportByDateCSV dates picked last", () => {
  it("keeps the study and date type chosen before the dates", async () => {
    const open = vi.spyOn(window, "open").mockImplementation(() => null);
    render(
      <NotificationContext.Provider
        value={{
          notifications: [],
          addNotification: vi.fn(),
          removeNotification: vi.fn(),
        }}
      >
        <IntlProvider locale="en" messages={messages}>
          <ReportByDateCSV
            report="CIStudyExport"
            id="header.label.study.ciexport"
          />
        </IntlProvider>
      </NotificationContext.Provider>,
    );
    const study = screen.getByRole("combobox", { name: "Select Study Type" });
    await userEvent.type(study, "routine");
    await userEvent.click(
      screen.getByRole("option", { name: "Routine HIV Testing" }),
    );
    await userEvent.selectOptions(
      document.getElementById("dateType"),
      "RESULT_DATE",
    );

    firstOnChange.startDate("10/01/2026");
    firstOnChange.endDate("11/04/2026");
    await userEvent.click(
      screen.getByRole("button", { name: /Generate Printable Version/ }),
    );

    expect(open).toHaveBeenCalledTimes(1);
    const url = new URL(open.mock.calls[0][0], "https://placeholder.invalid/");
    expect(url.searchParams.get("projectCode")).toBe("26");
    expect(url.searchParams.get("dateType")).toBe("RESULT_DATE");
    expect(url.searchParams.get("upperDateRange")).toBeTruthy();
    open.mockRestore();
  });
});
