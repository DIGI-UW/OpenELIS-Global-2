/**
 * The shipment dashboard's facility filters are searched by typing (OGC-1515),
 * and a chosen facility still applies when a box state is chosen too.
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
import ShipmentDashboard from "../ShipmentDashboard";

vi.mock("../../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return { ...actual, getFromOpenElisServer: vi.fn() };
});

vi.mock("../ShipmentNavigation", () => ({ default: () => null }));

const FACILITIES = [
  { id: "7", value: "Laboratoire Régional Nord" },
  { id: "8", value: "CAMES MAN" },
  { id: "9", value: "Central Lab" },
];

const box = (id, boxId, state, facilityId, facilityName) => ({
  id,
  boxId,
  state,
  destinationFacilityId: facilityId,
  destinationFacilityName: facilityName,
  sampleCount: 1,
  createdDate: "2026-11-01T10:00:00",
});

const BOXES = [
  box(1, "BOX-NORD-SENT", "SENT", 7, "Laboratoire Régional Nord"),
  box(2, "BOX-CAMES-SENT", "SENT", 8, "CAMES MAN"),
  box(3, "BOX-NORD-DRAFT", "DRAFT", 7, "Laboratoire Régional Nord"),
];

const addNotification = vi.fn();

const renderDashboard = (path = "/SampleShipment") =>
  render(
    <MemoryRouter initialEntries={[path]}>
      <NotificationContext.Provider value={{ addNotification }}>
        <IntlProvider locale="en" messages={messages}>
          <ShipmentDashboard />
        </IntlProvider>
      </NotificationContext.Provider>
    </MemoryRouter>,
  );

const optionsOf = (combobox) =>
  within(document.getElementById(combobox.getAttribute("aria-controls")))
    .queryAllByRole("option")
    .map((option) => option.textContent);

const choose = async (combobox, typed, name) => {
  await userEvent.clear(combobox);
  await userEvent.type(combobox, typed);
  await userEvent.click(
    within(
      document.getElementById(combobox.getAttribute("aria-controls")),
    ).getByRole("option", { name }),
  );
};

describe("Shipment dashboard facility filters", () => {
  beforeEach(() => {
    addNotification.mockReset();
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === "/rest/displayList/REFERRAL_ORGANIZATIONS")
        return callback(FACILITIES);
      if (url === "/rest/shipping-box") return callback(BOXES);
      if (url.startsWith("/rest/shipping-box/by-state/"))
        return callback(BOXES.filter((b) => b.state === url.split("/").pop()));
      if (url.startsWith("/rest/shipping-box/by-facility/"))
        return callback(
          BOXES.filter(
            (b) => String(b.destinationFacilityId) === url.split("/").pop(),
          ),
        );
      if (url === "/rest/shipping-box/statistics") return callback({});
      return callback([]);
    });
  });

  it("narrows the facilities to what is typed, ignoring case and accents, and shows All until one is chosen", async () => {
    renderDashboard();
    const facility = await screen.findByRole("combobox", {
      name: "Filter by Facility",
    });

    expect(facility).toHaveAttribute("placeholder", "All");
    await userEvent.click(facility);
    expect(optionsOf(facility)).toEqual([
      "Laboratoire Régional Nord",
      "CAMES MAN",
      "Central Lab",
    ]);

    await userEvent.type(facility, "REGIONAL");
    expect(optionsOf(facility)).toEqual(["Laboratoire Régional Nord"]);
  });

  it("lists only the chosen facility's boxes, also when a state is chosen", async () => {
    renderDashboard();
    await screen.findByText("BOX-CAMES-SENT");

    await choose(
      screen.getByRole("combobox", { name: "Filter by Facility" }),
      "nord",
      "Laboratoire Régional Nord",
    );
    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/shipping-box/by-facility/7",
      expect.any(Function),
    );
    expect(await screen.findByText("BOX-NORD-DRAFT")).toBeInTheDocument();
    expect(screen.queryByText("BOX-CAMES-SENT")).not.toBeInTheDocument();

    await userEvent.click(
      screen.getByRole("combobox", { name: "Filter by State" }),
    );
    await userEvent.click(screen.getByRole("option", { name: "Sent" }));

    expect(await screen.findByText("BOX-NORD-SENT")).toBeInTheDocument();
    expect(screen.queryByText("BOX-CAMES-SENT")).not.toBeInTheDocument();
    expect(screen.queryByText("BOX-NORD-DRAFT")).not.toBeInTheDocument();
  });

  it("filters the unassigned samples by the facility typed", async () => {
    renderDashboard("/SampleShipment/unassigned");
    const facility = await screen.findByRole("combobox", {
      name: "Filter by Facility",
    });

    await choose(facility, "cames", "CAMES MAN");

    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/unassigned-sample/items/by-facility/8",
      expect.any(Function),
    );
  });

  it("empties the list and says so when the facility's samples cannot be loaded", async () => {
    const unassigned = [
      {
        sampleItemId: "5",
        accessionNumber: "DEV-UNASSIGNED-1",
        typeOfSample: "Serum",
        referralTests: [],
      },
    ];
    const answer = getFromOpenElisServer.getMockImplementation();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === "/rest/unassigned-sample/items") return callback(unassigned);
      if (url.startsWith("/rest/unassigned-sample/items/by-facility/"))
        return callback(undefined);
      return answer(url, callback);
    });
    renderDashboard("/SampleShipment/unassigned");
    expect(await screen.findByText("DEV-UNASSIGNED-1")).toBeInTheDocument();

    await choose(
      screen.getByRole("combobox", { name: "Filter by Facility" }),
      "cames",
      "CAMES MAN",
    );

    expect(screen.queryByText("DEV-UNASSIGNED-1")).not.toBeInTheDocument();
    expect(addNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        kind: "error",
        message: messages["shipment.error.fetchUnassignedSamples"],
      }),
    );
  });

  it("empties the box list and says so when the boxes cannot be loaded", async () => {
    const answer = getFromOpenElisServer.getMockImplementation();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.startsWith("/rest/shipping-box/by-facility/"))
        return callback(undefined);
      return answer(url, callback);
    });
    renderDashboard();
    await screen.findByText("BOX-CAMES-SENT");

    await choose(
      screen.getByRole("combobox", { name: "Filter by Facility" }),
      "cames",
      "CAMES MAN",
    );

    expect(screen.queryByText("BOX-CAMES-SENT")).not.toBeInTheDocument();
    expect(addNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        kind: "error",
        message: messages["shipment.error.fetchBoxes"],
      }),
    );
  });
});
