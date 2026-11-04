import React from "react";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../languages/en.json";
import { getFromOpenElisServer } from "../utils/Utils";
import SampleType from "./SampleType";

vi.mock("../utils/Utils", async (importOriginal) => ({
  ...(await importOriginal()),
  getFromOpenElisServer: vi.fn(),
}));

/**
 * Batch Order Entry's sample type, panels and tests are searched by typing,
 * ignoring accents (OGC-1515), and the setup form always holds the selection
 * on screen: a changed sample type drops the previous type's tests, and an
 * unticked panel keeps tests another panel or a hand tick still asks for.
 */
describe("Batch Order Entry sample type", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === "/rest/user-sample-types") {
        callback([
          { id: "2", value: "Serum" },
          { id: "3", value: "Sang total" },
        ]);
      } else if (url === "/rest/sample-type-tests?sampleType=2") {
        callback({
          tests: [
            { id: "4", name: "Creatinine" },
            { id: "1", name: "GPT/ALAT" },
            { id: "6", name: "Glycémie" },
          ],
          panels: [
            { id: "9", name: "Bilan Biochimique", testIds: "1, 4" },
            { id: "10", name: "Bilan Hépatique", testIds: "1" },
          ],
        });
      } else if (url === "/rest/sample-type-tests?sampleType=3") {
        callback({ tests: [{ id: "7", name: "Hémoglobine" }], panels: [] });
      } else {
        callback([]);
      }
    });
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  const renderSampleType = () => {
    const updateFormValues = vi.fn();
    render(
      <IntlProvider locale="en" messages={messages}>
        <SampleType updateFormValues={updateFormValues} />
      </IntlProvider>,
    );
    return updateFormValues;
  };

  const lastReport = (updateFormValues) =>
    updateFormValues.mock.calls[updateFormValues.mock.calls.length - 1][0];

  const chooseSampleType = async (name) => {
    const box = screen.getByRole("combobox", { name: "Sample Type" });
    await userEvent.clear(box);
    await userEvent.type(box, name.slice(0, 3));
    await userEvent.click(screen.getByRole("option", { name }));
  };

  const optionsOf = (combobox) =>
    within(document.getElementById(combobox.getAttribute("aria-controls")))
      .queryAllByRole("option")
      .map((option) => option.textContent);

  it("narrows the sample types, panels and tests to what is typed, ignoring accents", async () => {
    renderSampleType();
    const box = screen.getByRole("combobox", { name: "Sample Type" });
    await userEvent.type(box, "sang");
    expect(optionsOf(box)).toEqual(["Sang total"]);
    await chooseSampleType("Serum");

    await userEvent.type(
      await screen.findByRole("searchbox", { name: "Search panels" }),
      "hepa",
    );
    expect(screen.getByLabelText("Bilan Hépatique")).toBeInTheDocument();
    expect(screen.queryByLabelText("Bilan Biochimique")).toBeNull();

    await userEvent.type(
      screen.getByRole("searchbox", { name: "Search tests" }),
      "glyce",
    );
    expect(screen.getByLabelText("Glycémie")).toBeInTheDocument();
    expect(screen.queryByLabelText("Creatinine")).toBeNull();
  });

  it("changing the sample type drops the previous type's tests from the form", async () => {
    const updateFormValues = renderSampleType();
    await chooseSampleType("Serum");
    await userEvent.click(await screen.findByLabelText("GPT/ALAT"));
    expect(lastReport(updateFormValues)).toMatchObject({
      selectedTests: [{ id: "1", value: "GPT/ALAT" }],
      sampleId: "2",
      sampleName: "Serum",
    });

    await chooseSampleType("Sang total");

    expect(lastReport(updateFormValues)).toMatchObject({
      selectedTests: [],
      selectedPanels: [],
    });
    expect(await screen.findByLabelText("Hémoglobine")).not.toBeChecked();
  });

  it("unticking a panel keeps tests another panel or a hand tick still asks for", async () => {
    const updateFormValues = renderSampleType();
    await chooseSampleType("Serum");

    await userEvent.click(await screen.findByLabelText("Glycémie"));
    await userEvent.click(screen.getByLabelText("Bilan Biochimique"));
    await userEvent.click(screen.getByLabelText("Bilan Hépatique"));
    expect(
      lastReport(updateFormValues)
        .selectedTests.map((test) => test.id)
        .sort(),
    ).toEqual(["1", "4", "6"]);

    await userEvent.click(screen.getByLabelText("Bilan Biochimique"));

    expect(screen.getByLabelText("GPT/ALAT")).toBeChecked();
    expect(screen.getByLabelText("Glycémie")).toBeChecked();
    expect(screen.getByLabelText("Creatinine")).not.toBeChecked();
    expect(lastReport(updateFormValues)).toMatchObject({
      selectedTests: [
        { id: "1", value: "GPT/ALAT" },
        { id: "6", value: "Glycémie" },
      ],
      selectedPanels: [{ id: "10", name: "Bilan Hépatique", testIds: "1" }],
      sampleId: "2",
    });
  });

  it("a test ticked before its panel is sent once", async () => {
    const updateFormValues = renderSampleType();
    await chooseSampleType("Serum");

    await userEvent.click(await screen.findByLabelText("GPT/ALAT"));
    await userEvent.click(screen.getByLabelText("Bilan Biochimique"));

    const ids = lastReport(updateFormValues).selectedTests.map(
      (test) => test.id,
    );
    expect(ids.sort()).toEqual(["1", "4"]);
  });
});
