import React from "react";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { afterEach, describe, expect, it, vi } from "vitest";
import messages from "../../../languages/en.json";
import { getFromOpenElisServer } from "../../utils/Utils";
import ReportByDateCSV from "./ReportByDateCSV";

vi.mock("../../utils/Utils", async (importOriginal) => ({
  ...(await importOriginal()),
  getFromOpenElisServer: vi.fn((url, callback) => {
    if (url === "/rest/studyprojects") {
      callback([
        { id: "1", value: "Étude ANC" },
        { id: "2", value: "Routine Testing" },
      ]);
    } else if (url === "/rest/trendsprojects") {
      callback([{ id: "VL", value: "Charge virale" }]);
    } else {
      callback([]);
    }
  }),
}));

const renderReport = (report) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <ReportByDateCSV report={report} id="header.label.study.ciexport" />
    </IntlProvider>,
  );

const optionsOf = (combobox) =>
  within(document.getElementById(combobox.getAttribute("aria-controls")))
    .queryAllByRole("option")
    .map((option) => option.textContent);

/**
 * The study export's study list is searched by typing, ignoring accents
 * (OGC-1515), and its date types read from the message bundle.
 */
describe("ReportByDateCSV", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it("narrows the studies to what is typed, ignoring accents", async () => {
    renderReport("CIStudyExport");
    const study = screen.getByRole("combobox", { name: "Select Study Type" });

    await userEvent.type(study, "etude");
    expect(optionsOf(study)).toEqual(["Étude ANC"]);
    await userEvent.click(screen.getByRole("option", { name: "Étude ANC" }));
    expect(study).toHaveValue("Étude ANC");
  });

  it("lists the viral load studies for the specific export", async () => {
    renderReport("Trends");
    const study = screen.getByRole("combobox", { name: "Select Study Type" });

    await userEvent.click(study);
    expect(optionsOf(study)).toEqual(["Charge virale"]);
    expect(screen.queryByLabelText(/date type/i)).toBeNull();
  });

  it("offers the date types from the message bundle", () => {
    renderReport("CIStudyExport");
    const dateType = document.getElementById("dateType");

    expect(Array.from(dateType.options).map((o) => o.text)).toEqual([
      messages["report.csv.dateType.select"],
      messages["report.csv.dateType.ORDER_DATE"],
      messages["report.csv.dateType.RESULT_DATE"],
      messages["report.csv.dateType.PRINT_DATE"],
    ]);
  });
});
