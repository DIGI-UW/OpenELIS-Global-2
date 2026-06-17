/**
 * RangesSection — OGC-949 M7 / OGC-969..971.
 *
 * Covers: loading ranges + the per-sex coverage report, adding a range through
 * the modal with age value+unit → day conversion in the saved payload, deleting
 * a range, and the fetch-error state.
 */

// ========== MOCKS (before imports) ==========
vi.mock("../../../layout/Layout", async () => {
  const React = await import("react");
  return {
    NotificationContext: React.createContext({
      addNotification: () => {},
      setNotificationVisible: () => {},
    }),
  };
});

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  putToOpenElisServer: vi.fn(),
}));

// ========== IMPORTS ==========
import React from "react";
import { fireEvent, render, screen, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import RangesSection from "./RangesSection";
import {
  getFromOpenElisServer,
  putToOpenElisServer,
} from "../../../utils/Utils";
import messages from "../../../../languages/en.json";

const renderSection = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <RangesSection testId="42" />
    </IntlProvider>,
  );

const emptyCoverage = {
  male: { sex: "M", status: "EMPTY", gaps: [], overlaps: [] },
  female: { sex: "F", status: "EMPTY", gaps: [], overlaps: [] },
};

beforeEach(() => {
  vi.clearAllMocks();
  putToOpenElisServer.mockImplementation((url, payload, cb) => cb(200));
});

describe("RangesSection", () => {
  it("renders the per-sex coverage report and the range rows", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb({
        testId: "42",
        ranges: [
          {
            id: "1",
            gender: "M",
            minAge: 0,
            maxAge: 6570, // 18 years in days
            lowNormal: 4,
            highNormal: 11,
            lowCritical: null,
            highCritical: null,
          },
        ],
        coverage: {
          male: {
            sex: "M",
            status: "GAP",
            gaps: [{ fromAge: 30, toAge: 45 }],
            overlaps: [],
          },
          female: { sex: "F", status: "EMPTY", gaps: [], overlaps: [] },
        },
      }),
    );
    renderSection();

    // Coverage panel: male has a gap, female has no ranges.
    expect(await screen.findByText("Has gaps")).toBeInTheDocument();
    expect(screen.getByText("No ranges")).toBeInTheDocument();
    // The uncovered window renders in human units (days), not raw years.
    expect(screen.getByText(/30 days/)).toBeInTheDocument();
    // The range row renders age in adaptive units + the normal band.
    expect(screen.getByText(/18 years/)).toBeInTheDocument();
    expect(screen.getByText("4 – 11")).toBeInTheDocument();
  });

  it("adds a range via the modal and saves it converted to days", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb({ testId: "42", ranges: [], coverage: emptyCoverage }),
    );
    renderSection();
    await screen.findByText(messages["label.testCatalog.ranges.empty"]);

    // Open the add-range modal.
    fireEvent.click(screen.getByTestId("add-range"));
    const dialog = await screen.findByRole("dialog");

    // Male, 0–18 years, normal 4–11.
    fireEvent.change(
      within(dialog).getByLabelText(
        messages["label.testCatalog.ranges.modal.sex"],
      ),
      { target: { value: "M" } },
    );
    fireEvent.change(
      within(dialog).getByLabelText(
        messages["label.testCatalog.ranges.modal.ageUnit"],
      ),
      { target: { value: "years" } },
    );
    fireEvent.change(
      within(dialog).getByLabelText(
        messages["label.testCatalog.ranges.modal.maxAge"],
      ),
      { target: { value: "18" } },
    );
    fireEvent.change(
      within(dialog).getByLabelText(
        messages["label.testCatalog.ranges.modal.lowNormal"],
      ),
      { target: { value: "4" } },
    );
    // Save the modal (scoped — the section has its own "Save" button too).
    fireEvent.click(within(dialog).getByRole("button", { name: "Save" }));

    // Row now shows the new male range.
    expect(await screen.findByText(/18 years/)).toBeInTheDocument();

    // Persist the section; payload carries the day-converted bounds.
    fireEvent.click(screen.getByRole("button", { name: "Save" }));
    await waitFor(() => expect(putToOpenElisServer).toHaveBeenCalled());
    const body = JSON.parse(putToOpenElisServer.mock.calls[0][1]);
    expect(body.ranges).toHaveLength(1);
    expect(body.ranges[0].gender).toBe("M");
    expect(body.ranges[0].minAge).toBe(0);
    expect(body.ranges[0].maxAge).toBe(6570); // 18 * 365
    expect(body.ranges[0].lowNormal).toBe(4);
  });

  it("deletes a range", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb({
        testId: "42",
        ranges: [{ id: "1", gender: "F", minAge: 0, maxAge: 6570 }],
        coverage: emptyCoverage,
      }),
    );
    renderSection();
    expect(await screen.findByText(/18 years/)).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Delete" }));
    expect(
      screen.getByText(messages["label.testCatalog.ranges.empty"]),
    ).toBeInTheDocument();
  });

  it("shows an error state when the fetch fails", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => cb(undefined));
    renderSection();
    expect(
      await screen.findByText(messages["label.testCatalog.ranges.loadError"]),
    ).toBeInTheDocument();
  });
});
