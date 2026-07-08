import React from "react";
import { act, fireEvent, render, screen, within } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../../languages/en.json";
import QAOverview from "../QAOverview";
import { getFromOpenElisServer } from "../../../utils/Utils";

vi.mock("../../../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
  };
});

// 3 critical+Pending, plus noise the predicates must ignore
const NCE_LIST = [
  { id: "1", severity: "CRITICAL", status: "Pending" },
  { id: "2", severity: "CRITICAL", status: "Pending" },
  { id: "3", severity: "CRITICAL", status: "Pending" },
  { id: "4", severity: "CRITICAL", status: "Closed" },
  { id: "5", severity: "MAJOR", status: "Pending" },
  { id: "6", severity: "MINOR", status: "Corrective Action" },
];

// Rendering with the real en.json also fails loudly if a referenced i18n key
// is missing (react-intl falls back to the raw key, breaking text assertions).
const renderPage = async () => {
  let view;
  await act(async () => {
    view = render(
      <IntlProvider locale="en" messages={messages}>
        <MemoryRouter>
          <QAOverview />
        </MemoryRouter>
      </IntlProvider>,
    );
  });
  return view;
};

beforeEach(() => {
  sessionStorage.clear();
  vi.clearAllMocks();
  getFromOpenElisServer.mockImplementation((url, callback) => {
    if (url.includes("/rest/nce/dashboard")) {
      callback({ nceList: NCE_LIST });
    }
  });
});

describe("QAOverview shell", () => {
  test("renders page title and the six sections with the expected placeholder slots", async () => {
    await renderPage();

    expect(
      screen.getByRole("heading", { name: "QA Overview" }),
    ).toBeInTheDocument();
    expect(
      screen.getByText(/Daily snapshot for the QA Officer/),
    ).toBeInTheDocument();

    // NCE slots are live (WS-C); the counts below are the remaining placeholders
    const slotCounts = {
      "Attention Required": 6,
      Today: 4,
      "This Week": 8,
      "Pillar Status": 4,
      "Recent Activity": 1,
    };
    Object.entries(slotCounts).forEach(([name, slots]) => {
      const region = screen.getByRole("region", { name });
      expect(within(region).getAllByText("Coming soon")).toHaveLength(slots);
    });

    // Tile + attention row mount together but share one deduped request
    expect(getFromOpenElisServer).toHaveBeenCalledTimes(1);
  });

  test("Today tiles carry the KPI titles, tickets, and the live NCE Pulse count", async () => {
    await renderPage();
    const today = screen.getByRole("region", { name: "Today" });

    [
      "Average TAT",
      "Rejection Rate",
      "Amendment Rate",
      "Critical Callback",
      "NCE Pulse",
    ].forEach((title) => {
      expect(within(today).getByText(title)).toBeInTheDocument();
    });
    ["OGC-696", "OGC-697", "OGC-698", "OGC-714"].forEach((ticket) => {
      expect(within(today).getByText(ticket)).toBeInTheDocument();
    });
    // NCE Pulse is live: no ticket tag, real counts from the mocked payload
    expect(within(today).queryByText("OGC-699")).not.toBeInTheDocument();
    expect(within(today).getByText("3")).toHaveClass("qa-live-amber");
    expect(
      within(today).getByText("1 in corrective action"),
    ).toBeInTheDocument();
  });

  test("attention section shows the live critical-NCE row plus ticket-annotated placeholders", async () => {
    await renderPage();

    const attention = screen.getByRole("region", {
      name: "Attention Required",
    });
    const liveRow = within(attention).getByRole("button", {
      name: /Critical NCEs pending acknowledgment/,
    });
    expect(within(liveRow).getByText("3")).toBeInTheDocument();
    expect(liveRow).toHaveClass("qa-live-row-alert");

    // Overdue CAPAs + effectiveness reviews both light up via OGC-707
    expect(within(attention).getAllByText("OGC-707")).toHaveLength(2);
    expect(within(attention).getByText("NCE v2")).toBeInTheDocument();

    const pillars = screen.getByRole("region", { name: "Pillar Status" });
    [
      "Statistical QC",
      "EQA",
      "Quality Indicators",
      "QMS & Improvement",
    ].forEach((pillar) => {
      expect(within(pillars).getByText(pillar)).toBeInTheDocument();
    });
  });

  test("inspector readiness is collapsed by default and open state sticks across a remount", async () => {
    const view = await renderPage();
    const heading = screen.getByRole("button", {
      name: /Inspector readiness/,
    });
    expect(heading).toHaveAttribute("aria-expanded", "false");

    fireEvent.click(heading);
    expect(heading).toHaveAttribute("aria-expanded", "true");
    expect(sessionStorage.getItem("qa.overview.inspectorOpen")).toBe("1");

    const inspector = screen.getByRole("region", {
      name: /Inspector readiness/,
    });
    expect(within(inspector).getAllByText("Coming soon")).toHaveLength(5);
    expect(
      within(inspector).getByText("Are runs in control?"),
    ).toBeInTheDocument();

    view.unmount();
    await renderPage();
    expect(
      screen.getByRole("button", { name: /Inspector readiness/ }),
    ).toHaveAttribute("aria-expanded", "true");
  });
});
