import React from "react";
import { fireEvent, render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { describe, expect, it, vi } from "vitest";
import messages from "../../../../languages/en.json";
import TATFilterBar from "../TATFilterBar";

vi.mock("../../../utils/Utils", async (importOriginal) => ({
  ...(await importOriginal()),
  getFromOpenElisServer: vi.fn((url, callback) => {
    if (url === "/rest/displayList/TEST_SECTION_ACTIVE") {
      callback([
        { id: "101", value: "Hématologie" },
        { id: "102", value: "Biochimie" },
      ]);
    } else if (url === "/rest/displayList/ALL_TESTS") {
      callback([{ id: "201", value: "Créatinine" }]);
    } else if (url === "/rest/user-sample-types") {
      callback([
        { id: "301", value: "Sérum" },
        { id: "302", value: "Sang total" },
      ]);
    } else if (url === "/rest/displayList/SAMPLE_PATIENT_REFERRING_CLINIC") {
      callback([
        { id: "401", value: "Centre de Santé Nord" },
        { id: "402", value: "Hôpital Régional" },
      ]);
    } else {
      callback([]);
    }
  }),
}));

const optionsOf = (combobox) =>
  within(document.getElementById(combobox.getAttribute("aria-controls")))
    .queryAllByRole("option")
    .map((option) => option.textContent);

const renderBar = () => {
  const onGenerate = vi.fn();
  render(
    <IntlProvider locale="en" messages={messages}>
      <TATFilterBar onGenerate={onGenerate} />
    </IntlProvider>,
  );
  return onGenerate;
};

const generate = () =>
  userEvent.click(screen.getByTestId("generate-report-button"));

/**
 * The TAT report's sample type and ordering site are searched by typing,
 * ignoring accents (OGC-1515), and Clear Filters empties every filter on
 * screen as well as in the report it generates.
 */
describe("TATFilterBar search and clear", () => {
  it("narrows the sample types and ordering sites to what is typed and reports the chosen ids", async () => {
    const onGenerate = renderBar();
    const sampleType = screen.getByRole("combobox", {
      name: messages["reports.tat.sampleType"],
    });
    const site = screen.getByRole("combobox", {
      name: messages["reports.tat.orderingSite"],
    });

    await userEvent.type(sampleType, "serum");
    expect(optionsOf(sampleType)).toEqual(["Sérum"]);
    await userEvent.click(screen.getByRole("option", { name: "Sérum" }));

    await userEvent.type(site, "hopital");
    expect(optionsOf(site)).toEqual(["Hôpital Régional"]);
    await userEvent.click(
      screen.getByRole("option", { name: "Hôpital Régional" }),
    );

    await generate();
    expect(onGenerate.mock.calls[0][0]).toMatchObject({
      sampleTypeId: "301",
      orderingSiteId: "402",
    });
  });

  it("clear filters empties the chosen sample type, site and lab units on screen", async () => {
    const onGenerate = renderBar();
    const sampleType = screen.getByRole("combobox", {
      name: messages["reports.tat.sampleType"],
    });
    const site = screen.getByRole("combobox", {
      name: messages["reports.tat.orderingSite"],
    });
    const labUnit = document.getElementById("tat-lab-unit-input");

    await userEvent.type(sampleType, "sang");
    await userEvent.click(screen.getByRole("option", { name: "Sang total" }));
    await userEvent.type(site, "nord");
    await userEvent.click(
      screen.getByRole("option", { name: "Centre de Santé Nord" }),
    );
    fireEvent.click(labUnit);
    fireEvent.click(await screen.findByRole("option", { name: "Biochimie" }));
    expect(
      await screen.findByRole("button", { name: /clear all selected items/i }),
    ).toBeInTheDocument();

    await userEvent.click(
      screen.getByRole("button", {
        name: messages["reports.tat.clearFilters"],
      }),
    );

    expect(sampleType).toHaveValue("");
    expect(site).toHaveValue("");
    expect(
      screen.queryByRole("button", { name: /clear all selected items/i }),
    ).toBeNull();
    await generate();
    expect(onGenerate.mock.calls[0][0]).toMatchObject({
      sampleTypeId: "",
      orderingSiteId: "",
      labUnitIds: [],
    });
  });
});
