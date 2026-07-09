import React from "react";
import { act, fireEvent, render, screen, within } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../../languages/en.json";
import QAOverview from "../QAOverview";
import { weekStart } from "../overviewData";
import { getFromOpenElisServer } from "../../../utils/Utils";

vi.mock("../../../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
  };
});

// Dates relative to the real clock so the week/24h predicates stay stable
// whichever day the suite runs.
const pad = (n) => String(n).padStart(2, "0");
const isoDay = (d) =>
  `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
const TODAY = isoDay(new Date());
const LAST_MONTH = isoDay(new Date(Date.now() - 10 * 864e5)); // before any Monday
const NOW_ISO = new Date().toISOString();
const HOUR_AGO_ISO = new Date(Date.now() - 3600e3).toISOString();

// 3 critical+Pending (pulse), 1 resolved-with-CAPA this week, plus noise
const NCE_LIST = [
  { id: "1", severity: "CRITICAL", status: "Pending", reportDate: TODAY },
  { id: "2", severity: "CRITICAL", status: "Pending", reportDate: TODAY },
  { id: "3", severity: "CRITICAL", status: "Pending", reportDate: TODAY },
  {
    id: "4",
    nceNumber: "NCE-0004",
    severity: "CRITICAL",
    status: "Closed",
    reportDate: LAST_MONTH,
    history: [
      {
        activity: "CORRECTIVE_ACTION",
        description: "Corrective action updated",
        timestamp: new Date(Date.now() - 9 * 864e5).toISOString(),
        userName: "Sara Chen",
      },
      {
        activity: "RESOLVED",
        description: "NCE resolved and marked as Completed",
        timestamp: NOW_ISO,
        userName: "Sara Chen",
      },
    ],
  },
  { id: "5", severity: "MAJOR", status: "Pending", reportDate: TODAY },
  {
    id: "6",
    severity: "MINOR",
    status: "Corrective Action",
    reportDate: LAST_MONTH,
  },
];

const SUMMARY = {
  qc: {
    compliantInstruments: 4,
    warningInstruments: 1,
    nonCompliantInstruments: 1,
    totalInstruments: 6,
    violations24h: 2,
    violationsThisWeek: 2,
    weekRuleBreakdown: { "1-3s": 1, "1-2s": 1 },
  },
  eqa: { open: 5, overdue: 1, dueSoon14d: 3 },
  week: {
    weekStart: isoDay(weekStart(new Date())),
    weekStartInstant: weekStart(new Date()).toISOString(),
    auditEntries: 8732,
    signatureEvents: 1247,
  },
  activity: [
    {
      type: "ESIG",
      timestamp: HOUR_AGO_ISO,
      actor: "L. Tran",
      meaning: "VALIDATED_AND_RELEASED",
      recordType: "RESULT",
      recordId: 44196,
    },
    {
      type: "QC_VIOLATION",
      timestamp: new Date(Date.now() - 2 * 3600e3).toISOString(),
      ruleCode: "1-2s",
      severity: "WARNING",
      instrumentName: "Architect ci8200",
    },
  ],
};

const TAT_CURRENT = { mean: 33.3333, totalCount: 12 };
const TAT_PRIOR = { mean: 40, totalCount: 9 };

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
  let tatCalls = 0;
  getFromOpenElisServer.mockImplementation((url, callback) => {
    if (url.includes("/rest/nce/dashboard")) {
      callback({ nceList: NCE_LIST });
    } else if (url.includes("/rest/qa/overview/summary")) {
      callback(SUMMARY);
    } else if (url.includes("/rest/reports/tat/summary")) {
      // fetchTatRollup always requests the current window before the prior one
      callback(++tatCalls === 1 ? TAT_CURRENT : TAT_PRIOR);
    } else if (url.includes("/rest/reports/amendment/summary")) {
      callback({ amendedCount: 8, releasedCount: 2580, ratePercent: 0.31 });
    }
  });
});

describe("QAOverview", () => {
  test("renders the six sections; remaining placeholders match the light-up plan", async () => {
    await renderPage();

    expect(
      screen.getByRole("heading", { name: "QA Overview" }),
    ).toBeInTheDocument();
    expect(
      screen.getByText(/Daily snapshot for the QA Officer/),
    ).toBeInTheDocument();

    // WS-F lit This Week / Pillars / Activity and the QC/EQA attention rows;
    // WS-E lit the Today Amendment tile (NCE Pulse already live via WS-C).
    // These placeholders remain.
    const slotCounts = {
      "Attention Required": 4,
      Today: 3,
      "This Week": 1,
      "Pillar Status": 1,
      "Recent Activity": 0,
    };
    Object.entries(slotCounts).forEach(([name, slots]) => {
      const region = screen.getByRole("region", { name });
      expect(within(region).queryAllByText("Coming soon")).toHaveLength(slots);
    });

    // One shared NCE fetch, one overview summary, two TAT windows, plus the
    // Amendment tile's own summary fetch
    expect(getFromOpenElisServer).toHaveBeenCalledTimes(5);
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
    ["OGC-696", "OGC-697", "OGC-714"].forEach((ticket) => {
      expect(within(today).getByText(ticket)).toBeInTheDocument();
    });
    // NCE Pulse is live: no ticket tag, real counts from the mocked payload
    expect(within(today).queryByText("OGC-699")).not.toBeInTheDocument();
    expect(within(today).getByText("3")).toHaveClass("qa-live-amber");
    expect(
      within(today).getByText("1 in corrective action"),
    ).toBeInTheDocument();
    // Amendment Rate is live (WS-E): rate under the green threshold
    expect(within(today).queryByText("OGC-698")).not.toBeInTheDocument();
    expect(within(today).getByText("0.31%")).toHaveClass("qa-live-green");
    expect(within(today).getByText("8 of 2580 released")).toBeInTheDocument();
  });

  test("attention queue: live NCE, QC-violation, and EQA-due rows with drill-throughs", async () => {
    await renderPage();
    const attention = screen.getByRole("region", {
      name: "Attention Required",
    });

    const nceRow = within(attention).getByRole("button", {
      name: /Critical NCEs pending acknowledgment/,
    });
    expect(within(nceRow).getByText("3")).toBeInTheDocument();
    expect(nceRow).toHaveClass("qa-live-row-alert");

    const qcRow = within(attention).getByRole("button", {
      name: /QC violations in last 24 hours/,
    });
    expect(within(qcRow).getByText("2")).toBeInTheDocument();
    expect(qcRow).toHaveClass("qa-live-row-alert");

    const eqaRow = within(attention).getByRole("button", {
      name: /EQA submissions due in next 14 days/,
    });
    expect(within(eqaRow).getByText("3")).toBeInTheDocument();
    expect(eqaRow).not.toHaveClass("qa-live-row-alert");

    // Overdue CAPAs + effectiveness reviews both light up via OGC-707
    expect(within(attention).getAllByText("OGC-707")).toHaveLength(2);
    expect(within(attention).getByText("NCE v2")).toBeInTheDocument();
  });

  test("This Week counters aggregate NCE, QC, EQA, audit, and signature numbers", async () => {
    await renderPage();
    const week = screen.getByRole("region", { name: "This Week" });

    const stat = (label) =>
      within(week).getByText(label).closest(".qa-live-stat");

    expect(within(stat("New NCEs")).getByText("4")).toBeInTheDocument();
    expect(
      within(stat("New NCEs")).getByText("3 critical · 1 major · 0 minor"),
    ).toBeInTheDocument();
    expect(within(stat("NCEs resolved")).getByText("1")).toBeInTheDocument();
    expect(within(stat("CAPAs completed")).getByText("1")).toBeInTheDocument();
    expect(within(stat("QC violations")).getByText("2")).toBeInTheDocument();
    expect(
      within(stat("QC violations")).getByText("1 × 1-3s · 1 × 1-2s"),
    ).toBeInTheDocument();
    expect(within(stat("EQA submissions")).getByText("5")).toBeInTheDocument();
    expect(
      within(stat("EQA submissions")).getByText("currently open · 1 overdue"),
    ).toBeInTheDocument();
    expect(
      within(stat("Audit log entries")).getByText("8,732"),
    ).toBeInTheDocument();
    expect(
      within(stat("Signature events")).getByText("1,247"),
    ).toBeInTheDocument();

    // Critical results stays a placeholder until OGC-714
    expect(within(week).getByText("OGC-714")).toBeInTheDocument();
  });

  test("pillar chips roll up QC, QI, and QMS status; EQA stays a placeholder", async () => {
    await renderPage();
    const pillars = screen.getByRole("region", { name: "Pillar Status" });

    // Name labels stay bound to the right chips
    [
      "Statistical QC",
      "EQA",
      "Quality Indicators",
      "QMS & Improvement",
    ].forEach((pillar) => {
      expect(within(pillars).getByText(pillar)).toBeInTheDocument();
    });

    const qcChip = within(pillars)
      .getByText("4 of 6 instruments in control")
      .closest(".qa-pillar-chip");
    expect(qcChip).toHaveClass("qa-pillar-red");
    expect(within(qcChip).getByText("Statistical QC")).toBeInTheDocument();

    // 33.3333h mean, improved by 6h40m vs prior window
    const qiChip = within(pillars)
      .getByText(/Avg TAT 33h 20m/)
      .closest(".qa-pillar-chip");
    expect(qiChip).toHaveClass("qa-pillar-green");
    expect(qiChip).toHaveTextContent("↓ 6h 40m");

    const qmsChip = within(pillars)
      .getByText("3 critical pending · 1 in corrective action")
      .closest(".qa-pillar-chip");
    expect(qmsChip).toHaveClass("qa-pillar-red");

    expect(within(pillars).getByText("OGC-721")).toBeInTheDocument();
  });

  test("recent activity merges NCE history, e-signature, and QC alert rows newest-first", async () => {
    await renderPage();
    const activity = screen.getByRole("region", { name: "Recent Activity" });

    const rows = activity.querySelectorAll(".qa-activity-row");
    expect(rows.length).toBe(3);
    expect(rows[0]).toHaveTextContent("Sara Chen resolved NCE-0004");
    expect(rows[1]).toHaveTextContent(
      "L. Tran validated & released RESULT #44196",
    );
    expect(rows[2]).toHaveTextContent(
      "QC alert: 1-2s (WARNING) on Architect ci8200",
    );

    expect(
      within(activity).getByRole("link", { name: /View full audit trail/ }),
    ).toHaveAttribute("href", "/qa/qms/audit-trail");
  });

  test("inspector readiness answers Q1/Q3/Q4 and keeps Q2/Q5 placeholders; open state sticks", async () => {
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
    expect(within(inspector).getAllByText("Coming soon")).toHaveLength(2);
    expect(
      within(inspector).getByText("4 of 6 instruments in control (30d)"),
    ).toBeInTheDocument();
    expect(
      within(inspector).getByText("Avg TAT 33h 20m vs prior 30d"),
    ).toBeInTheDocument();
    expect(
      within(inspector).getByText("3 critical NCEs pending acknowledgment"),
    ).toBeInTheDocument();

    view.unmount();
    await renderPage();
    expect(
      screen.getByRole("button", { name: /Inspector readiness/ }),
    ).toHaveAttribute("aria-expanded", "true");
  });
});
