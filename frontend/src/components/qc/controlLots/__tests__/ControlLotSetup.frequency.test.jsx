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

const renderSetup = () =>
  render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        <ControlLotSetup />
      </IntlProvider>
    </BrowserRouter>,
  );

describe("ControlLotSetup: QC frequency rule (OGC-427)", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/analyzer/analyzers")) {
        callback({ analyzers: [] });
      } else if (url.includes("ALL_TESTS")) {
        callback([{ id: "42", value: "Glucose" }]);
      }
    });
  });

  test("a new lot defaults to daily QC, enforced", async () => {
    renderSetup();

    expect(await screen.findByText("Daily")).toBeInTheDocument();
    expect(screen.getByText("QC required before release")).toBeInTheDocument();
    expect(screen.queryByLabelText("QC required every (hours)")).toBeNull();
  });

  test("every N hours asks for the hours and refuses a save without them", async () => {
    renderSetup();

    await userEvent.click(await screen.findByText("Daily"));
    await userEvent.click(await screen.findByText("Every N hours"));
    expect(
      screen.getByLabelText("QC required every (hours)"),
    ).toBeInTheDocument();

    await userEvent.click(screen.getByText("Save"));

    expect(
      await screen.findByText(
        "Enter a whole number of hours greater than zero.",
      ),
    ).toBeInTheDocument();
  });

  test("each shift shows the shift length, eight hours by default", async () => {
    renderSetup();

    await userEvent.click(await screen.findByText("Daily"));
    await userEvent.click(await screen.findByText("Each shift"));

    expect(screen.getByLabelText("Shift length (hours)")).toHaveValue(8);
  });
});
