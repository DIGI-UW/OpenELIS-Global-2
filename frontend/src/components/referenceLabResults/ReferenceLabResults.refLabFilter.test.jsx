/**
 * The Reference Lab filter is searched by typing (OGC-1515) and keeps only the
 * referrals sent to the lab chosen; clearing it lists them all again.
 */
import React from "react";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
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

const LABS = [
  { id: "9000101", value: "Laboratoire Régional Nord" },
  { id: "9000102", value: "CAMES MAN" },
];

const referral = (id, labNumber, labId, labName) => ({
  id,
  labNumber,
  referenceLabId: labId,
  referenceLabName: labName,
  testName: "Red Blood Cells Count (RBC)",
  priority: "ROUTINE",
  sentDate: "2026-11-01T10:00:00",
  daysOutstanding: 2,
});

const REFERRALS = [
  referral(1, "DEV-NORD-1", "9000101", "Laboratoire Régional Nord"),
  referral(2, "DEV-CAMES-1", "9000102", "CAMES MAN"),
];

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

const optionsOf = (combobox) =>
  within(document.getElementById(combobox.getAttribute("aria-controls")))
    .queryAllByRole("option")
    .map((option) => option.textContent);

describe("Reference Lab filter", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === "/rest/displayList/REFERRAL_ORGANIZATIONS")
        return callback(LABS);
      if (url.startsWith("/rest/reference-lab-results/referrals"))
        return callback(REFERRALS);
      if (url === "/rest/reference-lab-results/metrics")
        return callback({ outstanding: 2 });
      return callback([]);
    });
  });

  it("narrows the labs to what is typed and keeps only that lab's referrals", async () => {
    renderPage();
    await screen.findByText("DEV-CAMES-1");
    const lab = screen.getByRole("combobox", { name: "Reference Lab" });
    expect(lab).toHaveAttribute("placeholder", "All reference labs");

    await userEvent.type(lab, "regional");
    expect(optionsOf(lab)).toEqual(["Laboratoire Régional Nord"]);
    await userEvent.click(
      screen.getByRole("option", { name: "Laboratoire Régional Nord" }),
    );

    expect(screen.getByText("DEV-NORD-1")).toBeInTheDocument();
    expect(screen.queryByText("DEV-CAMES-1")).not.toBeInTheDocument();
  });

  it("lists every referral again when the filters are cleared", async () => {
    renderPage();
    await screen.findByText("DEV-CAMES-1");
    const lab = screen.getByRole("combobox", { name: "Reference Lab" });
    await userEvent.type(lab, "cames");
    await userEvent.click(screen.getByRole("option", { name: "CAMES MAN" }));
    expect(screen.queryByText("DEV-NORD-1")).not.toBeInTheDocument();

    await userEvent.click(
      screen.getByRole("button", {
        name: messages["referral.action.clearFilters"],
      }),
    );

    expect(lab).toHaveValue("");
    expect(screen.getByText("DEV-NORD-1")).toBeInTheDocument();
    expect(screen.getByText("DEV-CAMES-1")).toBeInTheDocument();
  });
});
