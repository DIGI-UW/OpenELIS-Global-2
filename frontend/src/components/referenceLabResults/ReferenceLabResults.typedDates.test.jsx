/**
 * OGC-1635: a refused start date keeps the end of the range in view, and a
 * corrected start brings the whole range back.
 */
import React from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../languages/en.json";
import { getFromOpenElisServer } from "../utils/Utils";
import { NotificationContext } from "../layout/Layout";
import ReferenceLabResults from "./ReferenceLabResults";

vi.mock("../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return { ...actual, getFromOpenElisServer: vi.fn() };
});

vi.mock("../shipment/ShipmentNavigation", () => ({ default: () => null }));

const renderPage = () =>
  render(
    <MemoryRouter
      initialEntries={[
        "/SampleShipment/reference-lab-results?view=outstanding",
      ]}
    >
      <NotificationContext.Provider value={{ addNotification: vi.fn() }}>
        <IntlProvider locale="en" messages={messages}>
          <ReferenceLabResults />
        </IntlProvider>
      </NotificationContext.Provider>
    </MemoryRouter>,
  );

const type = (input, text) => {
  act(() => input.focus());
  fireEvent.click(input);
  fireEvent.input(input, { target: { value: text } });
};

const clickElsewhere = () => {
  document.body.dispatchEvent(new Event("touchstart", { bubbles: true }));
  document.body.dispatchEvent(new MouseEvent("mousedown", { bubbles: true }));
  document.activeElement?.blur();
};

describe("Reference Lab Results date range typed by hand", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) => callback([]));
  });

  it("keeps the end in view while the start is refused, and restores the range once the start is fixed", () => {
    renderPage();
    const from = document.getElementById("date-from");
    const to = document.getElementById("date-to");

    type(from, "03/01/2026");
    type(to, "03/09/2026");
    clickElsewhere();
    type(from, "1/1/20");
    clickElsewhere();

    expect(from).toHaveValue("1/1/20");
    expect(to).toHaveValue("03/09/2026");
    expect(
      screen.getAllByText("Enter a valid date as mm/dd/yyyy."),
    ).toHaveLength(1);

    type(from, "03/02/2026");
    clickElsewhere();

    expect(from).toHaveValue("03/02/2026");
    expect(to).toHaveValue("03/09/2026");
  });
});
