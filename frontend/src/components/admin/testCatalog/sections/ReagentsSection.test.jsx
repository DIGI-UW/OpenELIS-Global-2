/**
 * ReagentsSection — OGC-949 M15 / OGC-991 + OGC-992 + OGC-993.
 *
 * Linked-reagents table with current stock (OGC-991), Link Reagent button
 * (OGC-992), and inline edit + unlink confirmation (OGC-993). Reads/writes the
 * OGC-987 endpoints.
 */

// ========== MOCKS (before imports) ==========
vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServer: vi.fn(),
  putToOpenElisServer: vi.fn(),
  deleteFromOpenElisServer: vi.fn(),
}));

// ========== IMPORTS ==========
import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import ReagentsSection from "./ReagentsSection";
import {
  getFromOpenElisServer,
  putToOpenElisServer,
  deleteFromOpenElisServer,
} from "../../../utils/Utils";
import messages from "../../../../languages/en.json";

const TWO_REAGENTS = [
  {
    id: "link-1",
    reagentId: 7,
    reagentName: "Glucose Reagent",
    manufacturer: "Acme",
    usageType: "PRIMARY",
    quantityPerTest: 2.5,
    quantityUnit: "mL",
    currentStock: 120,
    lowStockThreshold: 50,
  },
  {
    id: "link-2",
    reagentId: 9,
    reagentName: "Buffer Solution",
    manufacturer: "Acme",
    usageType: "SECONDARY",
    quantityPerTest: null,
    quantityUnit: null,
    currentStock: 0,
    lowStockThreshold: 10,
  },
];

const renderSection = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <ReagentsSection testId="42" />
    </IntlProvider>,
  );

beforeEach(() => {
  vi.clearAllMocks();
});

describe("ReagentsSection", () => {
  it("renders a row per linked reagent with stock and editable usage type", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => cb(TWO_REAGENTS));
    renderSection();

    expect(await screen.findByText("Glucose Reagent")).toBeInTheDocument();
    expect(screen.getByText("Buffer Solution")).toBeInTheDocument();
    // Stock counts (rendered inside the stock Tag).
    expect(screen.getByText("120")).toBeInTheDocument();
    expect(screen.getByText("0")).toBeInTheDocument();
    // Usage type is an editable Select seeded from the server value.
    expect(document.getElementById("usage-link-1").value).toBe("PRIMARY");
    expect(document.getElementById("usage-link-2").value).toBe("SECONDARY");
    // Per-row unlink action.
    expect(screen.getByTestId("unlink-link-1")).toBeInTheDocument();
  });

  it("shows the empty state when no reagents are linked", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => cb([]));
    renderSection();
    expect(
      await screen.findByText(messages["label.testCatalog.reagents.empty"]),
    ).toBeInTheDocument();
  });

  it("shows an error state when the fetch fails", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => cb(undefined));
    renderSection();
    expect(
      await screen.findByText(messages["label.testCatalog.reagents.loadError"]),
    ).toBeInTheDocument();
  });

  it("renders the Link Reagent button", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => cb([]));
    renderSection();
    expect(
      await screen.findByTestId("link-reagent-button"),
    ).toBeInTheDocument();
  });

  it("auto-saves a usage-type change via PUT and shows a success toast", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => cb(TWO_REAGENTS));
    putToOpenElisServer.mockImplementation((url, body, cb) => cb(200));
    renderSection();

    await screen.findByText("Glucose Reagent");
    fireEvent.change(document.getElementById("usage-link-1"), {
      target: { value: "SECONDARY" },
    });

    expect(putToOpenElisServer).toHaveBeenCalledWith(
      "/rest/test-catalog/42/reagents/7",
      expect.stringContaining('"usageType":"SECONDARY"'),
      expect.any(Function),
    );
    expect(
      await screen.findByText(messages["label.testCatalog.reagents.updated"]),
    ).toBeInTheDocument();
  });

  it("confirms unlink and calls DELETE", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => cb(TWO_REAGENTS));
    deleteFromOpenElisServer.mockImplementation((url, cb) => cb(204));
    renderSection();

    await screen.findByText("Glucose Reagent");
    fireEvent.click(screen.getByTestId("unlink-link-1"));

    // Confirmation modal shows the reagent name in the warning copy.
    expect(
      await screen.findByText(/Unlink Glucose Reagent from this test/),
    ).toBeInTheDocument();

    // Click the danger primary button in the modal footer.
    const confirmBtn = document.querySelector(
      ".cds--modal-footer .cds--btn--danger",
    );
    fireEvent.click(confirmBtn);

    expect(deleteFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/test-catalog/42/reagents/7",
      expect.any(Function),
    );
  });
});
