import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { describe, expect, it, vi } from "vitest";
import messages from "../../../languages/en.json";
import { NotificationContext } from "../../layout/Layout";
import ReportByDate from "./ReportByDate";

vi.mock("../../utils/Utils", async (importOriginal) => ({
  ...(await importOriginal()),
  getFromOpenElisServer: vi.fn((url, callback) =>
    callback(
      url === "/rest/test-list" ? [{ id: "44", value: "Créatinine" }] : [],
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
 * Carbon's DatePicker (flatpickr) keeps the change handler it was first given.
 * An activity report whose test was chosen before its dates used to rebuild
 * the form from the first render's values, drop the test, and refuse with
 * "Please select a value."
 */
describe("ReportByDate dates picked last", () => {
  it("keeps the test chosen before the dates", async () => {
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
          <ReportByDate report="activityReportByTest" />
        </IntlProvider>
      </NotificationContext.Provider>,
    );
    const test = screen.getByRole("combobox", {
      name: messages["label.form.searchby"],
    });
    await userEvent.type(test, "creat");
    await userEvent.click(screen.getByRole("option", { name: "Créatinine" }));

    firstOnChange.startDate("10/01/2026");
    firstOnChange.endDate("11/04/2026");
    await userEvent.click(
      screen.getByRole("button", { name: /Generate Printable Version/ }),
    );

    expect(open).toHaveBeenCalledTimes(1);
    expect(open.mock.calls[0][0]).toContain("selectList.selection=44");
    open.mockRestore();
  });
});
