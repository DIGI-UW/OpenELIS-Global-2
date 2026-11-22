import React from "react";
import { render } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { describe, expect, it, vi } from "vitest";
import messages from "../../../languages/en.json";
import ReportByDateCSV from "./ReportByDateCSV";

vi.mock("../../utils/Utils", async (importOriginal) => ({
  ...(await importOriginal()),
  getFromOpenElisServer: vi.fn((url, callback) => callback([])),
}));

/**
 * React reports a warning once per run, so this check renders the form in a
 * file of its own: before any date type is chosen, the date type select is
 * still controlled and React has nothing to warn about.
 */
describe("ReportByDateCSV first render", () => {
  it("renders the study export without React warnings", () => {
    const errors = vi.spyOn(console, "error").mockImplementation(() => {});
    render(
      <IntlProvider locale="en" messages={messages}>
        <ReportByDateCSV
          report="CIStudyExport"
          id="header.label.study.ciexport"
        />
      </IntlProvider>,
    );

    expect(errors.mock.calls.map((call) => String(call[0]))).toEqual([]);
    errors.mockRestore();
  });
});
