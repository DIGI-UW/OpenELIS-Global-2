import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import DisplayOrderSection from "./DisplayOrderSection";
import messages from "../../../../languages/en.json";

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  putToOpenElisServer: vi.fn(),
}));

import {
  getFromOpenElisServer,
  putToOpenElisServer,
} from "../../../utils/Utils";

const TYPES = [
  { id: "1", name: "Serum", isActive: true, sortOrder: 1 },
  { id: "2", name: "Urine", isActive: true, sortOrder: 2 },
  { id: "3", name: "Plasma", isActive: false, sortOrder: 3 },
];

const renderSection = (sampleTypeId = "2") =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <DisplayOrderSection sampleTypeId={sampleTypeId} />
    </IntlProvider>,
  );

beforeEach(() => {
  vi.clearAllMocks();
  getFromOpenElisServer.mockImplementation((_url, cb) =>
    cb({ success: true, data: TYPES }),
  );
});

describe("DisplayOrderSection", () => {
  it("lists every sample type in order and highlights the current one", async () => {
    renderSection("2");

    expect(await screen.findByText("Serum")).toBeInTheDocument();
    expect(screen.getByText("Plasma")).toBeInTheDocument();

    const currentRow = screen.getByTestId("display-order-current-row");
    expect(currentRow).toHaveTextContent("Urine");
    expect(currentRow).toHaveTextContent("This sample type");

    // position input pre-filled with the current 1-based position
    expect(document.getElementById("st-display-order-position")).toHaveValue(2);
  });

  it("saves the typed position to the display-order endpoint and refreshes", async () => {
    putToOpenElisServer.mockImplementation((_url, _body, cb) => cb(200));
    renderSection("2");
    await screen.findByText("Urine");

    fireEvent.change(document.getElementById("st-display-order-position"), {
      target: { value: "1" },
    });
    fireEvent.click(document.getElementById("st-display-order-save"));

    expect(
      await screen.findByText("Display order updated."),
    ).toBeInTheDocument();
    expect(putToOpenElisServer).toHaveBeenCalledWith(
      "/rest/sample-types/2/display-order",
      JSON.stringify({ position: 1 }),
      expect.any(Function),
    );
    // one initial load + one refresh after save
    expect(getFromOpenElisServer).toHaveBeenCalledTimes(2);
  });

  it("surfaces an error when the save fails", async () => {
    putToOpenElisServer.mockImplementation((_url, _body, cb) => cb(500));
    renderSection("2");
    await screen.findByText("Urine");

    fireEvent.click(document.getElementById("st-display-order-save"));

    expect(
      await screen.findByText("Could not update display order."),
    ).toBeInTheDocument();
  });
});
