import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../../languages/en.json";
import { getFromOpenElisServer } from "../../utils/Utils";
import { ConfigurationContext, NotificationContext } from "../../layout/Layout";
import ShipmentReport from "../ShipmentReport";

vi.mock("../../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return { ...actual, getFromOpenElisServer: vi.fn() };
});

vi.mock("../ShipmentNavigation", () => ({ default: () => null }));

const BOXES = [
  {
    id: 1,
    boxId: "BOX-OCTOBER",
    state: "SENT",
    sampleCount: 1,
    createdDate: "2026-10-20T10:00:00",
  },
  {
    id: 2,
    boxId: "BOX-NOVEMBER",
    state: "SENT",
    sampleCount: 1,
    createdDate: "2026-11-08T10:00:00",
  },
];

const renderReport = () =>
  render(
    <MemoryRouter>
      <ConfigurationContext.Provider
        value={{ configurationProperties: { DEFAULT_DATE_LOCALE: "fr-FR" } }}
      >
        <NotificationContext.Provider value={{ addNotification: vi.fn() }}>
          <IntlProvider locale="en" messages={messages}>
            <ShipmentReport />
          </IntlProvider>
        </NotificationContext.Provider>
      </ConfigurationContext.Provider>
    </MemoryRouter>,
  );

describe("Shipment report dates on a day-first site", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) =>
      callback(url === "/rest/shipping-box" ? BOXES : []),
    );
  });

  it("reads a typed From date day first", async () => {
    renderReport();
    const from = document.getElementById("report-date-from");
    expect(from).toHaveAttribute("placeholder", "dd/mm/yyyy");
    expect(document.getElementById("report-date-to")).toHaveAttribute(
      "placeholder",
      "dd/mm/yyyy",
    );

    await userEvent.type(from, "05/11/2026{enter}");
    await userEvent.click(
      screen.getByRole("button", {
        name: messages["shipment.report.generate"],
      }),
    );

    expect(await screen.findByText("BOX-NOVEMBER")).toBeInTheDocument();
    expect(screen.queryByText("BOX-OCTOBER")).not.toBeInTheDocument();
  });
});
