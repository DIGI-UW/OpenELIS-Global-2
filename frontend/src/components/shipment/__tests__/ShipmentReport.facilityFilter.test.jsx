/**
 * The shipment report's Reference Lab filter is searched by typing (OGC-1515)
 * and the report lists only the boxes sent to the lab chosen.
 */
import React from "react";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../../languages/en.json";
import { getFromOpenElisServer } from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import ShipmentReport from "../ShipmentReport";

vi.mock("../../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return { ...actual, getFromOpenElisServer: vi.fn() };
});

vi.mock("../ShipmentNavigation", () => ({ default: () => null }));

const LABS = [
  { id: "7", value: "Laboratoire Régional Nord" },
  { id: "8", value: "CAMES MAN" },
];

const BOXES = [
  {
    id: 1,
    boxId: "BOX-NORD",
    state: "SENT",
    destinationFacilityId: 7,
    destinationFacilityName: "Laboratoire Régional Nord",
    sampleCount: 1,
    createdDate: "2026-11-01T10:00:00",
  },
  {
    id: 2,
    boxId: "BOX-CAMES",
    state: "SENT",
    destinationFacilityId: 8,
    destinationFacilityName: "CAMES MAN",
    sampleCount: 1,
    createdDate: "2026-11-01T10:00:00",
  },
];

const renderReport = () =>
  render(
    <MemoryRouter>
      <NotificationContext.Provider value={{ addNotification: vi.fn() }}>
        <IntlProvider locale="en" messages={messages}>
          <ShipmentReport />
        </IntlProvider>
      </NotificationContext.Provider>
    </MemoryRouter>,
  );

describe("Shipment report Reference Lab filter", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === "/rest/displayList/REFERRAL_ORGANIZATIONS")
        return callback(LABS);
      if (url === "/rest/shipping-box") return callback(BOXES);
      return callback([]);
    });
  });

  it("narrows the labs to what is typed and reports only that lab's boxes", async () => {
    renderReport();
    const lab = screen.getByRole("combobox", { name: "Reference Lab" });
    expect(lab).toHaveAttribute("placeholder", "All");

    await userEvent.type(lab, "REGIONAL");
    expect(
      within(document.getElementById(lab.getAttribute("aria-controls")))
        .getAllByRole("option")
        .map((option) => option.textContent),
    ).toEqual(["Laboratoire Régional Nord"]);
    await userEvent.click(
      screen.getByRole("option", { name: "Laboratoire Régional Nord" }),
    );
    await userEvent.click(
      screen.getByRole("button", {
        name: messages["shipment.report.generate"],
      }),
    );

    expect(await screen.findByText("BOX-NORD")).toBeInTheDocument();
    expect(screen.queryByText("BOX-CAMES")).not.toBeInTheDocument();
  });

  it("clears the lab with the other filters", async () => {
    renderReport();
    const lab = screen.getByRole("combobox", { name: "Reference Lab" });
    await userEvent.type(lab, "cames");
    await userEvent.click(screen.getByRole("option", { name: "CAMES MAN" }));
    expect(lab).toHaveValue("CAMES MAN");

    await userEvent.click(
      screen.getByRole("button", { name: messages["shipment.filter.clear"] }),
    );
    expect(lab).toHaveValue("");
  });
});
