import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../../languages/en.json";
import BenchQcTab from "../BenchQcTab";
import { getFromOpenElisServer } from "../../../utils/Utils";

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
}));

// Rendering with the real en.json also fails loudly if a referenced i18n key is
// missing — react-intl falls back to the raw key, which breaks these assertions.
const renderTab = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <BenchQcTab />
    </IntlProvider>,
  );

const respondWith = (rows) => {
  getFromOpenElisServer.mockImplementation((url, callback) => callback(rows));
};

describe("BenchQcTab", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockReset();
  });

  test("requests bench QC without a source filter by default", async () => {
    respondWith([]);
    renderTab();

    // The stub invokes the callback synchronously, so the request has already been
    // made by the time render returns — no need to wait on it.
    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/qc/dashboard/bench",
      expect.any(Function),
    );
  });

  test("shows an explicit empty state rather than a bare table", async () => {
    respondWith([]);
    renderTab();

    expect(
      await screen.findByText(
        "No manual or rapid-test controls were recorded in this period.",
      ),
    ).toBeInTheDocument();
  });

  test("renders a row per lab unit and test, with the source", async () => {
    respondWith([
      {
        testSectionId: "701",
        testSectionName: "Haematology",
        testId: "6601",
        testName: "Bench Haemoglobin",
        source: "MANUAL",
        totalRuns: 3,
        failedRuns: 0,
        lastRun: "2025-06-10T09:00:00",
      },
    ]);
    renderTab();

    expect(await screen.findByText("Haematology")).toBeInTheDocument();
    expect(screen.getByText("Bench Haemoglobin")).toBeInTheDocument();
    expect(screen.getByText("MANUAL")).toBeInTheDocument();
    // The T separator is not something a lab tech should have to read.
    expect(screen.getByText("2025-06-10 09:00:00")).toBeInTheDocument();
  });

  test("an RDT failure is visible here — the only QC surface it reaches", async () => {
    respondWith([
      {
        testSectionId: "701",
        testSectionName: "Parasitology",
        testId: "6602",
        testName: "Bench Malaria RDT",
        source: "RDT",
        totalRuns: 4,
        failedRuns: 2,
        lastRun: "2025-06-10T10:00:00",
      },
    ]);
    renderTab();

    expect(await screen.findByText("Bench Malaria RDT")).toBeInTheDocument();
    expect(screen.getByText("RDT")).toBeInTheDocument();
    // Failures are tagged, not just counted: a number alone does not read as
    // "act on this" when scanning.
    const failedCell = screen.getByText("2");
    expect(failedCell).toBeInTheDocument();
    expect(failedCell.closest(".cds--tag")).not.toBeNull();
  });

  test("a clean group shows its zero without a failure tag", async () => {
    respondWith([
      {
        testSectionId: "701",
        testSectionName: "Haematology",
        testId: "6601",
        testName: "Bench Haemoglobin",
        source: "MANUAL",
        totalRuns: 5,
        failedRuns: 0,
        lastRun: "2025-06-10T09:00:00",
      },
    ]);
    renderTab();

    const zero = await screen.findByText("0");
    expect(zero.closest(".cds--tag")).toBeNull();
  });

  test("missing names degrade to a dash instead of blank cells", async () => {
    respondWith([
      {
        testSectionId: "701",
        testSectionName: null,
        testId: "6601",
        testName: null,
        source: "MANUAL",
        totalRuns: 1,
        failedRuns: 0,
        lastRun: null,
      },
    ]);
    renderTab();

    // Lab unit, test name and last run all fall back rather than rendering blank.
    expect(await screen.findAllByText("-")).toHaveLength(3);
  });
});
