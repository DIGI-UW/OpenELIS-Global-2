import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";
import { vi } from "vitest";
import messages from "../../../../languages/en.json";
import ControlLotSetup from "../ControlLotSetup";
import { getFromOpenElisServer } from "../../../utils/Utils";

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerFullResponse: vi.fn(),
}));

// Rendering against the real en.json also fails loudly on a missing i18n key —
// react-intl falls back to the raw key, which would break these assertions.
const renderSetup = () =>
  render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        <ControlLotSetup />
      </IntlProvider>
    </BrowserRouter>,
  );

describe("ControlLotSetup — bench control lots (OGC-1147 FR-B3)", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/analyzer/analyzers")) {
        callback({ analyzers: [{ id: "7", name: "Cobas c111" }] });
      } else if (url.includes("ALL_TESTS")) {
        callback([{ id: "42", value: "Glucose" }]);
      }
    });
  });

  test("defaults the analyzer field to the explicit bench choice", async () => {
    renderSetup();

    // The dropdown shows what will actually be submitted, so a lab admin can tell a
    // manual bench lot apart from a required field they forgot to fill in.
    expect(
      await screen.findByText("Bench / no analyzer (manual method)"),
    ).toBeInTheDocument();
    expect(
      screen.getByText(
        'Optional. Choose "Bench / no analyzer" for a manual method run at the bench without an instrument.',
      ),
    ).toBeInTheDocument();
  });

  test("offers the real analyzers alongside the bench choice", async () => {
    renderSetup();

    await userEvent.click(
      await screen.findByText("Bench / no analyzer (manual method)"),
    );

    expect(await screen.findByText("Cobas c111")).toBeInTheDocument();
  });

  test("submitting with no analyzer is not a validation error, unlike the other required fields", async () => {
    renderSetup();

    await userEvent.click(await screen.findByText("Save"));

    // Inversion of the removed Yup rule: the sibling required-field messages still
    // fire on the same submit, so the absence below is the rule being gone rather
    // than validation not having run.
    expect(
      await screen.findByText("Lot number is required"),
    ).toBeInTheDocument();
    expect(screen.getByText("Test is required")).toBeInTheDocument();
    expect(screen.queryByText("Analyzer is required")).not.toBeInTheDocument();
  });
});
